# InkWisp Product Specification

## Product identity

- English name: **InkWisp**
- Simplified Chinese name: **续墨**
- Android application ID: `com.inkwisp.app`
- Primary launch channel: Google Play
- License: MIT, copyright InkWisp contributors
- Default interface language: English; Simplified Chinese is selectable and persisted
- Launcher label: `续墨` on Simplified Chinese systems and `InkWisp` elsewhere

InkWisp is a local-first Android Markdown editor built around instant-render editing and optional user-configured AI assistance. It requires no InkWisp account or backend.

## Product principles

1. User-owned files are the source of truth.
2. Standard Markdown must survive every edit without destructive normalization.
3. Editing remains complete and usable offline; AI is optional.
4. AI requests are direct, inspectable, and bounded by explicit context rules.
5. The interface stays visually minimal even as capability grows.
6. Automatic behavior must never silently destroy or replace user work.

## Documents and workspaces

- A Workspace is a folder authorized through Android's system file access framework.
- InkWisp edits files in place and persists granted access where the provider permits it.
- Users may open Standalone Documents without creating a Workspace.
- A single app-private Scratch Document allows immediate writing before storage is selected; it becomes a Local Document only after an explicit save.
- Workspaces support creation, rename, move, duplicate, delete, recent documents, file tree navigation, outline navigation, and full-text search.
- Built-in cloud synchronization and Git are out of scope. External document providers and synchronization tools may modify files.
- Externally modified files must never be overwritten silently. Conflicts offer reload, keep current content, save as copy, and previewed non-overlapping merge.

## Editing experience

### Editing modes

- Instant-render Editing is the default, with Typora-style formatting of inactive blocks and necessary Markdown markers exposed for the active block.
- Source Mode exposes the complete source.
- Both modes edit the same Markdown string and share undo history where technically safe.
- Unsupported syntax remains present and editable as source.

### Markdown compatibility

- CommonMark
- GitHub Flavored Markdown, including tables, task lists, strikethrough, and autolinks
- YAML front matter
- Footnotes
- Inline and block LaTeX math
- Mermaid diagrams
- Fenced code blocks with syntax highlighting
- Wiki Links: `[[document]]` and `[[document#heading]]`
- Embedded Documents: `![[document]]`
- Derived backlinks and missing-link navigation

### Editing tools

- Context-sensitive formatting controls for headings, emphasis, links, images, lists, quotes, tables, code, math, and diagrams
- Find and replace, outline, word and character counts, undo and redo
- Android share/open-with integration
- Physical keyboard shortcuts where a conventional equivalent exists
- Accessibility semantics, touch targets, contrast, font scaling, screen-reader navigation, and reduced-motion support

### Attachments

- Pasted or selected media is copied by default to a configurable Workspace attachment directory, initially `assets/` beside the document.
- Markdown uses relative paths to Managed Attachments.
- Users may choose to reference an existing file instead of copying it.
- Deleting a document never automatically deletes attachments because they may be shared.

## Saving, recovery, and export

- Edits save automatically, with an explicit save action available.
- A Recovery Copy protects against crashes and failed filesystem writes.
- App-private Local Revisions retain the latest 30 versions for up to 30 days.
- Revision data does not add hidden history files to a Workspace.
- Export occurs entirely on the device and supports original Markdown, styled standalone HTML, configurable PDF, and plain text.
- DOCX export is not part of the first release.

## AI assistance

### Model Connections

- Users may create and switch among multiple Model Connections.
- Supported protocol adapters initially cover OpenAI Chat Completions, OpenAI Responses, Anthropic Messages, Google Gemini, and configurable OpenAI-compatible endpoints.
- Each connection may define its endpoint, credentials, model identifier, supported request settings, and optional headers.
- No default provider is required for the first release.
- A Capability Probe validates authentication and optional protocol behavior using fixed content that never reads a document.
- Where supported, InkWisp retrieves a model list; manual configuration remains available when discovery is unavailable or incorrect.

### Credential handling

- Credentials are encrypted under Android Keystore protection and remain device-bound.
- Configuration exports may contain non-secret connection settings and Saved Instructions but never API keys or other credentials.
- Credentials are masked, independently replaceable, and deletable.
- Model requests go directly from the device to the configured service. InkWisp has no model proxy or business backend.

