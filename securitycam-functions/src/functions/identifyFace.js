"use strict";

const { app } = require("@azure/functions");
const { embeddingFromImageBuffer, bestMatch } = require("../lib/faceEmbedding");
const { listTemplates } = require("../lib/templateStore");

function json(status, body) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json; charset=utf-8" },
  });
}

app.http("identifyFace", {
  methods: ["POST"],
  authLevel: "function",
  handler: async (request, context) => {
    try {
      const body = await request.json();
      const imageB64 = body.image;

      if (typeof imageB64 !== "string") {
        return json(400, { error: "image は base64 文字列で指定してください。" });
      }

      let imageBuffer;
      try {
        imageBuffer = Buffer.from(imageB64, "base64");
      } catch {
        return json(400, { error: "image の base64 が不正です。" });
      }

      const gallery = await listTemplates();
      if (gallery.size === 0) {
        return json(200, { registered: false, reason: "no_enrollments" });
      }

      const emb = await embeddingFromImageBuffer(imageBuffer);
      if (!emb) {
        return json(200, { registered: false, reason: "no_face" });
      }

      const match = bestMatch(emb, gallery);
      if (!match) {
        return json(200, { registered: false });
      }

      context.log(`identifyFace OK subject=${match.subjectId}`);
      return json(200, { registered: true, subject_id: match.subjectId });
    } catch (e) {
      context.log("identifyFace error", e);
      return json(500, { error: e.message || String(e) });
    }
  },
});
