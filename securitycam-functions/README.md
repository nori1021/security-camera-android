# securitycam-functions

Azure Functions (**Node.js**, **v4 programming model**) backend for a security-camera–style workflow.

Face embeddings are computed **inside the function** using **TensorFlow.js + face-api.js** (not Azure Cognitive Face REST). Templates persist to **Azure Table Storage**.

| HTTP route | Purpose |
|------------|---------|
| `POST /api/registerFace` | Multiple JPEG (base64) → averaged embedding → upsert template row |
| `POST /api/analyze` | Single image → embedding → cosine match; optional **ACS Email** on unknown / ambiguous paths |
| `POST /api/identifyFace` | Match only (no email) |
| `GET /api/health` | Liveness (**anonymous**) |

`registerFace`, `analyze`, and `identifyFace` use **`authLevel: function`** — callers must pass `?code=` (host or function key).

## Azure prerequisites

- Function App (Linux / Consumption or Flex — use **`func publish ... --build remote`** when native modules must build in Azure).
- Storage: `AzureWebJobsStorage`; optional dedicated `FACE_STORAGE_CONNECTION_STRING` for template tables.
- Email: **Azure Communication Services Email** with a **verified / linked sender domain** — set `ACS_CONNECTION_STRING`, `EMAIL_SENDER_ADDRESS`, and optionally `NOTIFY_EMAIL`.

Copy **`local.settings.json.example`** → **`local.settings.json`** locally (never commit). Deprecated **`FACE_ENDPOINT` / `FACE_KEY`** from older Face API designs are **not** used by current code.

## Run locally

1. Install [Azure Functions Core Tools v4](https://learn.microsoft.com/azure/azure-functions/functions-run-local).
2. Create `local.settings.json` from the example and fill values.
3.

```bash
npm install
func start
```

`npm install` runs **postinstall** to fetch face-api weights under `models/face-api`.

See **[LOCAL_DEV.md](LOCAL_DEV.md)** for Azurite + Table storage.

## Deploy

```bash
npm ci
func azure functionapp publish <YOUR_FUNCTION_APP_NAME> --build remote
```

Mirror `Values` from `local.settings.json` into the portal **Configuration → Application settings** for production.

### Seeing legacy “Face API 403” text from cloud

Current sources **do not call Azure Face REST**. If the cloud still returns Face-related errors, an **older deployment** is likely running — redeploy from this repo.

## Example HTTP calls

Replace `YOUR_KEY` with a host or function key.

### Register

```http
POST https://<app>.azurewebsites.net/api/registerFace?code=YOUR_KEY
Content-Type: application/json

{
  "subjectId": "owner",
  "images": ["<base64>", "<base64>"]
}
```

### Analyze

```http
POST https://<app>.azurewebsites.net/api/analyze?code=YOUR_KEY
Content-Type: application/json

{
  "image": "<base64>",
  "sendAlertIfUnknown": true,
  "notifyEmail": "optional@example.com"
}
```

## Operational notes

- Similarity threshold: `FACE_MATCH_THRESHOLD` (default **0.82**).
- Cold start / model load can exceed default HTTP timeouts — tune `host.json` `functionTimeout` and client read timeouts.

Japanese summary: [README.ja.md](README.ja.md)
