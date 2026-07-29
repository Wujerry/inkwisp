param(
    [string]$KeystorePath = "$HOME\.inkwisp\inkwisp-upload.jks",
    [string]$Alias = "inkwisp-upload",
    [int]$ValidityDays = 10000
)

$ErrorActionPreference = "Stop"

# Windows PowerShell and the JDK can disagree about the active code page.
# Force UTF-8 for the terminal and English for keytool so every prompt remains readable.
$utf8 = [System.Text.UTF8Encoding]::new($false)
[Console]::InputEncoding = $utf8
[Console]::OutputEncoding = $utf8
$OutputEncoding = $utf8

if ($ValidityDays -lt 3650) {
    throw "ValidityDays must be at least 3650 (10 years)."
}

$keytool = if ($env:JAVA_HOME -and (Test-Path "$env:JAVA_HOME\bin\keytool.exe")) {
    "$env:JAVA_HOME\bin\keytool.exe"
} else {
    (Get-Command keytool.exe -ErrorAction Stop).Source
}

$resolvedPath = $ExecutionContext.SessionState.Path.GetUnresolvedProviderPathFromPSPath($KeystorePath)
if (Test-Path -LiteralPath $resolvedPath) {
    throw "Refusing to overwrite an existing keystore: $resolvedPath"
}

$parent = Split-Path -Parent $resolvedPath
New-Item -ItemType Directory -Force -Path $parent | Out-Null

Write-Host "Creating InkWisp upload keystore at: $resolvedPath"
Write-Host "keytool will ask for the password and publisher identity interactively."
Write-Host "Use a strong unique password and save it in a password manager."

& $keytool `
    "-J-Duser.language=en" `
    "-J-Duser.country=US" `
    -genkeypair `
    -v `
    -keystore $resolvedPath `
    -storetype PKCS12 `
    -alias $Alias `
    -keyalg RSA `
    -keysize 4096 `
    -validity $ValidityDays

if ($LASTEXITCODE -ne 0 -or -not (Test-Path -LiteralPath $resolvedPath)) {
    throw "keytool failed with exit code $LASTEXITCODE."
}

$keystore = Get-Item -LiteralPath $resolvedPath
if ($keystore.Length -eq 0) {
    throw "keytool did not create a valid keystore file."
}

Write-Host ""
Write-Host "Keystore created. Next:"
Write-Host "1. Copy keystore.properties.example to keystore.properties."
Write-Host "2. Set storeFile to the absolute path above."
Write-Host "3. Set storePassword and keyPassword locally (PKCS12 normally uses the same password)."
Write-Host "4. Keep the .jks file and keystore.properties outside version control."
Write-Host "5. Back up the keystore and password in two secure locations."
