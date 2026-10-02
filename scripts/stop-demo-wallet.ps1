# © 2026 Nhóm 8. Chỉ dừng tiến trình do start-demo-wallet.ps1 tạo; hỗ trợ dọn trạng thái Cloudflare cũ.
$ErrorActionPreference='Stop'
$projectRoot=Split-Path -Parent $PSScriptRoot
$statePath=Join-Path $projectRoot 'target\demo-wallet\runtime.json'
if(!(Test-Path -LiteralPath $statePath)){Write-Host 'Không có bản demo do script quản lý.';return}
$state=Get-Content -LiteralPath $statePath -Raw | ConvertFrom-Json
foreach($entry in @(
    @{id=$state.appPid;started=$state.appStarted;path=$state.jar},
    @{id=$state.tunnelPid;started=$state.tunnelStarted;path=$state.tunnel}
)) {
    if(!$entry.id){continue}
    $processInfo=Get-CimInstance Win32_Process -Filter "ProcessId=$($entry.id)"
    if(!$processInfo){continue}
    $expectedPath=[IO.Path]::GetFullPath($entry.path)
    if(!$expectedPath.StartsWith([IO.Path]::GetFullPath((Join-Path $projectRoot 'target'))+[IO.Path]::DirectorySeparatorChar,[StringComparison]::OrdinalIgnoreCase)) {
        throw 'Từ chối dừng: đường dẫn tiến trình nằm ngoài target của dự án.'
    }
    $sameCommand=$processInfo.CommandLine -and $processInfo.CommandLine.IndexOf($expectedPath,[StringComparison]::OrdinalIgnoreCase) -ge 0
    $sameStarted=$entry.started -and [Math]::Abs((([datetime]$processInfo.CreationDate).ToUniversalTime()-([datetime]$entry.started).ToUniversalTime()).TotalMilliseconds) -lt 1
    if(!$sameCommand -or !$sameStarted){throw "PID $($entry.id) không còn khớp tiến trình của bản demo. Không dừng chương trình khác."}
    Stop-Process -Id $entry.id -ErrorAction Stop
}
Remove-Item -LiteralPath $statePath
Write-Host 'Đã tắt web rạp local. Ví Render vẫn do Render quản lý.'
