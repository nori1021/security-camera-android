#!/usr/bin/env node
/**
 * local.settings.json が無ければ example をコピーする。
 */
import fs from "fs";
import path from "path";
import { fileURLToPath } from "url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const root = path.join(__dirname, "..");
const dest = path.join(root, "local.settings.json");
const src = path.join(root, "local.settings.json.example");

if (fs.existsSync(dest)) {
  process.stdout.write(`exists: ${dest}\n`);
  process.exit(0);
}
fs.copyFileSync(src, dest);
process.stdout.write(`created: ${dest}\n`);
