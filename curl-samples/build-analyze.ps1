param(
  [Parameter(Mandatory = $true)][string]$ImagePath,
  [string]$OutFile = "$PSScriptRoot\analyze.json",
  [bool]$SendAlertIfUnknown = $true
)
$full = Resolve-Path -LiteralPath $ImagePath
$bytes = [System.IO.File]::ReadAllBytes($full)
$b64 = [Convert]::ToBase64String($bytes)
$obj = [ordered]@{
  image                = $b64
  sendAlertIfUnknown   = $SendAlertIfUnknown
}
$json = $obj | ConvertTo-Json -Compress
[System.IO.File]::WriteAllText($OutFile, $json, [System.Text.UTF8Encoding]::new($false))
Write-Host "Wrote $OutFile"
