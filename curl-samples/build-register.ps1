param(
  [Parameter(Mandatory = $true)][string]$ImagePath,
  [string]$OutFile = "$PSScriptRoot\register.json",
  [string]$SubjectId = "owner"
)
$full = Resolve-Path -LiteralPath $ImagePath
$bytes = [System.IO.File]::ReadAllBytes($full)
$b64 = [Convert]::ToBase64String($bytes)
$obj = [ordered]@{
  subjectId = $SubjectId
  images    = @($b64)
}
$json = $obj | ConvertTo-Json -Compress
[System.IO.File]::WriteAllText($OutFile, $json, [System.Text.UTF8Encoding]::new($false))
Write-Host "Wrote $OutFile"
