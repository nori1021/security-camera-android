# securitycam-functions

配置: `C:\Project\SecurityCamera\securitycam-functions`

セキュリティカメラ用の **Azure Functions（Node.js / v4 モデル）** 雛形です。

- `POST /api/registerFace` … 顔画像（base64 配列）を登録し、Person Group を Train まで実行
- `POST /api/analyze` … 1 枚を Detect → Identify。未登録なら（既定）**ACS Email** で通知＋画像添付
- `GET /api/health` … 疎通確認（匿名）

`registerFace` / `analyze` は **`function` 認証**（キー必須）です。

## 前提

- Azure に **Face API**・**Communication Services（メール検証済み）**・**関数アプリ** があること
- 関数アプリの **環境変数**（例: **設定 → 環境変数 → アプリ設定**）に、少なくとも次を設定済みであること:  
  `ACS_CONNECTION_STRING`, `APPLICATIONINSIGHTS_CONNECTION_STRING`, `AzureWebJobsStorage`, `DEPLOYMENT_STORAGE_CONNECTION_STRING`, `EMAIL_SENDER_ADDRESS`, `FACE_ENDPOINT`, `FACE_KEY`, `NOTIFY_EMAIL`
- 任意: `PERSON_GROUP_ID`（未設定時は `securitycam-users`）、`IDENTIFY_CONFIDENCE_MIN`（既定 `0.55`）

## ローカル・デプロイ

Core Tools は **devDependency** として入っています。`npm install` のあと **`npm run start`** でローカルホストが起動します。`local.settings.json` は example を元に自分の環境用に用意し、Git には含めないでください。Azure へは `func azure functionapp publish`（または `npm run publish -- <アプリ名>`）を利用します。詳細は [Azure Functions のドキュメント](https://learn.microsoft.com/azure/azure-functions/functions-run-local) を参照してください。

## 呼び出し例

**ホストキー**（ポータル: 関数アプリ → **アプリ キー**）を `YOUR_KEY` に置き換えます。

### 登録

```http
POST https://<app>.azurewebsites.net/api/registerFace?code=YOUR_KEY
Content-Type: application/json

{
  "personName": "owner",
  "images": ["<base64>", "<base64>"]
}
```

### 判定（未登録時メール）

```http
POST https://<app>.azurewebsites.net/api/analyze?code=YOUR_KEY
Content-Type: application/json

{
  "image": "<base64>",
  "sendAlertIfUnknown": true,
  "notifyEmail": "optional@example.com"
}
```

`notifyEmail` を省略すると `NOTIFY_EMAIL` に送信します。

## 注意

- Face の **recognitionModel** は `recognition_04`、検出は `detection_03` に固定しています（Face リソースの仕様に合わせて変更が必要な場合があります）。
- 初回の `analyze` の前に、必ず `registerFace` で **Person Group の Train が成功**している必要があります。
