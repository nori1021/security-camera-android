"use strict";

const RECOGNITION_MODEL = "recognition_04";
const DETECTION_MODEL = "detection_03";

function faceConfig() {
  const endpoint = (process.env.FACE_ENDPOINT || "").replace(/\/$/, "");
  const key = process.env.FACE_KEY || "";
  if (!endpoint || !key) {
    throw new Error("FACE_ENDPOINT と FACE_KEY を設定してください。");
  }
  return { endpoint, key };
}

function personGroupId() {
  return process.env.PERSON_GROUP_ID || "securitycam-users";
}

async function faceFetch(path, options = {}) {
  const { endpoint, key } = faceConfig();
  const url = `${endpoint}${path.startsWith("/") ? path : `/${path}`}`;
  const headers = {
    "Ocp-Apim-Subscription-Key": key,
    ...(options.headers || {}),
  };
  const res = await fetch(url, { ...options, headers });
  const text = await res.text();
  let body;
  try {
    body = text ? JSON.parse(text) : null;
  } catch {
    body = text;
  }
  if (!res.ok) {
    const msg =
      typeof body === "object" && body && body.error
        ? `${body.error.code || "error"}: ${body.error.message || JSON.stringify(body)}`
        : text || res.statusText;
    const err = new Error(`Face API ${res.status}: ${msg}`);
    err.status = res.status;
    err.body = body;
    throw err;
  }
  return body;
}

async function ensurePersonGroup(displayName) {
  const id = personGroupId();
  await faceFetch(`/face/v1.0/persongroups/${encodeURIComponent(id)}`, {
    method: "PUT",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({
      name: displayName || id,
      recognitionModel: RECOGNITION_MODEL,
      userData: "securitycam",
    }),
  }).catch((e) => {
    if (e.status === 409) return;
    throw e;
  });
  return id;
}

async function listPersons(groupId) {
  return faceFetch(`/face/v1.0/persongroups/${encodeURIComponent(groupId)}/persons`, {
    method: "GET",
  });
}

async function createPerson(groupId, name) {
  return faceFetch(`/face/v1.0/persongroups/${encodeURIComponent(groupId)}/persons`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ name }),
  });
}

async function findOrCreatePerson(groupId, personName) {
  const list = await listPersons(groupId);
  const found = Array.isArray(list) && list.find((p) => p.name === personName);
  if (found) return found.personId;
  const created = await createPerson(groupId, personName);
  return created.personId;
}

async function addPersonFace(groupId, personId, imageBuffer) {
  const path =
    `/face/v1.0/persongroups/${encodeURIComponent(groupId)}/persons/${encodeURIComponent(personId)}/persistedfaces` +
    `?detectionModel=${DETECTION_MODEL}`;
  return faceFetch(path, {
    method: "POST",
    headers: { "Content-Type": "application/octet-stream" },
    body: imageBuffer,
  });
}

async function trainPersonGroup(groupId) {
  await faceFetch(`/face/v1.0/persongroups/${encodeURIComponent(groupId)}/train`, {
    method: "POST",
  });
}

async function getTrainingStatus(groupId) {
  return faceFetch(`/face/v1.0/persongroups/${encodeURIComponent(groupId)}/training`, {
    method: "GET",
  });
}

async function waitTraining(groupId, timeoutMs = 120000) {
  const start = Date.now();
  while (Date.now() - start < timeoutMs) {
    const st = await getTrainingStatus(groupId);
    if (st.status === "succeeded") return st;
    if (st.status === "failed") {
      const err = new Error(st.message || "Train failed");
      err.body = st;
      throw err;
    }
    await new Promise((r) => setTimeout(r, 1500));
  }
  throw new Error("Train の完了待ちがタイムアウトしました。");
}

async function detectFaceIds(imageBuffer) {
  const q = new URLSearchParams({
    returnFaceId: "true",
    recognitionModel: RECOGNITION_MODEL,
    detectionModel: DETECTION_MODEL,
  });
  const path = `/face/v1.0/detect?${q.toString()}`;
  return faceFetch(path, {
    method: "POST",
    headers: { "Content-Type": "application/octet-stream" },
    body: imageBuffer,
  });
}

async function identify(faceIds, groupId) {
  return faceFetch("/face/v1.0/identify", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({
      personGroupId: groupId,
      faceIds,
      maxNumOfCandidates: 5,
      confidenceThreshold: 0.01,
    }),
  });
}

function identifyConfidenceMin() {
  const v = parseFloat(process.env.IDENTIFY_CONFIDENCE_MIN || "0.55", 10);
  return Number.isFinite(v) ? v : 0.55;
}

module.exports = {
  personGroupId,
  ensurePersonGroup,
  findOrCreatePerson,
  addPersonFace,
  trainPersonGroup,
  waitTraining,
  detectFaceIds,
  identify,
  identifyConfidenceMin,
  RECOGNITION_MODEL,
};
