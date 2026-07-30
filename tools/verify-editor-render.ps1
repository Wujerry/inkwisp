param(
    [int]$MinimumInkPixels = 80
)

$ErrorActionPreference = "Stop"
$repoRoot = Split-Path -Parent $PSScriptRoot
$localProperties = Join-Path $repoRoot "local.properties"
$sdkLine = Get-Content -LiteralPath $localProperties | Where-Object { $_ -like "sdk.dir=*" } | Select-Object -First 1
if (-not $sdkLine) {
    throw "sdk.dir is missing from local.properties."
}

$sdk = ($sdkLine -replace "^sdk.dir=", "") -replace "\\:", ":" -replace "\\\\", "\"
$adb = Join-Path $sdk "platform-tools\adb.exe"
$capture = Join-Path $env:TEMP "inkwisp-editor-render.png"
$apk = Join-Path $repoRoot "app\build\outputs\apk\debug\app-debug.apk"

if (-not (Test-Path -LiteralPath $apk)) {
    throw "Debug APK is missing. Run .\\gradlew.bat :app:assembleDebug first."
}

& $adb install -r $apk | Out-Null
& $adb shell pm clear com.inkwisp.app | Out-Null
& $adb shell am start -W -n com.inkwisp.app/.MainActivity | Out-Null
Start-Sleep -Seconds 3
& $adb shell input tap 540 2110 | Out-Null
Start-Sleep -Seconds 2
& $adb shell input tap 260 300 | Out-Null
Start-Sleep -Milliseconds 300
& $adb shell input text INKWISPTEST | Out-Null
Start-Sleep -Milliseconds 700
& $adb shell screencap -p /sdcard/inkwisp-editor-render.png | Out-Null
& $adb pull /sdcard/inkwisp-editor-render.png $capture | Out-Null

Add-Type -AssemblyName System.Drawing
$image = [System.Drawing.Bitmap]::FromFile($capture)
try {
    $left = 20
    $right = $image.Width - 20
    $top = [Math]::Min(230, $image.Height - 1)
    $bottom = [Math]::Min(820, $image.Height)
    $inkPixels = 0
    for ($y = $top; $y -lt $bottom; $y += 2) {
        for ($x = $left; $x -lt $right; $x += 2) {
            $pixel = $image.GetPixel($x, $y)
            $luminance = (0.2126 * $pixel.R) + (0.7152 * $pixel.G) + (0.0722 * $pixel.B)
            if ($luminance -lt 105) {
                $inkPixels++
            }
        }
    }
} finally {
    $image.Dispose()
}

Write-Host "Editor ink pixels: $inkPixels (minimum: $MinimumInkPixels)"
if ($inkPixels -lt $MinimumInkPixels) {
    throw "Editor content is present in state but not visibly rendered."
}
