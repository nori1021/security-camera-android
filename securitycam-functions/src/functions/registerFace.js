"use strict";

const { app } = require("@azure/functions");
const { embeddingFromImageBuffer, averageEmbeddings } = require("../lib/faceEmbedding");
const { upsertTemplate } = require("../lib/templateStore");
const { normalizeSubjectId, isValidSubjectId } = require("../lib/subjectId");

function json(status, body) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json; charset=utf-8" },
  });
}

app.http("registerFace", {
  methods: ["POST"],
  authLevel: "function",
  handler: async (request, context) => {
    try {
      const body = await request.json();
      const subjectId = normalizeSubjectId(body);

      if (!isValidSubjectId(subjectId)) {
        return json(400, {
          error:
            "subjectId（または personName）は 1〜128 文字の英数字・ハイフン・アンダースコアのみにしてください。",
        });
      }

      const images = body.images;
      if (!Array.isArray(images) || images.length === 0) {
        return json(400, { error: "images は base64 文字列の配列で指定してください。" });
      }

      const buffers = [];
      for (let i = 0; i < images.length; i++) {
        if (typeof images[i] !== "string") {
          return json(400, { error: `images[${i}] が文字列ではありません。` });
        }
        try {
          buffers.push(Buffer.from(images[i], "base64"));
        } catch {
          return json(400, { error: `images[${i}] の base64 が不正です。` });
        }
      }

      const embeddings = [];
      for (let i = 0; i < buffers.length; i++) {
        const emb = await embeddingFromImageBuffer(buffers[i]);
        if (emb) embeddings.push(emb);
      }

      if (embeddings.length === 0) {
        return json(400, {
          error: "いずれの画像からも顔を検出できませんでした。明るさ・距離を調整してください。",
        });
      }

      const template = averageEmbeddings(embeddings);
      await upsertTemplate(subjectId, template);

      context.log(`registerFace OK subject=${subjectId} images=${embeddings.length}/${buffers.length}`);

      return json(200, {
        subject_id: subjectId,
        images_used: embeddings.length,
        images_total: buffers.length,
      });
    } catch (e) {
      context.log("registerFace error", e);
      return json(500, { error: e.message || String(e) });
    }
  },
});
