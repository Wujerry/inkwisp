# Google Play upload signing

InkWisp uses a private **upload key** to sign the AAB sent to Google Play. With Play App Signing enabled, Google protects the separate app-signing key used for installs.

## Create the upload keystore

From the repository root in PowerShell:

```powershell
$env:JAVA_HOME = "C:\Users\wujer\AppData\Local\Programs\Android Studio\jbr"
.\tools\create-upload-keystore.ps1
```

The script intentionally does not accept a password argument, so the password does not appear in shell history or process listings. `keytool` asks for it interactively. Use the real publisher name or organization in the certificate identity prompts.

The default output is `%USERPROFILE%\.inkwisp\inkwisp-upload.jks`. Do not put the keystore inside the repository.

## Connect it to the Android build

1. Copy `keystore.properties.example` to `keystore.properties`.
2. Replace `storeFile` with the absolute keystore path.
3. Add the password locally as both `storePassword` and `keyPassword` for the default PKCS12 keystore.
4. Build the signed bundle:

```powershell
.\gradlew.bat :app:bundleRelease
```

The bundle is written to `app/build/outputs/bundle/release/app-release.aab`.

Both `keystore.properties` and `*.jks` are ignored by this repository. Back up the keystore and its password separately. Losing the upload key is recoverable through Play Console, but recovery delays releases and requires account verification.

