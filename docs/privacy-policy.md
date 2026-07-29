# InkWisp Privacy Policy

Effective date: 2026-07-29

InkWisp (续墨) is an open-source, local-first Markdown editor published by InkWisp contributors. This policy describes how the official Android application accesses and handles data.

## Local files

InkWisp accesses only the files and folders that a user selects through Android's system file picker. Documents, local search indexes, recovery copies, revision history, application settings, and model credentials are stored on the user's device. InkWisp does not operate an account, synchronization, analytics, or document-storage server.

Users can delete Workspace access through InkWisp or Android settings. Uninstalling InkWisp deletes app-private settings, credentials, indexes, recovery copies, and revisions; it does not delete user-owned Workspace documents or attachments.

## AI model services

AI features are optional and remain disabled until a user configures a Model Connection and accepts the in-app data-transfer disclosure. When invoked, InkWisp sends the active document excerpt, the selected text, and up to four locally retrieved or explicitly referenced Workspace passages directly from the device to the model endpoint configured by the user. A connection probe sends only fixed test text and no document content.

InkWisp contributors do not receive these requests or responses. The configured model provider processes data under its own privacy, retention, and security terms. Users should choose a provider and endpoint they trust. HTTPS is recommended; user-configured HTTP endpoints, intended for local services, are not encrypted in transit.

## Credentials

Model credentials are encrypted with a device-bound key held by Android Keystore. They are not displayed after storage and are never included in configuration exports, diagnostics, or Workspace files.

## Diagnostics

InkWisp does not collect behavioral analytics. The current release contains no remote crash-reporting or advertising SDK. If optional diagnostics are added in a future release, they will remain disabled by default and this policy and the Google Play Data safety declaration will be updated before release.

## Data sale and advertising

InkWisp does not sell user data. The current release contains no advertising. Any future advertising release must not use document content, filenames, Workspace paths, Model Connections, model requests, or model responses for advertising and must update this policy before publication.

## Children

InkWisp is a general-purpose writing tool and is not directed to children. It does not provide social, chat, or account features.

## Changes

Material changes will be published in the application and at the public URL used for the Google Play privacy-policy listing before the changes take effect.

## Contact

Publisher: JerryWu (GitHub: Wujerry)

Privacy inquiries can be submitted through the repository's dedicated privacy issue form: `https://github.com/Wujerry/inkwisp/issues/new?template=privacy.yml`. Do not include API keys, document contents, or other sensitive information in a public issue.

The bilingual public HTML version is `docs/privacy/index.html` and is published through GitHub Pages.
