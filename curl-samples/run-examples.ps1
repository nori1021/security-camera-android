# 事前: $env:FUNC_HOST にホスト名のみ（https なし）
#       $env:FUNC_KEY  に default ホストキー
# 事前: register.json / analyze.json を用意（build-*.ps1 または example をコピー）

$ErrorActionPreference = "Stop"
if (-not $env:FUNC_HOST) { throw "Set FUNC_HOST (hostname only, no https)" }
if (-not $env:FUNC_KEY) { throw "Set FUNC_KEY (default host key)" }

$base = "https://$($env:FUNC_HOST.TrimEnd('/'))"
$root = $PSScriptRoot

Write-Host "GET health..."
curl.exe -sS "$base/api/health"
Write-Host ""

Write-Host "POST registerFace..."
curl.exe -sS -X POST "$base/api/registerFace?code=$($env:FUNC_KEY)" `
  -H "Content-Type: application/json" `
  --data-binary "@$root\register.json"
Write-Host ""

Write-Host "POST analyze..."
curl.exe -sS -X POST "$base/api/analyze?code=$($env:FUNC_KEY)" `
  -H "Content-Type: application/json" `
  --data-binary "@$root\analyze.json"
Write-Host ""
