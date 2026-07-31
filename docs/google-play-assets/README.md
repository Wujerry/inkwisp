# Google Play image assets

These files are ready for the English (`en-US`) and Simplified Chinese (`zh-CN`) store listings.

| Play Console field | File | Dimensions | Notes |
| --- | --- | ---: | --- |
| App icon | `icon-512.png` | 512 x 512 | 32-bit PNG, under 1 MB |
| Feature graphic | `feature-graphic-1024x500.png` | 1024 x 500 | Generated from the InkWisp ink-wash brand mark; no text or unsupported claims |
| English phone screenshots | `screenshots/phone/en-US/` | 1080 x 1920 | Four 9:16 screenshots |
| Chinese phone screenshots | `screenshots/phone/zh-CN/` | 1080 x 1920 | Four 9:16 screenshots |
| English 7-inch tablet screenshots | `screenshots/tablet-7/en-US/` | 1260 x 2240 | Four 9:16 screenshots; 630dp effective width |
| Chinese 7-inch tablet screenshots | `screenshots/tablet-7/zh-CN/` | 1260 x 2240 | Four 9:16 screenshots; 630dp effective width |

Each language/device directory uses the same upload order:

1. `01-onboarding.png` - first app launch and inline-prediction value proposition.
2. `02-editor.png` - real WYSIWYG Markdown editing UI.
3. `03-ai-assistant.png` - real AI writing assistance sheet.
4. `04-model-settings.png` - real provider/model connection UI with model discovery control.

All screenshots were captured from the current Android build. The emulator navigation bar was excluded without stretching the app UI. They contain no API keys, credentials, debug overlays, pricing claims, or ranking claims.

Upload the `en-US` directories to the default English listing. After adding a Simplified Chinese custom store listing, upload the matching `zh-CN` directories there.
