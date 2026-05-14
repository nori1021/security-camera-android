# SecurityCamera (Android + Azure Functions)

End-to-end sample: an **Android** app (CameraX, ML Kit) that registers face templates and runs **on-device / server-side** face matching against a **Node.js Azure Functions** backend. Optional **email alerts** use **Azure Communication Services (Email)** with image attachments.

To try the Android app without building from source, sideload [`releases/app-release.apk`](releases/app-release.apk) on a device and allow installation from unknown sources when prompted.

> **Security note:** This project is a portfolio / reference implementation. Storing a **function host key inside a mobile app** is **not** a production security pattern. A real deployment would use a token-based API gateway, attestation, or short-lived credentials.

## Architecture (high level)

```text
[ Android app ]  --HTTPS JSON (base64 JPEGs)-->  [ Azure Functions ]
        |                                        |
        |                                        +-- Table storage (face templates)
        |                                        +-- Optional: ACS Email (alerts)
        +-- DataStore: settings, enrolled subject IDs (local list only)
```

- **Enrollment:** Multiple JPEGs → averaged embedding → upsert in Azure Table.
- **Monitor:** Person / face heuristic triggers snapshot → `analyze` → cosine similarity vs templates.
- **Alerts:** When templates exist and routing is configured, unknown / ambiguous detections can trigger email with snapshot.

## Repository layout

| Path | Description |
|------|-------------|
| `SecurityCameraAndroid/` | Kotlin, Jetpack Compose, CameraX, ML Kit, OkHttp |
| `securitycam-functions/` | Azure Functions v4 (Node), TF.js + face-api embeddings, Table persistence |
| `curl-samples/` | Example curl payloads (large base64 samples are gitignored; see `README` there) |

Open **`SecurityCamera`** (repo root) **or** **`SecurityCameraAndroid`** in Android Studio; Gradle can resolve `:app` from either layout depending on how the root `settings.gradle.kts` is structured.

## Prerequisites

- **Android:** Android Studio Koala+ recommended; JDK 17.
- **Functions:** Node 18+ / 20+, [Azure Functions Core Tools v4](https://learn.microsoft.com/azure/azure-functions/functions-run-local).
- **Azure (optional):** Function App, Storage (Table), ACS Email if you enable notifications.

## Quick start (local backend)

1. Copy `securitycam-functions/local.settings.json.example` → `local.settings.json` and fill secrets via portal **Application settings** pattern (never commit real values).
2. From `securitycam-functions/`:

   ```bash
   npm install
   npm run dev   # or: azurite + func start — see securitycam-functions/LOCAL_DEV.md
   ```

3. Android: create `SecurityCameraAndroid/local.properties` from **`local.properties.example`** (SDK path + placeholder URLs). Configure base URL and key **only on device** via the in-app Settings screen for demos.

## Deploy Functions (Azure CLI)

Use `--build remote` on Linux consumption/flex plans when native deps must compile in Azure:

```bash
cd securitycam-functions
npm ci
func azure functionapp publish <YOUR_FUNCTION_APP_NAME> --build remote
```

See `SecurityCameraAndroid/publish-functions-remote.bat.example` for a Windows helper template.

## Configuration (names only — **do not commit values**)

**Android (runtime):** base Functions URL, function key, optional notify email, ROI normalized rectangle.

**Azure Function App application settings:** `AzureWebJobsStorage`, optional `FACE_STORAGE_*`, `ACS_CONNECTION_STRING`, `EMAIL_SENDER_ADDRESS`, `NOTIFY_EMAIL`, etc. See `securitycam-functions/local.settings.json.example`.

## Screenshots (for README — add images later)

Replace this section with real screenshots when publishing the portfolio:

1. **Home** — navigation entry points.
2. **Settings** — Functions URL / key fields (**mask** any real key in screenshots).
3. **Enrolled users** — list with Retake / delete actions.
4. **Enrollment** — multi-capture registration flow.
5. **Monitor** — preview with optional ROI overlay and status line (`Registered` / `Not registered` / cooldown).

## License

See [LICENSE](LICENSE).

## Additional docs

- [Android app README](SecurityCameraAndroid/README.md)
- [Azure Functions README](securitycam-functions/README.md)
- [Local development (Azurite)](securitycam-functions/LOCAL_DEV.md)
