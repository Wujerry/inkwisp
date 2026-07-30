import { describe, expect, it } from "vitest";
import { closingInlineDelimiter, normalizedHeadingInsertion, smartBlockEnter } from "./instant";

describe("normalizedHeadingInsertion", () => {
  it("turns hash followed by text into a Markdown heading", () => {
    expect(normalizedHeadingInsertion("#", "Title")).toBe(" Title");
    expect(normalizedHeadingInsertion("###", "标题")).toBe(" 标题");
  });

  it("does not interfere with heading level entry or existing spacing", () => {
    expect(normalizedHeadingInsertion("#", "#")).toBeNull();
    expect(normalizedHeadingInsertion("#", " ")).toBeNull();
    expect(normalizedHeadingInsertion("text #", "x")).toBeNull();
  });
});

describe("closingInlineDelimiter", () => {
  it("finds the closing boundary left by formatting commands", () => {
    expect(closingInlineDelimiter("**draft**", 7)).toBe("**");
    expect(closingInlineDelimiter("`snippet`", 8)).toBe("`");
    expect(closingInlineDelimiter("~~removed~~", 9)).toBe("~~");
  });

  it("does not treat ordinary punctuation as a formatting boundary", () => {
    expect(closingInlineDelimiter("plain *", 6)).toBeNull();
    expect(closingInlineDelimiter("after\n`", 6)).toBeNull();
  });
});

describe("smartBlockEnter", () => {
  it("continues bullets, tasks, and quotes", () => {
    expect(smartBlockEnter("- item", 6)?.insert).toBe("\n- ");
    expect(smartBlockEnter("- [x] done", 10)?.insert).toBe("\n- [ ] ");
    expect(smartBlockEnter("> thought", 9)?.insert).toBe("\n> ");
  });

  it("exits an empty structural line", () => {
    expect(smartBlockEnter("- ", 2)).toEqual({ from: 0, to: 2, insert: "", cursor: 0 });
    expect(smartBlockEnter("- [ ] ", 6)).toEqual({ from: 0, to: 6, insert: "", cursor: 0 });
    expect(smartBlockEnter("> ", 2)).toEqual({ from: 0, to: 2, insert: "", cursor: 0 });
  });

  it("leaves ordinary paragraphs and mid-line cursors to CodeMirror", () => {
    expect(smartBlockEnter("paragraph", 9)).toBeNull();
    expect(smartBlockEnter("- item", 3)).toBeNull();
  });
});
