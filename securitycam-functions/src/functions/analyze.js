"use strict";

const { app } = require("@azure/functions");
const { embeddingFromImageBuffer, bestMatch } = require("../lib/faceEmbedding");
const { listTemplates } = require("../lib/templateStore");
const { sendEmail, notifyPipelineConfigured } = require("../lib/emailNotify");

function json(status, body) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json; charset=utf-8" },
  });
}

/**
 * @param {import("@azure/functions").InvocationContext} context
 * @param {{ sendAlert: boolean, notifyTo?: string, imageB64: string, subject: string, plainText: string }} opts
 */
async function maybeNotifyMail(context, opts) {
  const { sendAlert, notifyTo, imageB64, subject, plainText } = opts;
  if (!sendAlert) {
    return { notified: false };
  }
  if (!notifyPipelineConfigured(notifyTo)) {
    return { notified: false, mailSkippedReason: "notify_not_configured" };
  }
  try {
    await sendEmail({
      to: notifyTo,
      subject,
      plainText,
      attachmentBase64: imageB64,
      attachmentName: "snapshot.jpg",
    });
    return { notified: true };
  } catch (mailErr) {
    context.log("analyze mail error", mailErr);
    return { notified: false, mailError: mailErr.message || String(mailErr) };
  }
}

function compactMailFields(mailRes) {
  const out = {
    notified: mailRes.notified,
  };
  if (mailRes.mailError) out.mailError = mailRes.mailError;
  if (mailRes.mailSkippedReason) out.mailSkippedReason = mailRes.mailSkippedReason;
  return out;
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
      const notifyTo =
        typeof body.notifyEmail === "string" && body.notifyEmail.trim()
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

      const gallery = await listTemplates();
      const galleryEmpty = gallery.size === 0;
      const mailOk = notifyPipelineConfigured(notifyTo);

      if (galleryEmpty && !mailOk) {
        context.log(
          "analyze mail skipped (no enrollments and notify pipeline not configured)",
        );
        return json(200, {
          registered: false,
          reason: "no_enrollments",
          galleryEmpty: true,
          notifyConfigured: false,
          mailSkippedReason: "no_enrollments_and_mail_not_configured",
        });
      }

      const emb = await embeddingFromImageBuffer(imageBuffer);
      const isoTime = new Date().toISOString();

      if (!emb) {
        const mailRes = await maybeNotifyMail(context, {
          sendAlert,
          notifyTo,
          imageB64,
          subject: "[SecurityCam] 人物検知（顔の判別不可）",
          plainText:
            `画像から顔特徴を抽出できませんでした。\n` +
            `時刻: ${isoTime}\n` +
            (galleryEmpty ? "※登録テンプレートはありません。\n" : ""),
        });
        return json(200, {
          registered: false,
          reason: "no_face",
          galleryEmpty,
          notifyConfigured: mailOk,
          ...compactMailFields(mailRes),
        });
      }

      if (galleryEmpty) {
        const mailRes = await maybeNotifyMail(context, {
          sendAlert,
          notifyTo,
          imageB64,
          subject: "[SecurityCam] 人物検知（登録顔なし）",
          plainText:
            `人物は検知できましたが、照合用の登録がありません。\n時刻: ${isoTime}`,
        });
        return json(200, {
          registered: false,
          reason: "no_enrollments",
          galleryEmpty: true,
          notifyConfigured: mailOk,
          ...compactMailFields(mailRes),
        });
      }

      const match = bestMatch(emb, gallery);
      if (match) {
        return json(200, { registered: true, subject_id: match.subjectId });
      }

      const mailRes = await maybeNotifyMail(context, {
        sendAlert,
        notifyTo,
        imageB64,
        subject: "[SecurityCam] 未登録の人物を検知しました",
        plainText: `登録テンプレートと一致しませんでした。\n時刻: ${isoTime}`,
      });

      return json(200, {
        registered: false,
        galleryEmpty: false,
        notifyConfigured: mailOk,
        ...compactMailFields(mailRes),
      });
    } catch (e) {
      context.log("analyze error", e);
      return json(500, { error: e.message || String(e) });
    }
  },
});
