"use strict";

const { app } = require("@azure/functions");
const face = require("../lib/faceApi");

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
      const images = body.images;
      const personName = (body.personName || "owner").trim() || "owner";

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

      const groupId = await face.ensurePersonGroup("SecurityCam users");
      const personId = await face.findOrCreatePerson(groupId, personName);

      const persisted = [];
      for (let i = 0; i < buffers.length; i++) {
        const r = await face.addPersonFace(groupId, personId, buffers[i]);
        persisted.push(r.persistedFaceId);
      }

      await face.trainPersonGroup(groupId);
      const training = await face.waitTraining(groupId);

      context.log(`registerFace OK person=${personName} faces=${persisted.length}`);

      return json(200, {
        personGroupId: groupId,
        personId,
        personName,
        persistedFaceIds: persisted,
        training: training.status,
      });
    } catch (e) {
      context.log("registerFace error", e);
      return json(e.status && e.status < 600 ? e.status : 500, {
        error: e.message || String(e),
      });
    }
  },
});
