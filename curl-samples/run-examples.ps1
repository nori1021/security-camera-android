# 事前: $env:FUNC_HOST にホスト名のみ（例: localhost:7071 ）。https は含めない。
#       $env:FUNC_KEY  に function / host キー（?code=）
# 事前: register.json / analyze.json（または identify.json）を用意（build-*.ps1）
#
# スキーム: $env:FUNC_SCHEME = http | https を明示できる。
# 省略時は localhost / 127.0.0.1 なら http、それ以外は https。

$ErrorActionPreference = "Stop"
if (-not $env:FUNC_HOST) { throw "Set FUNC_HOST (hostname only, no https)" }
if (-not $env:FUNC_KEY) { throw "Set FUNC_KEY (default host key)" }

$hostOnly = $env:FUNC_HOST.TrimEnd('/')
$scheme = $env:FUNC_SCHEME
if (-not $scheme) {
  if ($hostOnly -match '^(localhost|127\.0\.0\.1)(:\d+)?$') {
    $scheme = 'http'
  }
  else {
    $scheme = 'https'
  }
}
$base = "${scheme}://${hostOnly}"
$root = $PSScriptRoot

$identifyBody = if (Test-Path -LiteralPath "$root\identify.json") { "$root\identify.json" } else { "$root\analyze.json" }

Write-Host "Using $base (scheme=$scheme)"
Write-Host "GET health..."
curl.exe -sS "$base/api/health"
Write-Host ""

Write-Host "POST registerFace..."
curl.exe -sS -X POST "$base/api/registerFace?code=$($env:FUNC_KEY)" `
  -H "Content-Type: application/json" `
  --data-binary "@$root\register.json"
Write-Host ""

Write-Host "POST identifyFace..."
curl.exe -sS -X POST "$base/api/identifyFace?code=$($env:FUNC_KEY)" `
  -H "Content-Type: application/json" `
  --data-binary "@$identifyBody"
Write-Host ""

Write-Host "POST analyze..."
curl.exe -sS -X POST "$base/api/analyze?code=$($env:FUNC_KEY)" `
  -H "Content-Type: application/json" `
  --data-binary "@$root\analyze.json"
Write-Host ""
