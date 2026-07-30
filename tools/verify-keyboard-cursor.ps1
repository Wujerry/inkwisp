param([int]$Port = 9222)

$ErrorActionPreference = "Stop"
adb shell am force-stop com.inkwisp.app
adb shell am start -n com.inkwisp.app/.MainActivity | Out-Null
Start-Sleep -Seconds 2
$pidValue = (adb shell pidof com.inkwisp.app).Trim()
if (-not $pidValue) { throw "InkWisp did not start." }
adb forward "tcp:$Port" "localabstract:webview_devtools_remote_$pidValue" | Out-Null
$target = $null
for ($attempt = 0; $attempt -lt 10 -and -not $target; $attempt += 1) {
    try {
        $target = (Invoke-RestMethod "http://127.0.0.1:$Port/json" -TimeoutSec 3)
            .Where({ $_.type -eq "page" }) | Select-Object -First 1
    } catch {
        Start-Sleep -Milliseconds 500
    }
}
if (-not $target) { throw "InkWisp WebView debug target is unavailable." }
$env:CDP_WS = $target.webSocketDebuggerUrl
node (Join-Path $PSScriptRoot "verify-keyboard-cursor.mjs") setup
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
adb shell input tap 350 800
Start-Sleep -Seconds 2
node (Join-Path $PSScriptRoot "verify-keyboard-cursor.mjs") setup
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
Start-Sleep -Milliseconds 700
node (Join-Path $PSScriptRoot "verify-keyboard-cursor.mjs") measure
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
adb shell screencap -p /sdcard/inkwisp-keyboard-cursor.png
adb pull /sdcard/inkwisp-keyboard-cursor.png captures/inkwisp-keyboard-cursor.png | Out-Null
