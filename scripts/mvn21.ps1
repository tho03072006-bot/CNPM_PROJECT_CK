param(
    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]] $MavenArgs = @('-v')
)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$previousJavaHome = $env:JAVA_HOME
$previousPath = $env:PATH
$previousMavenOpts = $env:MAVEN_OPTS
$candidates = @()
if ($env:JAVA_HOME) { $candidates += $env:JAVA_HOME }
if (Test-Path (Join-Path $env:USERPROFILE '.jdks')) {
    $candidates += Get-ChildItem (Join-Path $env:USERPROFILE '.jdks') -Directory |
        Select-Object -ExpandProperty FullName
}
if (Test-Path (Join-Path $projectRoot '.tools')) {
    $candidates += Get-ChildItem (Join-Path $projectRoot '.tools') -Directory -Filter 'jdk-21*' |
        Select-Object -ExpandProperty FullName
}
$jdk = $candidates | Where-Object {
    $releaseFile = Join-Path $_ 'release'
    (Test-Path $releaseFile) -and
        (Select-String -LiteralPath $releaseFile -Pattern '^JAVA_VERSION="21[.]' -Quiet)
} | Select-Object -First 1
if (!$jdk) { throw 'JDK 21 is required. Set JAVA_HOME to JDK 21. See docs/MODULE3.md.' }
try {
    $env:JAVA_HOME = $jdk
    $env:PATH = "$jdk/bin;$previousPath"
    $env:MAVEN_OPTS = "$previousMavenOpts -Dfile.encoding=UTF-8"
    Push-Location $projectRoot
    & mvn @MavenArgs
    $result = $LASTEXITCODE
} finally {
    Pop-Location
    $env:JAVA_HOME = $previousJavaHome
    $env:PATH = $previousPath
    $env:MAVEN_OPTS = $previousMavenOpts
}
exit $result
