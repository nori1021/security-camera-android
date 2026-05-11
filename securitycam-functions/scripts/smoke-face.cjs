#!/usr/bin/env node
/**
 * モデル読み込みと（任意で）1枚画像からの埋め込み抽出を確認する。
 *   node scripts/smoke-face.cjs
 *   node scripts/smoke-face.cjs path/to/photo.jpg
 */
"use strict";

const fs = require("fs");
const path = require("path");
const { loadModels, embeddingFromImageBuffer } = require("../src/lib/faceEmbedding.js");

async function main() {
  const imgArg = process.argv[2];
  await loadModels();
  process.stdout.write("models: OK\n");
  if (!imgArg) {
    process.stdout.write("(画像パス省略: 埋め込みテストはスキップ)\n");
    return;
  }
  const abs = path.resolve(imgArg);
  if (!fs.existsSync(abs)) {
    process.stderr.write(`file not found: ${abs}\n`);
    process.exit(1);
  }
  const buf = fs.readFileSync(abs);
  const emb = await embeddingFromImageBuffer(buf);
  if (!emb) {
    process.stdout.write("embedding: 顔を検出できませんでした\n");
    process.exit(2);
  }
  process.stdout.write(`embedding: OK (dim=${emb.length})\n`);
}

main().catch((e) => {
  process.stderr.write(String(e && e.stack ? e.stack : e) + "\n");
  process.exit(1);
});
