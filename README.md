# InkWisp · 续墨

InkWisp is a local-first Android Markdown editor with instant-render editing, user-configured model connections, inline prediction, and workspace-aware AI assistance.

## Current capabilities

- Kotlin, Jetpack Compose, and a bundled local CodeMirror 6 editor core
- user-owned folder Workspaces and standalone Markdown documents via Android's system picker
- instant-render and source editing modes with formatting commands
- autosave, crash recovery copies, 30 local revisions, and external-edit conflict protection
- local full-text Workspace indexing, Wiki Link backlinks, and bounded Workspace Context
- OpenAI Chat Completions, OpenAI Responses, Anthropic Messages, Google Gemini, and custom OpenAI-compatible connections
- Android Keystore-protected credentials, optional keyless local services, connection probes, inline prediction, and review-before-apply Assisted Edits
- Markdown, HTML, PDF, and plain-text export
- phone-language default (Chinese locales use Simplified Chinese; all others use English) with explicit English/Chinese switching
- adaptive phone/tablet layout, water-ink identity, bundled Newsreader + LXGW WenKai Lite typography, paper-and-ink light/dark theme, and reduced-motion behavior

InkWisp has no account system, proprietary backend, analytics, advertising SDK, or bundled model credentials.

## Build

Requirements:

- JDK 17
- Android SDK Platform 36 and Build Tools 36.0.0
- Node.js and npm

The Android build automatically installs and bundles Editor Core dependencies:

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug
```

Run Editor Core tests independently:

```powershell
Set-Location .\editor
npm ci
npm test
```

Debug APK output: `app/build/outputs/apk/debug/app-debug.apk`.

## Google Play release setup

- Create the private upload key with `./tools/create-upload-keystore.ps1`; detailed signing steps are in `docs/google-play-signing.md`.
- Publish `docs/` with GitHub Pages and use the resulting `/privacy/` HTML URL in Play Console; setup steps and required publisher placeholders are in `docs/github-pages-privacy-setup.md`.
- Never commit `keystore.properties`, the upload keystore, API keys, or publisher verification documents.

## Model credentials

Never commit an API key. Add Model Connections in the running app. Credentials remain encrypted under Android Keystore and are never included in settings exports.

## License

MIT © 2026 InkWisp contributors.

Bundled font files retain their SIL Open Font License 1.1 terms; see `third_party/fonts/`.
