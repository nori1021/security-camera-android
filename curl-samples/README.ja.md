# curl サンプル（日本語）

詳細は英語版 [README.md](README.md) を参照してください。

`securitycam-functions` で `npm install` → 必要に応じて `npm run models`。ストレージ警告対策は `LOCAL_DEV.md` を参照。

画像から JSON を生成する例:

```powershell
cd curl-samples
.\build-register.ps1 -ImagePath C:\temp\face.jpg
.\build-analyze.ps1  -ImagePath C:\temp\face2.jpg
```

`FUNC_HOST` / `FUNC_KEY` を設定して `run-examples.ps1` を実行します。
