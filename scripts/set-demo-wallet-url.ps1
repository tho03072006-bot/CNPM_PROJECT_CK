[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [string]$PublicUrl
)
$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$uri = $null
if (-not [Uri]::TryCreate($PublicUrl.Trim(), [UriKind]::Absolute, [ref]$uri) -or
    $uri.Scheme -ne 'https' -or $uri.IsLoopback -or
    $uri.Host -eq '0.0.0.0' -or $uri.Host.EndsWith('.localhost') -or
    $uri.HostNameType -eq [UriHostNameType]::Unknown -or
    $uri.UserInfo -or $uri.Query -or $uri.Fragment -or
    $uri.AbsolutePath.TrimEnd('/') -notin @('', '/demo-wallet')) {
    throw 'Nhap URL HTTPS cua vi Render, vi du https://ten-vi.onrender.com/demo-wallet. Khong dung localhost, query hay khoa giao dich.'
}
$origin = $uri.GetLeftPart([UriPartial]::Authority)
$secretPath = Join-Path $root 'application-secrets-cloud.properties'
if (-not (Test-Path -LiteralPath $secretPath -PathType Leaf)) {
    throw 'Chua co application-secrets-cloud.properties. Tao file cau hinh database cloud theo file example truoc.'
}
$content = [IO.File]::ReadAllText($secretPath)
$content = [regex]::Replace($content, '(?m)^demo-wallet\.(public-base-url|enabled|require-public-url)\s*=.*(?:\r?\n|$)', '')
$content = $content.TrimEnd() + [Environment]::NewLine +
    'demo-wallet.enabled=true' + [Environment]::NewLine +
    'demo-wallet.require-public-url=true' + [Environment]::NewLine +
    'demo-wallet.public-base-url=' + $origin + [Environment]::NewLine
[IO.File]::WriteAllText($secretPath, $content, [Text.UTF8Encoding]::new($false))
Write-Host ('Da cau hinh vi online: ' + $origin + '/demo-wallet')
Write-Host 'Khoi dong lai web rap local bang profile cloud. QR moi se dung dia chi HTTPS nay.'
Write-Host 'Chay web local bang IDE/Maven; script start-demo-wallet.ps1 danh rieng cho Cloudflare va se ghi de URL bang tunnel.'
