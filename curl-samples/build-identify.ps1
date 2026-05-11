param(
  [Parameter(Mandatory = $true)][string]$ImagePath,
  [string]$OutFile = "$PSScriptRoot\identify.json"
)
$full = Resolve-Path -LiteralPath $ImagePath
$bytes = [System.IO.File]::ReadAllBytes($full)
$b64 = [Convert]::ToBase64String($bytes)
$obj = [ordered]@{
  image = $b64
}
$json = $obj | ConvertTo-Json -Compress
[System.IO.File]::WriteAllText($OutFile, $json, [System.Text.UTF8Encoding]::new($false))
Write-Host "Wrote $OutFile"
