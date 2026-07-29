export type FormatCommand = "heading" | "bold" | "italic" | "bullet" | "task" | "code";

export interface TextEdit {
  from: number;
  to: number;
  insert: string;
  selectionAnchor: number;
  selectionHead: number;
}

export function formatSelection(
  document: string,
  from: number,
  to: number,
  command: FormatCommand,
): TextEdit {
  const selected = document.slice(from, to);
  switch (command) {
    case "bold":
      return wrap(from, to, selected, "**", "**");
    case "italic":
      return wrap(from, to, selected, "*", "*");
    case "code":
      return selected.includes("\n")
        ? wrap(from, to, selected, "```\n", "\n```")
        : wrap(from, to, selected, "`", "`");
    case "heading":
      return prefixLines(document, from, to, "# ", /^#{1,6}\s+/);
    case "bullet":
      return prefixLines(document, from, to, "- ", /^[-*+]\s+/);
    case "task":
      return prefixLines(document, from, to, "- [ ] ", /^[-*+]\s+(?:\[[ xX]\]\s+)?/);
  }
}

function wrap(from: number, to: number, selected: string, before: string, after: string): TextEdit {
  const placeholder = selected || "text";
  const insert = `${before}${placeholder}${after}`;
  return {
    from,
    to,
    insert,
    selectionAnchor: from + before.length,
    selectionHead: from + before.length + placeholder.length,
  };
}

function prefixLines(
  document: string,
  from: number,
  to: number,
  prefix: string,
  existingPrefix: RegExp,
): TextEdit {
  const lineStart = document.lastIndexOf("\n", Math.max(0, from - 1)) + 1;
  const nextBreak = document.indexOf("\n", to);
  const lineEnd = nextBreak === -1 ? document.length : nextBreak;
  const block = document.slice(lineStart, lineEnd);
  const lines = block.split("\n");
  const insert = lines
    .map((line) => `${prefix}${line.replace(existingPrefix, "")}`)
    .join("\n");
  const added = insert.length - block.length;
  return {
    from: lineStart,
    to: lineEnd,
    insert,
    selectionAnchor: Math.max(lineStart, from + prefix.length),
    selectionHead: Math.max(lineStart, to + added),
  };
}

