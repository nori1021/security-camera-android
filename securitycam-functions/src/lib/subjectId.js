"use strict";

function normalizeSubjectId(body) {
  const raw = body.subjectId ?? body.personName ?? body.person_name;
  if (typeof raw !== "string") return "";
  return raw.trim();
}

function isValidSubjectId(id) {
  return typeof id === "string" && /^[a-zA-Z0-9_-]{1,128}$/.test(id);
}

module.exports = { normalizeSubjectId, isValidSubjectId };
