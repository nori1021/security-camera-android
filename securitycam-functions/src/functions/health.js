"use strict";

const { app } = require("@azure/functions");

app.http("health", {
  methods: ["GET"],
  authLevel: "anonymous",
  handler: async () =>
    new Response(JSON.stringify({ ok: true, ts: new Date().toISOString() }), {
      status: 200,
      headers: { "Content-Type": "application/json; charset=utf-8" },
    }),
});
