export function normalizedHeadingInsertion(lineBeforeCursor: string, typed: string): string | null {
  if (!typed || /^\s/.test(typed) || typed.startsWith("#")) return null;
  if (!/^#{1,6}$/.test(lineBeforeCursor)) return null;
  return ` ${typed}`;
}

const inlineClosingDelimiters = ["**", "~~", "`", "*", "_"];

export function closingInlineDelimiter(document: string, cursor: number): string | null {
  const lineStart = document.lastIndexOf("\n", Math.max(0, cursor - 1)) + 1;
  const before = document.slice(lineStart, cursor);
  return inlineClosingDelimiters.find((delimiter) =>
    document.startsWith(delimiter, cursor) && before.lastIndexOf(delimiter) >= 0,
  ) ?? null;
}

export interface SmartEnterEdit {
  from: number;
  to: number;
  insert: string;
  cursor: number;
}

export function smartBlockEnter(document: string, cursor: number): SmartEnterEdit | null {
  const lineStart = document.lastIndexOf("\n", Math.max(0, cursor - 1)) + 1;
  const lineEnd = document.indexOf("\n", cursor);
  const resolvedEnd = lineEnd === -1 ? document.length : lineEnd;
  if (cursor !== resolvedEnd) return null;
  const line = document.slice(lineStart, resolvedEnd);
  const task = /^(\s*[-+*]\s+)\[[ xX]\](\s+)(.*)$/.exec(line);
  if (task) {
    const prefix = `${task[1]}[ ]${task[2]}`;
    return task[3]
      ? { from: cursor, to: cursor, insert: `\n${prefix}`, cursor: cursor + prefix.length + 1 }
      : { from: lineStart, to: cursor, insert: "", cursor: lineStart };
  }
  const bullet = /^(\s*[-+*]\s+)(.*)$/.exec(line);
  if (bullet) {
    return bullet[2]
      ? { from: cursor, to: cursor, insert: `\n${bullet[1]}`, cursor: cursor + bullet[1].length + 1 }
      : { from: lineStart, to: cursor, insert: "", cursor: lineStart };
  }
  const quote = /^(\s*>\s?)(.*)$/.exec(line);
  if (quote) {
    return quote[2]
      ? { from: cursor, to: cursor, insert: `\n${quote[1]}`, cursor: cursor + quote[1].length + 1 }
      : { from: lineStart, to: cursor, insert: "", cursor: lineStart };
  }
  return null;
}
