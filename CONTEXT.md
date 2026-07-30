# InkWisp Context

InkWisp is a local-first Markdown writing environment whose optional AI assistance continues text inside the editor.

## Language

**Local Document**:
A Markdown document owned by the user and stored on their device without requiring an InkWisp account or cloud service.
_Avoid_: Cloud note, server document

**Inline Prediction**:
An AI-generated continuation offered at the writing cursor that remains provisional until the user accepts it.
_Avoid_: Autocomplete, AI rewrite, generated document

**Prediction Language**:
The language inferred from content near the cursor for Inline Prediction, unless overridden for the Local Document or Workspace; it is independent of the application interface language.
_Avoid_: App language, model language, translation language

**Assisted Edit**:
An AI-proposed transformation of selected document content that remains a reviewable difference until the user explicitly applies it.
_Avoid_: Inline prediction, chat response, automatic rewrite

**Saved Instruction**:
A user-defined reusable instruction offered alongside built-in Assisted Edit actions.
_Avoid_: Prompt template, system prompt, macro

**Instant-render Editing**:
A Markdown editing mode that renders inactive content as formatted text while exposing the necessary Markdown markers for the block currently being edited. The underlying document remains standard Markdown.
_Avoid_: Rich-text conversion, WYSIWYG, preview mode

**Source Mode**:
An alternate editing mode that exposes the complete Markdown source without instant rendering.
_Avoid_: Raw document, developer mode

**Editor Core**:
The local CodeMirror-based component that displays and edits Markdown, including instant rendering and provisional Inline Predictions, without owning files, credentials, indexing, or model requests.
_Avoid_: Web app, browser editor, document store

**Native Shell**:
The Android application layer that owns Workspaces, files, recovery, Model Connections, local indexing, settings, and the constrained bridge to the Editor Core.
_Avoid_: Wrapper, host page, backend

**Supported Markdown**:
CommonMark and GitHub Flavored Markdown plus YAML front matter, footnotes, LaTeX math, Mermaid diagrams, and fenced code blocks. Unsupported syntax remains user-authored source and must survive editing unchanged.
_Avoid_: InkWisp Markdown, rich-text format, proprietary syntax

**Wiki Link**:
A preserved `[[document]]` or `[[document#heading]]` reference resolved within a Workspace, including references to documents that do not yet exist.
_Avoid_: Markdown link, URL, file path

**Embedded Document**:
A preserved `![[document]]` reference rendered from another Workspace document without copying that document's source into the active document.
_Avoid_: Include, attachment, pasted content

**Backlink**:
A Workspace reference from another document to the active document, derived from indexed Wiki Links rather than stored in either document.
_Avoid_: Incoming URL, related document, citation

**Workspace**:
A collection of Local Documents shown together in InkWisp. Every installation has one Managed Workspace and may also connect user-authorized folders.
_Avoid_: Notebook, vault, file picker

**Managed Workspace**:
The zero-setup default Workspace created by InkWisp for ordinary writing, where a document's first meaningful title supplies its default filename.
_Avoid_: Scratch folder, demo workspace, temporary library

**Connected Folder**:
A user-authorized filesystem folder whose Markdown documents InkWisp edits in place without importing copies into the Managed Workspace.
_Avoid_: External workspace, imported folder

**Standalone Document**:
A single Markdown file opened through the Android document picker outside a Workspace.
_Avoid_: Imported note, loose note

**Scratch Document**:
A short-lived recovery document used only while a Managed Workspace document is being created or recovered.
_Avoid_: Default note, permanent draft, unsaved file

**Managed Attachment**:
A media file copied into a Workspace attachment directory and referenced from Markdown by a relative path. It is not automatically deleted with a document because multiple documents may reference it.
_Avoid_: Embedded image, uploaded image, document asset

**Recovery Copy**:
An app-private copy of unsafely interrupted document state used only to recover edits after a crash or failed filesystem write.
_Avoid_: Backup, autosave file, synchronized copy

**Local Revision**:
An app-private, time-limited historical version of a Local Document that a user can inspect or restore without adding files to the Workspace.
_Avoid_: Git commit, cloud history, recovery copy

**Edit Conflict**:
A state where both InkWisp's unsaved document and its externally stored source have changed since their last shared version, requiring review before either version can replace the other.
_Avoid_: Save error, sync failure, duplicate document

## Model Connections

**Model Connection**:
A user-owned configuration containing a model protocol, endpoint, credentials, model identifier, and optional request settings used for AI assistance.
_Avoid_: Provider account, AI account, model preset

**Protocol Adapter**:
InkWisp's translation between its AI-assistance requests and a model API protocol shared by one or more providers.
_Avoid_: Provider integration, vendor plugin

**Prediction Protocol**:
The request and response contract used specifically for Inline Prediction, which may differ from the Model Connection protocol used for Assisted Edits while sharing the same provider credentials.
_Avoid_: Model protocol, prompt format, provider mode

**Prompt Format**:
The model-specific encoding that places a prefix and suffix around a fill-in-the-middle hole when a Prediction Protocol expects one formatted prompt rather than native prefix and suffix fields.
_Avoid_: System prompt, prediction protocol, chat template

**Custom Connection**:
A Model Connection whose endpoint, model identifier, and optional headers are supplied by the user rather than selected from a vendor-specific integration.
_Avoid_: Custom model, unknown provider

**Capability Probe**:
A content-free test request that checks whether a Model Connection can authenticate and which optional protocol behaviors it supports without reading a Local Document or Workspace.
_Avoid_: Model benchmark, health check, document request

**Direct Model Request**:
An AI-assistance request sent from the user's device to the selected Model Connection without an InkWisp proxy, account, or business backend.
_Avoid_: Proxied request, InkWisp API, cloud inference

## Workspace Assistance

**Workspace Index**:
An on-device index of user-authorized Workspace content used to find relevant passages without uploading the Workspace as a whole.
_Avoid_: Cloud index, model memory, knowledge base

**Workspace Context**:
The small set of locally retrieved or explicitly referenced Workspace passages included in a particular AI-assistance request.
_Avoid_: Entire workspace, training data, attachments

**Explicit Reference**:
A user's `@` reference to a Workspace document that requires its relevant content to be considered for the current assistance request.
_Avoid_: File upload, link

**Indexable File**:
A Markdown or user-approved plain-text file within a Workspace that is eligible for local retrieval, subject to configured extension, exclusion, and size rules.
_Avoid_: Attachment, workspace file, imported file
