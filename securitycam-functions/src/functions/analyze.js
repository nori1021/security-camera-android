"use strict";

const { app } = require("@azure/functions");
const face = require("../lib/faceApi");
const { sendEmail } = require("../lib/emailNotify");

function json(status, body) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json; charset=utf-8" },
  });
}

app.http("analyze", {
  methods: ["POST"],
  authLevel: "function",
  handler: async (request, context) => {
    try {
      const body = await request.json();
      const imageB64 = body.image;
      const sendAlert =
        body.sendAlertIfUnknown === undefined ? true : Boolean(body.sendAlertIfUnknown);
      const notifyTo = typeof body.notifyEmail === "string" && body.notifyEmail.trim()
        ? body.notifyEmail.trim()
        : undefined;

      if (typeof imageB64 !== "string") {
        return json(400, { error: "image は base64 文字列で指定してください。" });
      }

      let imageBuffer;
      try {
        imageBuffer = Buffer.from(imageB64, "base64");
      } catch {
        return json(400, { error: "image の base64 が不正です。" });
      }

      const groupId = face.personGroupId();
      const detected = await face.detectFaceIds(imageBuffer);
      if (!Array.isArray(detected) || detected.length === 0) {
        return json(200, {
          isRegistered: false,
          reason: "no_face",
          message: "顔が検出できませんでした。",
        });
      }

      const faceIds = detected.map((d) => d.faceId).filter(Boolean);
      if (faceIds.length === 0) {
        return json(200, {
          isRegistered: false,
          reason: "no_face_id",
        });
      }

      const identifyResults = await face.identify(faceIds, groupId);
      const minConf = face.identifyConfidenceMin();
      let best = null;
      if (Array.isArray(identifyResults) && identifyResults.length > 0) {
        const cands = identifyResults[0].candidates || [];
        for (const c of cands) {
          if (!best || (c.confidence || 0) > (best.confidence || 0)) best = c;
        }
      }

      const isRegistered = best && (best.confidence || 0) >= minConf;

      if (isRegistered) {
        return json(200, {
          isRegistered: true,
          personId: best.personId,
          confidence: best.confidence,
          threshold: minConf,
        });
      }

      let notified = false;
      if (sendAlert) {
        try {
          await sendEmail({
            to: notifyTo,
            subject: "[SecurityCam] 未登録の人物を検知しました",
            plainText: `未登録と判定されました（信頼度しきい値: ${minConf}）。\n時刻: ${new Date().toISOString()}`,
            attachmentBase64: imageB64,
            attachmentName: "snapshot.jpg",
          });
          notified = true;
        } catch (mailErr) {
          context.log("analyze mail error", mailErr);
          return json(200, {
            isRegistered: false,
            confidence: best?.confidence ?? null,
            threshold: minConf,
            notified: false,
            mailError: mailErr.message || String(mailErr),
          });
        }
      }

      return json(200, {
        isRegistered: false,
        confidence: best?.confidence ?? null,
        threshold: minConf,
        notified,
      });
    } catch (e) {
      context.log("analyze error", e);
      return json(e.status && e.status < 600 ? e.status : 500, {
        error: e.message || String(e),
      });
    }
  },
});
