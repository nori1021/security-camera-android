#!/usr/bin/env node
/**
 * local.settings.json の AzureWebJobsStorage が空なら Azurite 接続文字列を設定する。
 */
import fs from "fs";
import path from "path";
import { fileURLToPath } from "url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const root = path.join(__dirname, "..");
const dest = path.join(root, "local.settings.json");

const AZURITE =
  "DefaultEndpointsProtocol=http;AccountName=devstoreaccount1;AccountKey=Eby8vdM02xNOcqFlqUwJPLlmEtlCDXJ1OUzFT50uSRZ6IFsuFq2UVErCz4I6tq/K1SZFPTOtr/KBHBeksoGMGw==;BlobEndpoint=http://127.0.0.1:10000/devstoreaccount1;QueueEndpoint=http://127.0.0.1:10001/devstoreaccount1;TableEndpoint=http://127.0.0.1:10002/devstoreaccount1;";

function main() {
  if (!fs.existsSync(dest)) {
    process.stderr.write(`missing ${dest} — run npm run init:settings first\n`);
    process.exit(1);
  }
  const raw = fs.readFileSync(dest, "utf8");
  const j = JSON.parse(raw);
  const v = j.Values?.AzureWebJobsStorage;
  const empty = v === undefined || v === null || String(v).trim() === "";
  if (!empty) {
    process.stdout.write("AzureWebJobsStorage already set — unchanged.\n");
    process.exit(0);
  }
  j.Values = j.Values || {};
  j.Values.AzureWebJobsStorage = AZURITE;
  fs.writeFileSync(dest, JSON.stringify(j, null, 2) + "\n", "utf8");
  process.stdout.write("AzureWebJobsStorage set to Azurite (local emulator).\n");
}

main();
