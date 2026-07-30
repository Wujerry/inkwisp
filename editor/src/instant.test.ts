import { describe, expect, it } from "vitest";
import { normalizedHeadingInsertion } from "./instant";

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
