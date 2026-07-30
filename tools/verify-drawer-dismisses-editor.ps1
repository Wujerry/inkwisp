$ErrorActionPreference = "Stop"

adb shell am start -W -n com.inkwisp.app/.MainActivity | Out-Null
Start-Sleep -Seconds 2
adb shell uiautomator dump /sdcard/inkwisp-drawer-setup.xml | Out-Null
$setup = adb shell cat /sdcard/inkwisp-drawer-setup.xml
if ($setup -match "关闭导航菜单|Close navigation menu") {
    adb shell input keyevent 4 | Out-Null
    Start-Sleep -Milliseconds 500
}
adb shell input tap 300 330 | Out-Null
Start-Sleep -Seconds 2

$before = (adb shell dumpsys input_method) -join "`n"
if ($before -notmatch "mInputShown=true") {
    throw "The editor did not open the keyboard, so the drawer regression setup is invalid."
}

adb shell input tap 70 125 | Out-Null
Start-Sleep -Milliseconds 700
$after = (adb shell dumpsys input_method) -join "`n"
if ($after -match "mInputShown=true") {
    throw "Opening the Workspace drawer left the editor keyboard active."
}

Write-Host "Drawer dismisses editor input: PASS"
