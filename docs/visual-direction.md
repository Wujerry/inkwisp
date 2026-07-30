# InkWisp Visual Direction

## Visual thesis

InkWisp feels like warm paper carrying crisp digital ink: quiet, editorial, and responsive, with a restrained vermilion signal that makes AI and navigation states legible without turning writing into a dashboard.

## Content plan

The application is not a marketing surface. Its working hierarchy is:

1. **Editor** — the document and cursor are always the dominant visual content.
2. **Navigation** — a temporary phone drawer or persistent wide-screen rail exposes Workspace files, search, outline, and backlinks.
3. **Editing context** — a low-profile keyboard-adjacent toolbar exposes only actions relevant to the current selection or block.
4. **Secondary detail** — settings, model connections, revisions, export, and diagnostics use plain grouped lists and dividers rather than card mosaics.

## Interaction thesis

1. **Spatial navigation** — the Workspace drawer and wide-screen rail share one continuous horizontal transition so users retain document context.
2. **Provisional intelligence** — Inline Prediction fades and slightly rises into place as ghost text; accepting it resolves the ghost into normal ink without moving the cursor unexpectedly.
3. **Editing modes** — Instant-render and Source Mode transition through local syntax-marker reveals rather than a full-screen crossfade, preserving scroll and selection position.

All primary motion lasts roughly 160–240 ms, responds to interruption, and becomes an immediate state change when reduced motion is enabled.

## Visual tokens

- Paper: `#F5F0E6`
- Paper elevated: `#FBF7EF`
- Ink: `#211F1B`
- Muted ink: `#706D66`
- Vermilion: `#B84C38`
- Hairline: ink at 12% opacity
- Dark paper: `#171614`
- Dark elevated: `#201E1B`
- Dark ink: `#F1ECE2`

Typography uses bundled Manrope for the English interface, Literata for long-form Latin Markdown, and LXGW WenKai Lite for Chinese. Mixed Markdown prose falls back per glyph so Latin and Han scripts keep their intended voices. Interface hierarchy relies on type scale, optical spacing, dividers, and one vermilion signal rather than shadows or decorative containers.
