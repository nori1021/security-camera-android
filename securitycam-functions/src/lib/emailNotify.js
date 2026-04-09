"use strict";

const { EmailClient } = require("@azure/communication-email");

function getConfig() {
  const conn = process.env.ACS_CONNECTION_STRING || "";
  const sender = process.env.EMAIL_SENDER_ADDRESS || "";
  const to = process.env.NOTIFY_EMAIL || "";
  if (!conn) throw new Error("ACS_CONNECTION_STRING を設定してください。");
  if (!sender) throw new Error("EMAIL_SENDER_ADDRESS を設定してください。");
  if (!to) throw new Error("NOTIFY_EMAIL を設定してください。");
  return { conn, sender, defaultTo: to };
}

/**
 * @param {{ to?: string, subject: string, plainText: string, html?: string, attachmentBase64?: string, attachmentName?: string }} opts
 */
async function sendEmail(opts) {
  const { conn, sender, defaultTo } = getConfig();
  const toAddress = opts.to || defaultTo;
  const client = new EmailClient(conn);

  const message = {
    senderAddress: sender,
    content: {
      subject: opts.subject,
      plainText: opts.plainText,
      ...(opts.html ? { html: opts.html } : {}),
    },
    recipients: {
      to: [{ address: toAddress }],
    },
  };

  if (opts.attachmentBase64 && opts.attachmentName) {
    message.attachments = [
      {
        name: opts.attachmentName,
        contentType: "image/jpeg",
        contentInBase64: opts.attachmentBase64,
      },
    ];
  }

  const poller = await client.beginSend(message);
  const result = await poller.pollUntilDone();
  return result;
}

module.exports = { sendEmail, getConfig };
