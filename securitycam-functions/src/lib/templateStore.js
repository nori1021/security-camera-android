"use strict";

const fs = require("fs").promises;
const path = require("path");
const { TableClient } = require("@azure/data-tables");

const PARTITION = "facembed";

function localStorePath() {
  return path.join(__dirname, "..", "..", "local_data", "face-templates.json");
}

function connectionString() {
  return process.env.FACE_STORAGE_CONNECTION_STRING || process.env.AzureWebJobsStorage || "";
}

function tableName() {
  return process.env.FACE_TEMPLATES_TABLE || "FaceTemplates";
}

function useTable() {
  return Boolean(connectionString().trim());
}

async function loadLocal() {
  const p = localStorePath();
  try {
    const raw = await fs.readFile(p, "utf8");
    const j = JSON.parse(raw);
    const map = new Map();
    if (j && typeof j === "object") {
      for (const [k, v] of Object.entries(j)) {
        if (Array.isArray(v)) map.set(k, v.map(Number));
      }
    }
    return map;
  } catch {
    return new Map();
  }
}

async function saveLocal(map) {
  const p = localStorePath();
  await fs.mkdir(path.dirname(p), { recursive: true });
  const obj = Object.fromEntries(map);
  await fs.writeFile(p, JSON.stringify(obj), "utf8");
}

function getTableClient() {
  const cs = connectionString();
  return TableClient.fromConnectionString(cs, tableName());
}

/**
 * @returns {Promise<Map<string, number[]>>}
 */
async function listTemplates() {
  if (useTable()) {
    const client = getTableClient();
    const map = new Map();
    try {
      const iter = client.listEntities({
        queryOptions: { filter: `PartitionKey eq '${PARTITION}'` },
      });
      for await (const e of iter) {
        const sid = e.rowKey;
        const raw = e.embeddingJson;
        if (typeof raw === "string") {
          const arr = JSON.parse(raw);
          if (Array.isArray(arr)) map.set(sid, arr.map(Number));
        }
      }
    } catch (err) {
      if (err.statusCode === 404) return new Map();
      throw err;
    }
    return map;
  }
  return loadLocal();
}

/**
 * @param {string} subjectId
 * @param {number[]} embedding L2-normalized
 */
async function upsertTemplate(subjectId, embedding) {
  if (useTable()) {
    const client = getTableClient();
    await client.createTable().catch((e) => {
      if (e.statusCode !== 409) throw e;
    });
    await client.upsertEntity({
      partitionKey: PARTITION,
      rowKey: subjectId,
      embeddingJson: JSON.stringify(embedding),
    });
    return;
  }
  const map = await loadLocal();
  map.set(subjectId, embedding);
  await saveLocal(map);
}

module.exports = {
  listTemplates,
  upsertTemplate,
  useTable,
};
