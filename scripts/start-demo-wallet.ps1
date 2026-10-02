# © 2026 Nhóm 8. HTTPS miễn phí cho web MoMo GIẢ LẬP.
[CmdletBinding()]
param([int]$Port=8082,[int]$WalletPort=8084,[string]$JavaHome=$env:JAVA_HOME,[switch]$SkipBuild)
$ErrorActionPreference='Stop'
$projectRoot=Split-Path -Parent $PSScriptRoot
$runtimeRoot=Join-Path $projectRoot 'target\demo-wallet'
$statePath=Join-Path $runtimeRoot 'runtime.json'
$jarPath=Join-Path $projectRoot 'target\cinema-booking-0.0.1-SNAPSHOT.jar'
$tunnelPath=Join-Path $projectRoot 'target\tools\cloudflared.exe'
if ($Port -lt 1024 -or $Port -gt 65535) {throw 'Cổng phải từ 1024 đến 65535.'}
if (!(Test-Path -LiteralPath (Join-Path $projectRoot 'application-secrets-cloud.properties'))) {
    throw 'Cần cấu hình application-secrets-cloud.properties theo file example trước.'
}
if (Test-Path -LiteralPath $statePath) {
    $oldState=Get-Content -LiteralPath $statePath -Raw | ConvertFrom-Json
    foreach($ownedId in @($oldState.appPid,$oldState.tunnelPid)) {
        if($ownedId -and (Get-Process -Id $ownedId -ErrorAction SilentlyContinue)) {
            throw 'Bản demo đang chạy. Dùng scripts/stop-demo-wallet.ps1 trước khi bật lại.'
        }
    }
}
if ($WalletPort -lt 1024 -or $WalletPort -gt 65535 -or $WalletPort -eq $Port){throw 'Cổng ví phải hợp lệ và khác cổng web rạp.'}
if(Get-NetTCPConnection -LocalPort $Port,$WalletPort -State Listen -ErrorAction SilentlyContinue) {
    throw "Cổng $Port đang được chương trình khác sử dụng. Dừng chương trình đó hoặc chọn -Port khác."
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
New-Item -ItemType Directory -Path (Split-Path -Parent $tunnelPath) -Force | Out-Null
if(!(Test-Path -LiteralPath $tunnelPath)) {
    Write-Host 'Đang tải công cụ chính thức Cloudflare (miễn phí)...'
    Invoke-WebRequest -Uri 'https://github.com/cloudflare/cloudflared/releases/latest/download/cloudflared-windows-amd64.exe' -OutFile $tunnelPath -UseBasicParsing
}
if(!$SkipBuild) {
    $previousJava=$env:JAVA_HOME;$previousPath=$env:Path
    Push-Location $projectRoot
    try {
        $env:JAVA_HOME=$JavaHome;$env:Path=(Join-Path $JavaHome 'bin')+';'+$previousPath
        & mvn -B -ntp -DskipTests package
        if($LASTEXITCODE -ne 0){throw 'Build thất bại. Chưa mở bản demo.'}
    }finally{$env:JAVA_HOME=$previousJava;$env:Path=$previousPath;Pop-Location}
}
if(!(Test-Path -LiteralPath $jarPath)){throw 'Chưa có file build. Chạy lại không dùng -SkipBuild.'}
$tunnelLog=Join-Path $runtimeRoot 'tunnel.log'
$appLog=Join-Path $runtimeRoot 'app.log'
$state=[ordered]@{appPid=$null;appStarted=$null;tunnelPid=$null;tunnelStarted=$null;url=$null;port=$Port;walletPort=$WalletPort;jar=$jarPath;tunnel=$tunnelPath}
function Save-State { $state | ConvertTo-Json | Set-Content -LiteralPath $statePath -Encoding UTF8 }
try {
    $tunnel=Start-Process -FilePath $tunnelPath -ArgumentList @('tunnel','--no-autoupdate','--protocol','http2','--url',"http://127.0.0.1:$WalletPort") -WorkingDirectory $projectRoot -WindowStyle Hidden -PassThru -RedirectStandardOutput (Join-Path $runtimeRoot 'tunnel.out.log') -RedirectStandardError $tunnelLog
    $state.tunnelPid=$tunnel.Id;$state.tunnelStarted=$tunnel.StartTime.ToUniversalTime().ToString('o');Save-State
    $deadline=(Get-Date).AddSeconds(60);$publicUrl=$null
    while((Get-Date) -lt $deadline) {
        if($tunnel.HasExited){throw 'Cloudflare chưa kết nối được; xem target/demo-wallet/tunnel.log.'}
        if(Test-Path -LiteralPath $tunnelLog) {
            $tunnelText=Get-Content -LiteralPath $tunnelLog -Raw
            if($tunnelText){
                $match=[regex]::Match($tunnelText,'https://[a-z0-9-]+\.trycloudflare\.com')
                if($match.Success){$publicUrl=$match.Value;break}
            }
        }
        Start-Sleep -Milliseconds 500
    }
    if(!$publicUrl){throw 'Chưa nhận được HTTPS miễn phí trong 60 giây. Thử lại sau.'}
    $state.url=$publicUrl;Save-State
    $app=Start-Process -FilePath $javaPath -ArgumentList @('-Dsun.net.httpserver.maxReqTime=15','-Dsun.net.httpserver.maxRspTime=15','-jar',('"' + $jarPath + '"'),'--spring.profiles.active=cloud',"--server.port=$Port",'--server.address=127.0.0.1','--demo-wallet.enabled=true','--demo-wallet.gateway-enabled=true',"--demo-wallet.gateway-port=$WalletPort","--demo-wallet.public-base-url=$publicUrl",'--server.forward-headers-strategy=framework','--app.mail.enabled=false') -WorkingDirectory $projectRoot -WindowStyle Hidden -PassThru -RedirectStandardOutput $appLog -RedirectStandardError (Join-Path $runtimeRoot 'app.err.log')
    $state.appPid=$app.Id;$state.appStarted=$app.StartTime.ToUniversalTime().ToString('o');Save-State
    $deadline=(Get-Date).AddSeconds(120);$ready=$false
    while((Get-Date) -lt $deadline) {
        if($app.HasExited){throw 'Ứng dụng chưa khởi động được. Kiểm tra migration và target/demo-wallet/app.log.'}
        try {$reply=Invoke-WebRequest -Uri "http://127.0.0.1:$WalletPort/demo-wallet" -UseBasicParsing -TimeoutSec 3
            if($reply.StatusCode -eq 200){$ready=$true;break}}catch{}
        Start-Sleep -Seconds 1
    }
    if(!$ready){throw 'Ứng dụng chưa sẵn sàng trong 120 giây. Xem nhật ký để kiểm tra kết nối cloud.'}
    Write-Host "Web rạp: http://localhost:$Port"
    Write-Host "Web điện thoại MoMo GIẢ LẬP: $publicUrl/demo-wallet"
    Write-Host 'Chọn ghế trên web rạp -> MoMo giả lập -> quét QR bằng camera điện thoại.'
    Write-Host 'HTTPS chỉ mở ví giả lập; không mở web rạp hay trang quản trị.'
    Write-Host 'HTTPS miễn phí; URL thay đổi mỗi lần bật. Cần giữ máy tính và bản demo hoạt động.'
    Write-Host '© Nhóm 8. Không phát sinh tiền thật. Dừng bằng scripts/stop-demo-wallet.ps1.'
}catch {
    foreach($ownedProcess in @($app,$tunnel)){if($ownedProcess -and !$ownedProcess.HasExited){$ownedProcess.Kill();$ownedProcess.WaitForExit(5000)|Out-Null}}
    throw
}
