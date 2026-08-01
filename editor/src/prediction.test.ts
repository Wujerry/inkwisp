import { readFileSync } from "node:fs";
import { describe, expect, it } from "vitest";
import { classifyPredictionDragIntent, classifyPredictionSwipe } from "./prediction";

describe("inline prediction interaction", () => {
  it("accepts a deliberate right swipe", () => {
    expect(classifyPredictionSwipe({ x: 40, y: 80 }, { x: 132, y: 91 })).toBe("accept");
  });

  it("does not accept short, leftward, or mostly vertical gestures", () => {
    expect(classifyPredictionSwipe({ x: 40, y: 80 }, { x: 90, y: 82 })).toBe("ignore");
    expect(classifyPredictionSwipe({ x: 100, y: 80 }, { x: 10, y: 82 })).toBe("ignore");
    expect(classifyPredictionSwipe({ x: 40, y: 80 }, { x: 140, y: 145 })).toBe("ignore");
  });

  it("distinguishes a horizontal acceptance gesture from vertical scrolling", () => {
    expect(classifyPredictionDragIntent({ x: 40, y: 80 }, { x: 45, y: 84 })).toBe("pending");
    expect(classifyPredictionDragIntent({ x: 40, y: 80 }, { x: 70, y: 86 })).toBe("horizontal");
    expect(classifyPredictionDragIntent({ x: 40, y: 80 }, { x: 45, y: 112 })).toBe("vertical");
    expect(classifyPredictionDragIntent({ x: 0, y: 0 }, { x: 9, y: 7 })).toBe("pending");
    expect(classifyPredictionDragIntent({ x: 0, y: 0 }, { x: 10, y: 120 })).toBe("vertical");
  });

  it("keeps provisional text visually distinct from normal line spans", () => {
    const css = readFileSync(new URL("./style.css", import.meta.url), "utf8");
    expect(css).toMatch(/\.cm-line\s+\.iw-prediction\s*\{/);
    expect(css).toMatch(/\.iw-prediction[\s\S]*?touch-action:\s*pan-y/);
    expect(css).toMatch(/color:\s*var\(--prediction\)\s*!important/);
    expect(css).not.toMatch(/@keyframes prediction-in\s*\{\s*from\s*\{[^}]*opacity/);
  });

  it("keeps a touch-event fallback for Android WebViews that cancel pointer gestures", () => {
    const source = readFileSync(new URL("./main.ts", import.meta.url), "utf8");
    expect(source).toContain('addEventListener("touchstart"');
    expect(source).toContain('addEventListener("touchend"');
    expect(source).toContain("{ passive: false }");
    expect(source).toContain('event.pointerType === "touch"');
  });
});
