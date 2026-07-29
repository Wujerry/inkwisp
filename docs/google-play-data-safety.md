# Google Play Data Safety Working Notes

These notes describe the current build and are not a substitute for completing the Play Console form against the final artifact and chosen model providers.

## Current artifact inventory

- Permissions: `INTERNET` only
- File access: Android Storage Access Framework; no `MANAGE_EXTERNAL_STORAGE`, media, contacts, location, camera, microphone, advertising ID, or account permission
- SDKs: AndroidX, Kotlin coroutines/serialization, OkHttp, and the bundled local CodeMirror Editor Core
- No analytics, ads, remote crash reporting, authentication, or InkWisp backend

## Conservative declaration guidance

When AI is enabled, the application transmits optional user-generated document content off-device to the user-configured model endpoint. Google Play defines off-device transmission as collection even when the developer does not receive it. Review the final form categories for:

- **Files and docs / other user-generated content**: optional; app functionality; sent only when the user enables or invokes AI
- **Data sharing**: assess the user-initiated-transfer exception against the final in-app disclosure and each supported provider; do not assume that direct BYOK transfer eliminates the declaration
- **Ephemeral processing**: do not claim this unless every supported provider and custom endpoint contractually processes requests only in memory and retains nothing
- **Encryption in transit**: do not claim all transmitted data is encrypted while user-configured HTTP endpoints remain supported
- **Deletion**: local app-private data is deleted by uninstall; provider-side deletion is governed by the provider selected by the user

All declarations must match `docs/privacy-policy.md` and be re-audited if advertising, diagnostics, new SDKs, or a backend are introduced.

## Before production submission

1. Host the final privacy policy at a public non-PDF URL and add the same link in the app.
2. Add the publisher's legal/public identity and monitored privacy contact.
3. Complete the Data safety form for every testing track except an internal-only track.
4. Re-run dependency, permission, network endpoint, and secret audits against the signed AAB.
5. Verify the AI disclosure remains prominent and requires affirmative consent.

