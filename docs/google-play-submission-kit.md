# InkWisp Google Play submission kit

Verified against the Android project and current Google Play guidance on 2026-07-30.

## 1. App identity

| Play Console field | Value |
| --- | --- |
| Package name | `com.inkwisp.app` |
| Default app name | `InkWisp` |
| Simplified Chinese app name | `续墨` |
| Default language | English (United States) |
| App or game | App |
| Category | Productivity |
| Pricing | Free |
| Contains ads | No |
| Account required | No |
| Version name | `0.1.0` |
| Version code | `1` |
| Minimum Android | Android 8.0 / API 26 |
| Target Android | Android 16 / API 36 |
| Developer website | `https://wujerry.github.io/inkwisp/` |
| Privacy policy | `https://wujerry.github.io/inkwisp/privacy/` |
| Source code | `https://github.com/Wujerry/inkwisp` |
| License | MIT |

The privacy-policy URL was publicly reachable without authentication on 2026-07-30.

## 2. English store listing

### App name

`InkWisp`

### Short description

`A local-first Markdown editor with bring-your-own-model inline prediction.`

### Full description

Write without breaking your flow.

InkWisp is a local-first Markdown editor for Android, built around inline prediction. Pause while writing and a short continuation can appear directly at the cursor. Keep typing to ignore it, or accept it when it fits.

Markdown that stays yours

- Edit standard Markdown in an instant-render, Typora-style writing view
- Switch to source mode whenever you want to see every marker
- Open individual documents or connect folders through Android's system file picker
- Autosave, recovery copies, revision history, search, Wiki Links, backlinks, and export

Bring your own model

- Configure OpenAI, Anthropic, Gemini, DeepSeek, Mistral, compatible gateways, or local services
- Fetch the model list when the provider supports discovery, or enter any model ID manually
- Use native or compatible fill-in-the-middle prediction formats
- Review assisted edits before applying them

Local-first by design

InkWisp has no account, proprietary synchronization service, analytics SDK, advertising SDK, or bundled API key. Documents and model credentials remain on your device. When you enable an AI feature, the relevant writing context is sent directly to the model endpoint you selected under that provider's terms.

InkWisp is free and open source under the MIT License.

## 3. Simplified Chinese store listing

### 应用名称

`续墨`

### 简短说明

`本地优先的 Markdown 编辑器，用你自己的模型获得行内续写。`

### 完整说明

写作不必被工具打断。

续墨是一款以行内预测为核心的 Android Markdown 编辑器。写作时稍作停顿，续写建议会直接出现在光标处；继续输入即可忽略，合适时再接受。

Markdown 始终属于你

- 使用类似 Typora 的即时渲染模式编辑标准 Markdown
- 随时切换源码模式，查看完整标记
- 通过 Android 系统文件选择器打开单个文档或连接文件夹
- 支持自动保存、恢复副本、版本历史、搜索、Wiki Link、反向链接和导出

使用你自己的模型

- 可配置 OpenAI、Anthropic、Gemini、DeepSeek、Mistral、兼容网关或本地服务
- 服务支持时自动获取模型列表，也始终允许手动填写模型 ID
- 支持原生及兼容的 FIM 行内预测格式
- AI 改写先预览，再决定是否应用

本地优先

续墨没有账户系统、专有同步服务、分析 SDK、广告 SDK，也不内置 API Key。文档和模型凭据保存在设备上。启用 AI 功能后，相关写作上下文会直接发送到你选择的模型服务，并受该服务条款约束。

续墨完全免费，并以 MIT License 开源。

## 4. Release notes

### English

`First public release. Includes instant-render Markdown editing, local workspaces, inline prediction with user-configured models, assisted edits, search, revisions, and export.`

### 简体中文

`首个公开版本：支持 Markdown 即时渲染、本地工作区、自定义模型行内续写、AI 改写、搜索、版本历史和导出。`

## 5. App content answers

Use these answers for the current build. Re-audit before changing SDKs, adding ads, accounts, cloud sync, diagnostics, or a backend.

| Questionnaire | Suggested answer |
| --- | --- |
| App access | All functionality is available without login. AI is optional and uses a model credential supplied by the user. No reviewer credential is required. |
| Ads | No, the current app contains no ads or advertising SDK. |
| Target audience | 18 and over for the first release, because users may connect unrestricted third-party generative models. |
| News app | No |
| Government app | No |
| Health app | No |
| Financial features | No |
| Social or dating features | No |
| User-to-user communication | No |
| Account creation | No |
| Account deletion URL | Not applicable; InkWisp has no account system. |
| Sensitive permissions declaration | Not required for the current artifact; it requests only `INTERNET`. |

