# Use standard Markdown and preserve unknown syntax

InkWisp will treat CommonMark and GitHub Flavored Markdown as its compatibility baseline, with YAML front matter, footnotes, LaTeX math, Mermaid diagrams, and fenced code blocks as supported extensions. Instant-render and source modes operate on the same Markdown source, and syntax the editor cannot render must remain intact rather than being normalized or discarded. This prioritizes interoperability and lossless round trips over a simpler proprietary rich-text document model.
