param([int]$Port = 9224)

$ErrorActionPreference = "Stop"
$pidValue = (adb shell pidof com.inkwisp.app).Trim()
if (-not $pidValue) { throw "InkWisp is not running." }
adb forward "tcp:$Port" "localabstract:webview_devtools_remote_$pidValue" | Out-Null
$target = (Invoke-RestMethod "http://127.0.0.1:$Port/json").Where({ $_.type -eq "page" }) |
    Select-Object -First 1
if (-not $target) { throw "InkWisp WebView debug target is unavailable." }
$env:CDP_WS = $target.webSocketDebuggerUrl
node (Join-Path $PSScriptRoot "verify-markdown-editing.mjs")
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