### Reviewer notes

`InkWisp is usable immediately without an account. Open the included managed workspace or create a Markdown document to review editing, search, revisions, and export. AI features are optional BYOK features and remain disabled until the user configures a model endpoint, provides its credential, and accepts the in-app transfer disclosure. No demo login is required.`

## 6. Data Safety form

Google defines off-device transmission as collection even when InkWisp itself does not operate the receiving server. Use the following conservative declaration for the optional BYOK AI path.

| Question | Suggested answer |
| --- | --- |
| Does the app collect or share required user data types? | Yes, only when the user enables or invokes AI. |
| Files and docs | Collected and shared; optional; app functionality; conservatively mark as linked to the model-service account; not ephemeral. |
| Other user-generated content | Declare if Play Console classifies typed Markdown excerpts here instead of Files and docs; do not duplicate the same payload in both categories without reviewing the form wording. |
| User IDs | Conservatively declare when the user-supplied API credential identifies an account at the selected provider; optional; app functionality; linked to the user. |
| Data sold | No |
| Data used for advertising | No |
| Encryption in transit | No for the global declaration while user-configured HTTP endpoints are supported. Cloud presets use HTTPS. |
| Data deletion request mechanism | No developer-side request mechanism. Users delete local documents/connections themselves or uninstall; provider-side deletion follows the selected provider's terms. |
| Independent security review | No |

Do not claim ephemeral processing for arbitrary custom providers. Whether third-party transmission qualifies for a user-initiated-transfer or service-provider sharing exception requires reviewing the final Play Console wording and each provider relationship; this kit intentionally does not assume the exemption.

## 7. Store assets

Ready in `docs/google-play-assets/`:

- [x] 512 x 512, 32-bit PNG store icon, at most 1 MB: `icon-512.png`.
- [x] 1024 x 500 PNG feature graphic: `feature-graphic-1024x500.png`.
- [x] Three 1080 x 2400 phone screenshots showing the WYSIWYG editor, AI assistance, and model connection/model discovery UI.
- [x] Checked for API keys, endpoint credentials, emulator debug overlays, misleading rankings, prices, and unsupported claims.

See `docs/google-play-assets/README.md` for the exact Play Console upload mapping.

## 8. Developer-account information you must supply

These values cannot be invented or committed publicly:

- [ ] Legal name and legal address matching the Google Payments profile.
- [ ] Verified private contact email and phone number for Google.
- [ ] Monitored public developer/support email shown on Google Play.
- [ ] For an organization account: verified public developer phone number and D-U-N-S details when requested.
- [ ] For a new personal account: identity document and physical Android device verification when Play Console requests them.
- [ ] Complete the Android developer/package-name verification tasks shown in Play Console.

Recommended public developer name: `JerryWu` (keep it consistent with the privacy policy and GitHub identity).

## 9. Upload and rollout order

1. Create the app in Play Console with package `com.inkwisp.app`; a published package name cannot be changed later.
2. Complete developer identity, contact, device, and package-name verification tasks.
3. Upload the signed `app-release.aab` and enroll in Play App Signing.
4. Fill Store listing, App access, Ads, Target audience, Content rating, and Data Safety using this kit.
5. Add the public privacy-policy URL and monitored support email.
6. Upload the required icon, feature graphic, and screenshots.
7. Start with Internal testing. If the personal developer account was created after 2023-11-13, run a Closed test with at least 12 continuously opted-in testers for 14 days before applying for production access.
8. Review Pre-launch report, Android vitals, device compatibility, and policy warnings.
9. Submit a staged production rollout only after all declarations match the uploaded AAB.

## 10. Current audit status

- Automated Play-policy audit: compliant; no active policy risks detected.
- Manifest permission inventory: `INTERNET` only.
- No login or restricted reviewer area.
- No analytics, advertising, remote crash reporting, or developer-operated backend.
- The audit cannot guarantee approval; Google Play review and the final console declarations remain authoritative.

Official references:

- Data Safety: <https://support.google.com/googleplay/android-developer/answer/10787469>
- Store listing assets: <https://support.google.com/googleplay/android-developer/answer/9866151>
- App review content: <https://support.google.com/googleplay/android-developer/answer/9859455>
- Target API requirements: <https://developer.android.com/google/play/requirements/target-sdk>
- Personal-account testing: <https://support.google.com/googleplay/android-developer/answer/14151465>
- Developer verification: <https://support.google.com/googleplay/android-developer/answer/10841920>
