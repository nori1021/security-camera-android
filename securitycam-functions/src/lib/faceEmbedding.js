"use strict";

const path = require("path");
const tf = require("@tensorflow/tfjs");
require("@tensorflow/tfjs-backend-cpu");
const faceapi = require("face-api.js");
const sharp = require("sharp");

const MODEL_DIR = path.join(__dirname, "..", "..", "models", "face-api");

let loadPromise = null;
let backendReady = false;

async function ensureBackend() {
  if (backendReady) return;
  await tf.setBackend("cpu");
  await tf.ready();
  backendReady = true;
}

function getMatchThreshold() {
  const v = Number.parseFloat(process.env.FACE_MATCH_THRESHOLD || "0.82");
  return Number.isFinite(v) ? v : 0.82;
}

function normalizeL2(vec) {
  let s = 0;
  for (let i = 0; i < vec.length; i++) s += vec[i] * vec[i];
  const n = Math.sqrt(s) || 1;
  const out = new Array(vec.length);
  for (let i = 0; i < vec.length; i++) out[i] = vec[i] / n;
  return out;
}

function averageEmbeddings(embeddings) {
  if (!embeddings.length) return null;
  const dim = embeddings[0].length;
  const sum = new Array(dim).fill(0);
  for (const e of embeddings) {
    for (let i = 0; i < dim; i++) sum[i] += e[i];
  }
  for (let i = 0; i < dim; i++) sum[i] /= embeddings.length;
  return normalizeL2(sum);
}

function cosineSimilarity(a, b) {
  let s = 0;
  for (let i = 0; i < a.length; i++) s += a[i] * b[i];
  return s;
}

async function loadModels() {
  await ensureBackend();
  if (loadPromise) return loadPromise;
  loadPromise = (async () => {
    await faceapi.nets.ssdMobilenetv1.loadFromDisk(MODEL_DIR);
    await faceapi.nets.faceLandmark68Net.loadFromDisk(MODEL_DIR);
    await faceapi.nets.faceRecognitionNet.loadFromDisk(MODEL_DIR);
  })();
  return loadPromise;
}

/**
 * @param {Buffer} imageBuffer
 * @returns {Promise<import("@tensorflow/tfjs").Tensor3D>}
 */
async function imageBufferToTensor(imageBuffer) {
  const { data, info } = await sharp(imageBuffer).raw().toBuffer({ resolveWithObject: true });
  const w = info.width;
  const h = info.height;
  const ch = info.channels;
  if (ch === 3) {
    return tf.tensor3d(new Uint8Array(data), [h, w, 3]).toFloat();
  }
  if (ch === 4) {
    const rgb = new Uint8Array(w * h * 3);
    for (let i = 0; i < w * h; i++) {
      const o = i * 4;
      rgb[i * 3] = data[o];
      rgb[i * 3 + 1] = data[o + 1];
      rgb[i * 3 + 2] = data[o + 2];
    }
    return tf.tensor3d(rgb, [h, w, 3]).toFloat();
  }
  throw new Error(`unsupported image channels: ${ch}`);
}

/**
 * @param {Buffer} imageBuffer
 * @returns {Promise<number[]|null>} L2-normalized 128-d descriptor or null
 */
async function embeddingFromImageBuffer(imageBuffer) {
  await loadModels();
  const imageTensor = await imageBufferToTensor(imageBuffer);
  try {
    const det = await faceapi
      .detectSingleFace(imageTensor, new faceapi.SsdMobilenetv1Options({ minConfidence: 0.5 }))
      .withFaceLandmarks()
      .withFaceDescriptor();
    if (!det || !det.descriptor) return null;
    const arr = Array.from(det.descriptor);
    return normalizeL2(arr);
  } finally {
    imageTensor.dispose();
  }
}

/**
 * @param {number[]} query normalized embedding
 * @param {Map<string, number[]>} gallery subjectId -> normalized embedding
 * @returns {{ subjectId: string, score: number } | null}
 */
function bestMatch(query, gallery) {
  const threshold = getMatchThreshold();
  let best = null;
  for (const [subjectId, emb] of gallery) {
    const score = cosineSimilarity(query, emb);
    if (!best || score > best.score) best = { subjectId, score };
  }
  if (!best || best.score < threshold) return null;
  return best;
}

module.exports = {
  loadModels,
  embeddingFromImageBuffer,
  averageEmbeddings,
  bestMatch,
  getMatchThreshold,
  normalizeL2,
};
