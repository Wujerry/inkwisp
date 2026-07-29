import { describe, expect, it } from "vitest";
import { formatSelection } from "./format";

describe("formatSelection", () => {
  it("wraps a selected phrase without changing its text", () => {
    expect(formatSelection("keep this safe", 5, 9, "bold")).toEqual({
      from: 5,
      to: 9,
      insert: "**this**",
      selectionAnchor: 7,
      selectionHead: 11,
    });
  });

  it("replaces an existing heading marker instead of stacking markers", () => {
    expect(formatSelection("### Heading", 4, 11, "heading").insert).toBe("# Heading");
  });

  it("formats every selected line as a task", () => {
    expect(formatSelection("first\nsecond", 0, 12, "task").insert).toBe("- [ ] first\n- [ ] second");
  });

  it("uses fenced code for multiline content", () => {
    expect(formatSelection("a\nb", 0, 3, "code").insert).toBe("```\na\nb\n```");
  });
});

