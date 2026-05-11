# SecurityCamera Android

Kotlin / Jetpack Compose client for the companion Azure Functions API ([`../securitycam-functions`](../securitycam-functions)): enrollment with CameraX, monitoring with ML Kit (face + object / label heuristics), ROI overlay, and configurable cooldown before calling `analyze`.

## Requirements

- **Android Studio** (open **`SecurityCamera`** repo root or this folder).
- **Min SDK** 26 · **Target SDK** 35
- If Gradle wrapper is missing in your checkout, use Android Studio’s Gradle wrapper import or generate one.

## Features

| Screen | Behavior |
|--------|----------|
| Home | Navigate to monitor, enrollment, settings |
| Settings | Functions base URL, function key, notify email, normalized ROI |
| Enrolled users | Local list of `subjectId`; delete; Retake → enrollment |
| Enrollment | Multi-photo capture → `POST /api/registerFace` |
| Monitor | Stable person/face heuristic → snapshot → `POST /api/analyze`; displays match status and optional mail outcome |

### Cooldown

Client-side **`analyze`** calls are throttled (default **90 s**) to limit load and duplicate alerts.

### Network

- **Production:** HTTPS Functions URL.
- **Debug:** `src/debug/AndroidManifest.xml` enables cleartext HTTP so emulators can hit `http://10.0.2.2:7071` during local Functions development.

### API contract

- **`POST /api/registerFace?code=`** — `{ "subjectId": string, "images": [ base64 jpeg ... ] }`
- **`POST /api/analyze?code=`** — `{ "image": base64, "sendAlertIfUnknown": bool, "notifyEmail": optional }`
- Response fields include `registered`, `subject_id`, `reason`, `notified`, `mailError`, `galleryEmpty`, `notifyConfigured`, `mailSkippedReason` depending on path.

Storing the raw **function key on-device** is convenient for demos only; treat it as a **secret** and rotate if leaked.

## Configuration files

- Copy **`local.properties.example`** → **`local.properties`** (Git-ignored). Set `sdk.dir` and optionally default Functions URL/key for `BuildConfig`.

## Main dependencies

Jetpack Compose · CameraX · ML Kit (face, object detection, image labeling) · OkHttp · DataStore.

## Docs

- Functions local dev: [`../securitycam-functions/LOCAL_DEV.md`](../securitycam-functions/LOCAL_DEV.md)

Japanese summary (legacy): [README.ja.md](README.ja.md)
