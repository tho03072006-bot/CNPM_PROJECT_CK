$ErrorActionPreference = 'Stop'
& (Join-Path $PSScriptRoot 'mvn21.ps1') spring-boot:run '-Dspring-boot.run.profiles=cloud'
exit $LASTEXITCODE
