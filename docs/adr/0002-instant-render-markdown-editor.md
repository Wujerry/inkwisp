# Use instant-render Markdown editing

InkWisp will use Typora-style instant rendering as its primary editing experience: inactive content appears formatted, the active block exposes necessary Markdown markers, and users can switch to a full source mode. Both modes edit the same standard Markdown document; InkWisp will not use a rich-text model that converts to Markdown only when saving, because round-trip conversion risks changing or losing user-authored syntax and would make inline prediction behavior harder to reason about.
