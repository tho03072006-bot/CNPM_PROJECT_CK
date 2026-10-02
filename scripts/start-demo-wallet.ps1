# © 2026 Nhóm 8. Bật web rạp local; ví MoMo giả lập chạy trên Render HTTPS.
[CmdletBinding()]
param([int]$Port=8082,[string]$JavaHome=$env:JAVA_HOME,[string]$PublicUrl,[switch]$SkipBuild)
$ErrorActionPreference='Stop'
$projectRoot=Split-Path -Parent $PSScriptRoot
$runtimeRoot=Join-Path $projectRoot 'target\demo-wallet'
$statePath=Join-Path $runtimeRoot 'runtime.json'
$jarPath=Join-Path $projectRoot 'target\cinema-booking-0.0.1-SNAPSHOT.jar'
$secretPath=Join-Path $projectRoot 'application-secrets-cloud.properties'
if ($Port -lt 1024 -or $Port -gt 65535) {throw 'Cổng phải từ 1024 đến 65535.'}
if (!(Test-Path -LiteralPath $secretPath)) {throw 'Cần cấu hình application-secrets-cloud.properties trước.'}
if (!$PublicUrl) {
    $secretText=[IO.File]::ReadAllText($secretPath)
    $PublicUrl=[regex]::Match($secretText,'(?m)^demo-wallet\.public-base-url[ \t]*=[ \t]*(.*)$').Groups[1].Value.Trim()
}
if (!$PublicUrl) {throw 'Chưa có URL ví Render. Chạy scripts/set-demo-wallet-url.ps1 -PublicUrl URL_HTTPS_THUC_TE trước.'}
& (Join-Path $PSScriptRoot 'set-demo-wallet-url.ps1') -PublicUrl $PublicUrl
$publicOrigin=([Uri]$PublicUrl).GetLeftPart([UriPartial]::Authority)
if (Test-Path -LiteralPath $statePath) {
    $oldState=Get-Content -LiteralPath $statePath -Raw | ConvertFrom-Json
    foreach($entry in @(
        @{id=$oldState.appPid;started=$oldState.appStarted;path=$oldState.jar},
        @{id=$oldState.tunnelPid;started=$oldState.tunnelStarted;path=$oldState.tunnel}
    )) {
        if(!$entry.id -or !$entry.path -or !$entry.started){continue}
        $oldProcess=Get-CimInstance Win32_Process -Filter "ProcessId=$($entry.id)" -ErrorAction SilentlyContinue
        if($oldProcess -and $oldProcess.CommandLine -and
            $oldProcess.CommandLine.IndexOf($entry.path,[StringComparison]::OrdinalIgnoreCase) -ge 0 -and
            [Math]::Abs((([datetime]$oldProcess.CreationDate).ToUniversalTime()-([datetime]$entry.started).ToUniversalTime()).TotalMilliseconds) -lt 1) {
            throw 'Web do script quản lý đang chạy. Dùng scripts/stop-demo-wallet.ps1 trước khi bật lại.'
        }
    }
}
if(Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue) {
    throw "Cổng $Port đang bận. Dừng web hiện tại hoặc chọn -Port khác."
}
if(!$JavaHome) {
    $javaCommand=Get-Command java.exe -ErrorAction Stop
    $JavaHome=Split-Path -Parent (Split-Path -Parent $javaCommand.Source)
}
$javaPath=Join-Path $JavaHome 'bin\java.exe'
if(!(Test-Path -LiteralPath $javaPath)){throw 'Không tìm thấy Java; truyền -JavaHome đường dẫn JDK 21.'}
$versionOutput=(& $javaPath --version | Out-String)
if($versionOutput -notmatch '(?:^|\n)(?:openjdk|java) (21|2[2-9]|[3-9][0-9])') {throw 'Cần JDK 21 trở lên.'}
New-Item -ItemType Directory -Path $runtimeRoot -Force | Out-Null
if(!$SkipBuild) {
    $previousJava=$env:JAVA_HOME;$previousPath=$env:Path
    Push-Location $projectRoot
    try {
        $env:JAVA_HOME=$JavaHome;$env:Path=(Join-Path $JavaHome 'bin')+';'+$previousPath
        & mvn -B -ntp -DskipTests package
        if($LASTEXITCODE -ne 0){throw 'Build thất bại. Chưa bật web.'}
    }finally{$env:JAVA_HOME=$previousJava;$env:Path=$previousPath;Pop-Location}
}
if(!(Test-Path -LiteralPath $jarPath)){throw 'Chưa có file build. Chạy lại không dùng -SkipBuild.'}
$state=[ordered]@{mode='local-web';appPid=$null;appStarted=$null;tunnelPid=$null;tunnelStarted=$null;url=$publicOrigin;port=$Port;walletPort=$null;jar=$jarPath;tunnel=$null}
try {
    $app=Start-Process -FilePath $javaPath -ArgumentList @('-jar',('"' + $jarPath + '"'),
        '--spring.profiles.active=cloud',"--server.port=$Port",'--server.address=127.0.0.1',
        "--app.base-url=http://localhost:$Port",'--demo-wallet.enabled=true',
        '--demo-wallet.require-public-url=true','--demo-wallet.phone-enabled=false',
        '--demo-wallet.gateway-enabled=false',"--demo-wallet.public-base-url=$publicOrigin") -WorkingDirectory $projectRoot -WindowStyle Hidden -PassThru -RedirectStandardOutput (Join-Path $runtimeRoot 'app.log') -RedirectStandardError (Join-Path $runtimeRoot 'app.err.log')
    $state.appPid=$app.Id;$state.appStarted=$app.StartTime.ToUniversalTime().ToString('o')
    $state | ConvertTo-Json | Set-Content -LiteralPath $statePath -Encoding UTF8
    $deadline=(Get-Date).AddSeconds(120);$ready=$false
    while((Get-Date) -lt $deadline) {
        if($app.HasExited){throw 'Web chưa khởi động được. Xem target/demo-wallet/app.log.'}
        try {
            $reply=Invoke-WebRequest -Uri "http://127.0.0.1:$Port/" -UseBasicParsing -TimeoutSec 3
            if($reply.StatusCode -eq 200){$ready=$true;break}
        }catch{}
        Start-Sleep -Seconds 1
    }
    if(!$ready){throw 'Web chưa sẵn sàng trong 120 giây. Xem nhật ký kết nối database cloud.'}
    Write-Host "Web rạp local: http://localhost:$Port"
    Write-Host "Ví MoMo GIẢ LẬP online: $publicOrigin/demo-wallet"
    Write-Host 'QR mở thẳng giao dịch HTTPS trên Render. Local chỉ tạo QR và theo dõi kết quả.'
    Write-Host '© Nhóm 8. Không phát sinh tiền thật. Dừng web local bằng scripts/stop-demo-wallet.ps1.'
}catch{
    if($app -and !$app.HasExited){$app.Kill();$app.WaitForExit(5000)|Out-Null}
    throw
}
