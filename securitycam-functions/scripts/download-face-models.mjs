#!/usr/bin/env node
/**
 * face-api.js の重みを weights マニフェストから取得して models/face-api に保存する。
 * マニフェストは [{ paths, weights }] 形式と、古い { weights: [{ paths }] } の両方に対応。
 */
import fs from "fs";
import path from "path";
import { fileURLToPath } from "url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const ROOT = path.join(__dirname, "..");
const OUT = path.join(ROOT, "models", "face-api");
const BASE =
  process.env.FACE_API_WEIGHTS_BASE ||
  "https://raw.githubusercontent.com/justadudewhohacks/face-api.js/master/weights";

const MANIFESTS = [
  "ssd_mobilenetv1_model-weights_manifest.json",
  "face_landmark_68_model-weights_manifest.json",
  "face_recognition_model-weights_manifest.json",
];

/** @param {unknown} json */
function collectShardPaths(json) {
  const paths = new Set();
  const roots = Array.isArray(json) ? json : [json];
  for (const root of roots) {
    if (!root || typeof root !== "object") continue;
    if (Array.isArray(root.paths)) {
      for (const p of root.paths) paths.add(p);
    }
    if (Array.isArray(root.weights)) {
      for (const w of root.weights) {
        if (w && Array.isArray(w.paths)) {
          for (const p of w.paths) paths.add(p);
        }
      }
    }
  }
  return paths;
}

async function fetchBuf(url) {
  const res = await fetch(url);
  if (!res.ok) throw new Error(`${res.status} ${url}`);
  return Buffer.from(await res.arrayBuffer());
}

async function downloadFile(rel) {
  const url = `${BASE}/${rel}`;
  const dest = path.join(OUT, rel);
  fs.mkdirSync(path.dirname(dest), { recursive: true });
  if (fs.existsSync(dest) && fs.statSync(dest).size > 0) {
    return;
  }
  const buf = await fetchBuf(url);
  fs.writeFileSync(dest, buf);
  process.stdout.write(`OK ${rel}\n`);
}

async function main() {
  fs.mkdirSync(OUT, { recursive: true });
  for (const m of MANIFESTS) {
    await downloadFile(m);
    const manifestPath = path.join(OUT, m);
    const json = JSON.parse(fs.readFileSync(manifestPath, "utf8"));
    const paths = collectShardPaths(json);
    for (const p of paths) {
      await downloadFile(p);
    }
  }
  process.stdout.write(`Models ready under ${OUT}\n`);
}

main().catch((e) => {
  console.error(e);
  process.exit(1);
});
