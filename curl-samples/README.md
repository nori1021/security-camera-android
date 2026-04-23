# curl サンプル

## 画像から JSON を生成

```powershell
cd C:\Project\SecurityCamera\curl-samples
.\build-register.ps1 -ImagePath C:\temp\face.jpg
.\build-analyze.ps1  -ImagePath C:\temp\face2.jpg
```

## curl で実行

```powershell
$env:FUNC_HOST = "＜概要の既定ドメイン＞"
$env:FUNC_KEY  = "＜アプリ キー default＞"
.\run-examples.ps1
```

`register.example.json` / `analyze.example.json` を `register.json` / `analyze.json` にコピーしてから base64 を埋めてもよいです。
