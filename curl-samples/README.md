# curl samples

## Before hitting local Functions

From `securitycam-functions`: run `npm install` and ensure face weights exist (`npm run models` if your workflow requires it). Optional smoke: `npm run verify`.

If **`AzureWebJobsStorage`** is empty, the host may log unhealthy every ~30s. Use `npm run init:settings`, then `npm run dev` (Azurite + `func start`), or see [`../securitycam-functions/LOCAL_DEV.md`](../securitycam-functions/LOCAL_DEV.md).

## Build JSON from an image

```powershell
cd curl-samples
.\build-register.ps1 -ImagePath C:\path\to\face.jpg
.\build-analyze.ps1  -ImagePath C:\path\to\face2.jpg
.\build-identify.ps1 -ImagePath C:\path\to\face2.jpg
```

## Run curl

```powershell
$env:FUNC_HOST = "localhost:7071"
$env:FUNC_KEY  = "<function or host key from portal / local.settings>"
# Optional:
# $env:FUNC_SCHEME = "https"
.\run-examples.ps1
```

You may copy `register.example.json` / `analyze.example.json` to `register.json` / `analyze.json` and fill base64 manually. These generated files are **gitignored** when they contain real image data.

## Endpoints

- `GET /api/health`
- `POST /api/registerFace` — `{ subjectId, images[] }` (base64 JPEG)
- `POST /api/identifyFace` — `{ image }` → `registered` / `subject_id`
- `POST /api/analyze` — match + optional email on unknown (ACS configured in Azure)

Japanese notes: [README.ja.md](README.ja.md)
