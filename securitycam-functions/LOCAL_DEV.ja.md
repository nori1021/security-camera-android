# ローカル開発（ストレージ・ヘルスチェック）

Azure Functions ホストは **`AzureWebJobsStorage`** が無効だと、約 30 秒ごとに  
`Unable to access AzureWebJobsStorage` のヘルス警告が出続けます。

## 手順（推奨: Azurite）

1. **依存関係**

   ```powershell
   cd securitycam-functions
   npm install
   ```

2. **`local.settings.json`**

   - 無い場合: `npm run init:settings`
   - **`AzureWebJobsStorage` が空**のままなら: `npm run settings:azurite`  
     （Azurite 用の接続文字列を自動で書き込みます）

   手動でも、`local.settings.json.example` の `AzureWebJobsStorage` と同じ値にしてください。

3. **Azurite + Functions を同時起動**

   ```powershell
   npm run dev
   ```

   - Table ストレージ（ポート **10002**）が立ち上がってから `func start` が動きます。
   - 終了は **Ctrl+C**（Azurite と Functions の両方が止まります）。

4. **別ウィンドウで動かす場合**

   ターミナル A:

   ```powershell
   npm run azurite
   ```

   ターミナル B:

   ```powershell
   npm run start
   ```

## 本番・クラウドのストレージをローカルで使う場合

実際のストレージアカウントの接続文字列を `AzureWebJobsStorage` に設定すれば Azurite は不要です。

## 顔テンプレートの保存先

`FACE_STORAGE_CONNECTION_STRING` が空のときは **`AzureWebJobsStorage`** と同じ接続が使われます。  
Azurite を使う場合はそのままで **Table にテンプレートが保存**されます。

English version: [LOCAL_DEV.md](LOCAL_DEV.md)