### Inline Prediction

- Once a connection is configured, prediction is enabled automatically by default and can be disabled globally or per Workspace.
- Prediction begins after approximately 700 ms of input inactivity and is cancelled when the document or cursor state changes.
- Provisional continuation appears as ghost text at the cursor and never enters the document until accepted.
- Swipe right or physical `Tab` accepts the whole prediction; a contextual control accepts the next word or Chinese phrase; continued typing, Back, or physical `Esc` dismisses it.
- Prediction language is inferred from content near the cursor, independent of interface language, with Workspace and document overrides.
- The app must remain fully editable while the network is unavailable, credentials are missing, a provider rate-limits requests, or a prediction fails.

### Workspace Context

- InkWisp builds a local index of Markdown and approved plain-text files.
- Default indexable extensions are `.md`, `.markdown`, and `.txt`; users may adjust extensions, exclusions, folders, and file-size limits.
- Images are rendered in documents but are not OCR-indexed. PDF, Office, and other complex formats are not indexed in the first release.
- Each request sends only a bounded set of locally retrieved passages, never an entire Workspace.
- Users may require context through `@document` references.
- The UI discloses which Workspace files contributed context and allows exclusion of a file or folder.

### Assisted Edits

- Selecting text offers Rewrite, Shorten, Expand, Translate, Fix grammar, and Custom instruction.
- Users may create Saved Instructions.
- Results appear as a reviewable difference and replace source only after explicit confirmation.
- Applying an Assisted Edit is undoable.
- InkWisp does not add a permanent chat home or chat sidebar in the first release.

## Interface and brand

### Layout

- Phones open the most recent document into a focused single-column editor.
- A left drawer contains Workspace switching, file tree, search, outline, and backlinks.
- Tablets and wide landscape layouts adapt to a navigation-and-editor split view.
- Formatting controls appear near the software keyboard while editing rather than occupying permanent screen space.

### Visual language

- The design uses warm paper white and near-black ink in light mode, and ink black with soft gray-white text in dark mode.
- A restrained vermilion accent identifies the cursor, selection, and key states.
- Hierarchy comes from typography and whitespace, not stacked cards, gradients, or heavy shadows.
- Theme defaults to the system and can be set to light or dark.
- Motion communicates state and spatial relationships in approximately 160–240 ms, never blocks input, avoids decorative loops and launch-page spectacle, and follows the system reduced-motion preference.
- The icon is a single ink drop rising into a light brush stroke that subtly forms a `W`, with a small vermilion detail and no text.

## Onboarding

- No sign-in or model setup is required.
- At most three concise pages explain local files, instant-render editing, and optional AI.
- The user can immediately write in a Scratch Document or choose a Workspace.
- AI stays unobtrusively unavailable until a Model Connection exists; the first AI invocation offers contextual setup.

## Privacy and diagnostics

- InkWisp collects no behavioral analytics.
- Crash reporting is off by default and requires explicit opt-in.
- Diagnostic reports contain only app version, Android version, broad device category, and scrubbed stack information.
- Users can preview a report and erase diagnostic data.
- Document content and paths, filenames, credentials, Model Connection data, model requests and responses, and writing activity never enter diagnostics.

## Business model

- The first release is fully free and contains no advertising SDK or payment flow.
- A future version may show non-personalized static ads only at the bottom of the Workspace drawer and on settings surfaces.
- Ads never appear in document content, prediction, file-opening, startup, exit, interstitial, or rewarded flows and never use writing data for targeting.
- A future one-time purchase disables advertising requests entirely.

## Platform and release constraints

- Android phone and tablet application
- Minimum Android 8.0 / API 26
- Target Android 16 / API 36 for the planned Google Play release
- Distributed as an Android App Bundle for production and an APK for local verification
- English store listing first, with Simplified Chinese app resources included

## Explicit first-release non-goals

- InkWisp accounts or proprietary backend
- Built-in cloud synchronization
- Built-in Git client
- Provider-hosted Workspace indexing
- OCR and indexing of image, PDF, or Office content
- DOCX export
- Permanent AI chat interface
- Ads, subscriptions, or billing in the initial release

