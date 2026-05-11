# securitycam-functions（日本語サマリー）

セキュリティカメラ用途の **Azure Functions（Node.js / v4）** バックエンドです。英語の詳細は [README.md](README.md) を参照してください。

顔埋め込みは **Azure Face API ではなく**、関数内の **TensorFlow.js + face-api.js** で計算し、テンプレートを **Azure Table** に保存します。

| HTTP | 説明 |
|------|------|
| `POST /api/registerFace` | 複数 JPEG（base64）→ 平均埋め込み → Table upsert |
| `POST /api/analyze` | 1 枚 → 照合。未登録等では **ACS Email**（設定時） |
| `POST /api/identifyFace` | 照合のみ |
| `GET /api/health` | 疎通（anonymous） |

`registerFace` / `analyze` / `identifyFace` は **`authLevel: function`**（`?code=` 必須）。

ローカル実行・Azurite は [LOCAL_DEV.md](LOCAL_DEV.md)（英語）を参照してください。
