export function normalizedHeadingInsertion(lineBeforeCursor: string, typed: string): string | null {
  if (!typed || /^\s/.test(typed) || typed.startsWith("#")) return null;
  if (!/^#{1,6}$/.test(lineBeforeCursor)) return null;
  return ` ${typed}`;
}
