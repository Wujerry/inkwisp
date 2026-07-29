# InkWisp Implementation Plan

## Architecture

InkWisp uses a Kotlin and Jetpack Compose Native Shell with a bundled CodeMirror 6 Editor Core loaded in a local-only WebView.

The Native Shell owns:

- Android lifecycle and adaptive Compose UI
- Storage Access Framework permissions and filesystem operations
- autosave, conflict detection, recovery, and local revisions
- Workspace indexing, retrieval, and Wiki Link graph
- Model Connections, credentials, protocol adapters, and direct networking
- settings, localization, export, diagnostics, and future billing or ads

The Editor Core owns:

- Markdown text state, selections, commands, and undo transactions
- source-mode editing and instant-render decorations
- syntax-aware formatting and embedded renderers
- provisional Inline Prediction presentation and acceptance gestures
- a narrow event and command interface to the Native Shell

The WebView loads only versioned application assets. It cannot navigate to arbitrary remote content. The bridge accepts typed, size-bounded messages, exposes no general Android object surface, and treats rendered Markdown as untrusted content.

## Repository shape

```text
/
├── app/                  Android application and Compose Native Shell
├── editor/               TypeScript CodeMirror Editor Core
├── docs/                 Product specification and ADRs
├── gradle/               Gradle wrapper support
├── CONTEXT.md            Domain glossary
├── LICENSE
└── README.md
```

## Milestone 1: trustworthy editing vertical slice

Deliver a runnable APK containing:

- Compose application shell and adaptive navigation frame
- system folder and standalone-file selection
- persisted Workspace permission and recent-document state
- bundled local WebView and typed bridge
- source editing plus instant-render support for core CommonMark/GFM blocks
- autosave, crash recovery, undo/redo, and basic conflict detection
- English and Simplified Chinese resource structure
- initial paper-and-ink theme and reduced-motion plumbing

Gate:

- unit tests for storage state and bridge message validation
- Editor Core tests for text transactions and lossless round trips
- Android instrumentation test covering folder selection, edit, process recreation, and reload
- verified debug APK launch on an API 26 device profile and a current Android device profile

## Milestone 2: complete local Markdown workspace

Deliver:

- remaining supported Markdown extensions and lossless unsupported syntax behavior
- formatting controls, find/replace, outline, counts, and keyboard shortcuts
- images and Managed Attachments
- Workspace search and local index
- Wiki Links, Embedded Documents, backlinks, and missing-link navigation
- external edit conflict comparison and merge preview
- Local Revision browser and restore

Gate:

- parser and renderer fixtures for every supported construct
- round-trip corpus tests including unknown syntax
- conflict, attachment, index exclusion, and revision-retention tests
- performance tests for large documents and large Workspaces on constrained devices
- accessibility checks with font scaling and screen-reader semantics

## Milestone 3: provider-independent AI

Deliver:

- secure Model Connection management and device-bound secrets
- protocol adapters for OpenAI Chat Completions, OpenAI Responses, Anthropic Messages, Google Gemini, and custom OpenAI-compatible endpoints
- Capability Probe and optional model discovery
- automatic, cancellable Inline Prediction with ghost text and mobile/keyboard acceptance
- local Workspace retrieval, `@document` references, context disclosure, and exclusions
- Assisted Edits with diff preview, undo, and Saved Instructions
- configuration export without secrets

Gate:

- protocol contract fixtures with secret-redacted logs
- cancellation, stale-response, rate-limit, malformed-stream, and offline tests
- tests proving unaccepted predictions never modify source
- tests proving excluded files and secrets never enter model payloads or exports
- end-to-end verification against local mock servers for every protocol

## Milestone 4: release quality and Google Play preparation

Deliver:

- Markdown, HTML, PDF, and plain-text export
- final adaptive phone/tablet layouts, branding, icon, and purposeful motion
- onboarding, settings, privacy controls, and About/license screens
- optional content-free crash reporting path, kept disabled by default
- Android App Bundle build, release signing configuration placeholders, and store assets
- privacy policy and Google Play Data safety answers derived from the shipped build

Gate:

- full unit, Editor Core, instrumentation, and end-to-end suite
- verified API 26 and API 36 behavior plus representative phone and tablet layouts
- cold start, typing latency, large-file, index battery, and memory checks
- release AAB inspection and installed build smoke test
- dependency license inventory, secret scan, network endpoint audit, and Play pre-launch report review

## Completion rule

A milestone is complete only when its runnable artifact and listed gates pass. The first public release occurs only after all four milestones are complete; intermediate APKs are development evidence, not public product releases.
