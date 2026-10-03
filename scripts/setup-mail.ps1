$ErrorActionPreference = 'Stop'
$setupScript = Join-Path $PSScriptRoot 'setup-mail.py'
& python -X utf8 $setupScript
if ($LASTEXITCODE -ne 0) {
    Write-Host 'Setup was not completed. Run this script again after checking Gmail and App Password.'
}
