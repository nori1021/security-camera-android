# SecurityCamera Android（日本語サマリー）

Azure Functions（[`../securitycam-functions`](../securitycam-functions)）と連携するローカル監視・顔登録アプリです。詳細な英語ドキュメントは [README.md](README.md) を参照してください。

## 開き方

このフォルダまたはリポジトリルートを **Android Studio** で開いてください。

## 機能概要

| 画面 | 内容 |
|------|------|
| ホーム | 監視・ユーザー登録・設定へ |
| 設定 | Functions のベース URL、`code` キー、通知メール、ROI |
| 登録ユーザー | subjectId の一覧・削除・Retake |
| 登録撮影 | CameraX で複数枚 → `POST /api/registerFace` |
| 監視 | ML Kit で人物検知の安定後にスナップショット → `POST /api/analyze` |

監視からの `analyze` は **90 秒に 1 回まで**（アプリ側クールダウン）。

## 設定

`local.properties.example` を `local.properties` にコピーし、`sdk.dir` 等を設定してください（`local.properties` はバージョン管理に含めません）。
