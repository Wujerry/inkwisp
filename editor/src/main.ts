import "./style.css";
import { defaultKeymap, history, historyKeymap, indentWithTab } from "@codemirror/commands";
import { markdown } from "@codemirror/lang-markdown";
import { ensureSyntaxTree, forceParsing, syntaxHighlighting, defaultHighlightStyle, syntaxTree } from "@codemirror/language";
import { Compartment, EditorSelection, EditorState, StateEffect, StateField } from "@codemirror/state";
import {
  Decoration,
  DecorationSet,
  EditorView,
  ViewPlugin,
  ViewUpdate,
  WidgetType,
  keymap,
  placeholder,
} from "@codemirror/view";
import { GFM } from "@lezer/markdown";
import { formatSelection, type FormatCommand } from "./format";
import { closingInlineDelimiter, normalizedHeadingInsertion, smartBlockEnter } from "./instant";

declare global {
  interface Window {
    InkWispNative?: {
      ready(): void;
      changed(content: string, revision: number, cursor: number): void;
      command(command: string): void;
    };
    InkWispEditor?: InkWispEditorApi;
  }
}

interface InkWispEditorApi {
  setDocument(content: string, revision: number): void;
  setMode(mode: "instant" | "source"): void;
  setAppearance(theme: "light" | "dark", reducedMotion: boolean): void;
  setLocale(locale: "en" | "zh-CN"): void;
  runCommand(command: FormatCommand): void;
  setPrediction(text: string): void;
  acceptPrediction(amount?: "all" | "next"): void;
  requestAssistedEdit(action: string): void;
  insertText(text: string): void;
  focus(): void;
  blur(): void;
  getDocument(): string;
}

const externalDocument = StateEffect.define<boolean>();
const setPredictionEffect = StateEffect.define<string>();
const clearPredictionEffect = StateEffect.define<void>();
const refreshInstantRenderEffect = StateEffect.define<void>();
const modeCompartment = new Compartment();
const localeCompartment = new Compartment();

class PredictionWidget extends WidgetType {
  constructor(readonly text: string) {
    super();
  }

  eq(other: PredictionWidget): boolean {
    return other.text === this.text;
  }

  toDOM(): HTMLElement {
    const span = document.createElement("span");
    span.className = "iw-prediction";
    span.textContent = this.text;
    span.setAttribute("aria-label", `Prediction: ${this.text}`);
    return span;
  }

  ignoreEvent(): boolean {
    return false;
  }
}

class MarkdownMarkerWidget extends WidgetType {
  constructor(readonly glyph: string, readonly className: string) {
    super();
  }

  eq(other: MarkdownMarkerWidget): boolean {
    return other.glyph === this.glyph && other.className === this.className;
  }

  toDOM(): HTMLElement {
    const span = document.createElement("span");
    span.className = this.className;
    span.textContent = this.glyph;
    span.setAttribute("aria-hidden", "true");
    return span;
  }

  ignoreEvent(): boolean {
    return true;
  }
}

const predictionField = StateField.define<{ at: number; text: string } | null>({
  create: () => null,
  update(value, transaction) {
    for (const effect of transaction.effects) {
      if (effect.is(setPredictionEffect)) {
        return effect.value ? { at: transaction.state.selection.main.head, text: effect.value } : null;
      }
      if (effect.is(clearPredictionEffect)) return null;
    }
    if (transaction.docChanged || transaction.selection) return null;
    return value;
  },
  provide: (field) =>
    EditorView.decorations.from(field, (prediction) => {
      if (!prediction) return Decoration.none;
      return Decoration.set([
        Decoration.widget({ widget: new PredictionWidget(prediction.text), side: 1 }).range(prediction.at),
      ]);
    }),
});

const hiddenMarkerNames = new Set([
  "EmphasisMark",
  "CodeMark",
  "LinkMark",
  "URL",
  "StrikethroughMark",
  "CodeInfo",
]);

function buildInstantDecorations(view: EditorView): DecorationSet {
  const ranges: Array<ReturnType<typeof Decoration.mark>["range"] extends never ? never : any> = [];
  const cursor = view.state.selection.main.head;
  const cursorLine = view.state.doc.lineAt(cursor).number;

  for (let lineNumber = 1; lineNumber <= view.state.doc.lines; lineNumber += 1) {
    const line = view.state.doc.line(lineNumber);
    const heading = /^(#{1,6})(\s+)/.exec(line.text);
    if (!heading) continue;
    const level = heading[1].length;
    const contentStart = line.from + heading[0].length;
    const cursorTouchesMarker = lineNumber === cursorLine && cursor <= contentStart;
    ranges.push(Decoration.line({ class: `iw-heading iw-h${level}` }).range(line.from));
    if (!cursorTouchesMarker) {
      ranges.push(Decoration.replace({}).range(line.from, contentStart));
    }
  }

  const tree = ensureSyntaxTree(
    view.state,
    Math.max(view.viewport.to, view.state.selection.main.head),
    10,
  ) ?? syntaxTree(view.state);
  tree.iterate({
    enter(node) {
        const line = view.state.doc.lineAt(node.from);
        const activeLine = line.number === cursorLine;
        const cursorTouchesMarker = activeLine && cursor > node.from && cursor < node.to;
        if (!cursorTouchesMarker && hiddenMarkerNames.has(node.name) && node.from < node.to) {
          ranges.push(Decoration.replace({}).range(node.from, node.to));
        }
        if (node.name === "StrongEmphasis") {
          ranges.push(Decoration.mark({ class: "iw-strong" }).range(node.from, node.to));
        } else if (node.name === "Emphasis") {
          ranges.push(Decoration.mark({ class: "iw-emphasis" }).range(node.from, node.to));
        } else if (node.name === "InlineCode") {
          ranges.push(Decoration.mark({ class: "iw-inline-code" }).range(node.from, node.to));
        } else if (node.name === "Blockquote") {
          ranges.push(Decoration.mark({ class: "iw-quote" }).range(node.from, node.to));
        } else if (node.name === "Link") {
          ranges.push(Decoration.mark({ class: "iw-link" }).range(node.from, node.to));
        } else if (node.name === "Strikethrough") {
          ranges.push(Decoration.mark({ class: "iw-strike" }).range(node.from, node.to));
        } else if (node.name === "FencedCode" || node.name === "CodeBlock") {
          ranges.push(Decoration.mark({ class: "iw-code-block" }).range(node.from, node.to));
        } else if (node.name === "ListMark" && !cursorTouchesMarker) {
          const isTask = /^\s*[-+*]\s+\[[ xX]\]/.test(line.text);
          ranges.push(
            (isTask
              ? Decoration.replace({})
              : Decoration.replace({ widget: new MarkdownMarkerWidget("•", "iw-list-marker") }))
              .range(node.from, node.to),
          );
        } else if (node.name === "TaskMarker" && !cursorTouchesMarker) {
          const checked = /[xX]/.test(view.state.sliceDoc(node.from, node.to));
          ranges.push(
            Decoration.replace({
              widget: new MarkdownMarkerWidget(checked ? "☑" : "☐", `iw-task-marker${checked ? " is-checked" : ""}`),
            }).range(node.from, node.to),
          );
        } else if (node.name === "QuoteMark" && !cursorTouchesMarker) {
          ranges.push(Decoration.replace({}).range(node.from, Math.min(node.to + 1, line.to)));
          ranges.push(Decoration.line({ class: "iw-quote-line" }).range(line.from));
        } else if (node.name === "HorizontalRule") {
          ranges.push(
            Decoration.replace({ widget: new MarkdownMarkerWidget("", "iw-horizontal-rule") })
              .range(node.from, node.to),
          );
        }
    },
  });
  return Decoration.set(ranges, true);
}

const instantRender = ViewPlugin.fromClass(
  class {
    decorations: DecorationSet;
    parseTimer: number | null = null;
    constructor(view: EditorView) {
      this.decorations = buildInstantDecorations(view);
      this.scheduleParsedRefresh(view);
    }
    update(update: ViewUpdate) {
      if (update.docChanged) this.scheduleParsedRefresh(update.view);
      if (update.docChanged || update.selectionSet || update.viewportChanged || update.transactions.length > 0) {
        this.decorations = buildInstantDecorations(update.view);
      }
    }
    scheduleParsedRefresh(view: EditorView) {
      if (this.parseTimer !== null) window.clearTimeout(this.parseTimer);
      this.parseTimer = window.setTimeout(() => {
        this.parseTimer = null;
        forceParsing(view, Math.max(view.viewport.to, view.state.selection.main.head), 50);
        view.dispatch({ effects: refreshInstantRenderEffect.of() });
      }, 0);
    }
    destroy() {
      if (this.parseTimer !== null) window.clearTimeout(this.parseTimer);
    }
  },
  { decorations: (plugin) => plugin.decorations },
);

let nativeRevision = 0;
let pointerStart: { x: number; y: number } | null = null;

function scheduleKeyboardSafeScroll(target: EditorView): void {
  window.setTimeout(() => {
    if (!target.hasFocus) return;
    const compactViewport = (window.visualViewport?.height ?? window.innerHeight) < 560;
    target.dispatch({
      effects: EditorView.scrollIntoView(target.state.selection.main.head, {
        y: compactViewport ? "center" : "nearest",
        yMargin: compactViewport ? 96 : 48,
      }),
    });
  }, 0);
}

const state = EditorState.create({
  doc: "",
  extensions: [
    history(),
    markdown({ extensions: [GFM] }),
    syntaxHighlighting(defaultHighlightStyle, { fallback: true }),
    keymap.of([
      {
        key: "Enter",
        run: (view) => insertOutsideClosingDelimiter(view, "\n") || applySmartBlockEnter(view),
      },
      {
        key: "ArrowRight",
        run: (view) => moveOutsideClosingDelimiter(view),
      },
      ...defaultKeymap,
      ...historyKeymap,
      indentWithTab,
      {
        key: "Tab",
        run: (view) => acceptPrediction(view, "all"),
      },
      {
        key: "Escape",
        run: (view) => clearPrediction(view),
      },
    ]),
    predictionField,
    modeCompartment.of([instantRender, EditorView.editorAttributes.of({ class: "iw-instant" })]),
    localeCompartment.of([
      placeholder("Begin writing…"),
      EditorView.contentAttributes.of({
        spellcheck: "true",
        autocapitalize: "sentences",
        "aria-label": "Markdown editor",
      }),
    ]),
    EditorView.lineWrapping,
    EditorView.updateListener.of((update) => {
      if (update.docChanged || update.selectionSet) scheduleKeyboardSafeScroll(update.view);
      if (update.docChanged && !update.transactions.some(
        (transaction) => transaction.effects.some((effect) => effect.is(externalDocument)),
      )) {
        nativeRevision += 1;
        window.InkWispNative?.changed(
          update.state.doc.toString(),
          nativeRevision,
          update.state.selection.main.head,
        );
      }
    }),
    EditorView.inputHandler.of((target, from, to, text) => {
      if (from !== to) return false;
      const delimiter = closingInlineDelimiter(target.state.doc.toString(), from);
      if (delimiter && (text === "\n" || /^\s/.test(text) || text === delimiter)) {
        const insert = text === delimiter ? "" : text;
        const at = from + delimiter.length;
        target.dispatch({
          changes: insert ? { from: at, insert } : undefined,
          selection: EditorSelection.cursor(at + insert.length),
          scrollIntoView: true,
          userEvent: "input.type",
        });
        return true;
      }
      if (text === "\n") {
        const edit = smartBlockEnter(target.state.doc.toString(), from);
        if (edit) {
          target.dispatch({
            changes: { from: edit.from, to: edit.to, insert: edit.insert },
            selection: EditorSelection.cursor(edit.cursor),
            scrollIntoView: true,
            userEvent: "input.type",
          });
          return true;
        }
      }
      const line = target.state.doc.lineAt(from);
      const insertion = normalizedHeadingInsertion(
        target.state.doc.sliceString(line.from, from),
        text,
      );
      if (insertion === null) return false;
      target.dispatch({
        changes: { from, insert: insertion },
        selection: EditorSelection.cursor(from + insertion.length),
        scrollIntoView: true,
        userEvent: "input.type",
      });
      return true;
    }),
    EditorView.domEventHandlers({
      beforeinput(event, view) {
        const text = event.data ?? "";
        const lineBreak = event.inputType === "insertParagraph" ||
          event.inputType === "insertLineBreak" || text === "\n";
        const handled = lineBreak
          ? insertOutsideClosingDelimiter(view, "\n") || applySmartBlockEnter(view)
          : /^\s/.test(text) && insertOutsideClosingDelimiter(view, text);
        if (!handled) return false;
        event.preventDefault();
        return true;
      },
      pointerdown(event) {
        pointerStart = { x: event.clientX, y: event.clientY };
        return false;
      },
      pointerup(event, view) {
        if (!pointerStart) return false;
        const deltaX = event.clientX - pointerStart.x;
        const deltaY = Math.abs(event.clientY - pointerStart.y);
        pointerStart = null;
        return deltaX > 72 && deltaY < 48 ? acceptPrediction(view, "all") : false;
      },
    }),
    EditorView.theme({
      "&": { height: "100%" },
      ".cm-scroller": { overflow: "auto" },
    }),
  ],
});

const root = document.querySelector<HTMLElement>("#editor");
if (!root) throw new Error("Editor root not found");
const view = new EditorView({ state, parent: root });

function keepCursorAboveKeyboard(): void {
  if (!view.hasFocus) return;
  window.requestAnimationFrame(() => {
    view.dispatch({
      effects: EditorView.scrollIntoView(view.state.selection.main.head, {
        y: "center",
        yMargin: 72,
      }),
    });
  });
}

window.visualViewport?.addEventListener("resize", keepCursorAboveKeyboard);

function clearPrediction(target: EditorView): boolean {
  if (!target.state.field(predictionField, false)) return false;
  target.dispatch({ effects: clearPredictionEffect.of() });
  return true;
}

function acceptPrediction(target: EditorView, amount: "all" | "next"): boolean {
  const prediction = target.state.field(predictionField, false);
  if (!prediction) return false;
  const accepted = amount === "next" ? nextPredictionUnit(prediction.text) : prediction.text;
  const remainder = prediction.text.slice(accepted.length);
  target.dispatch({
    changes: { from: prediction.at, insert: accepted },
    selection: EditorSelection.cursor(prediction.at + accepted.length),
    effects: remainder ? setPredictionEffect.of(remainder) : clearPredictionEffect.of(),
    scrollIntoView: true,
  });
  return true;
}

function nextPredictionUnit(text: string): string {
  const match = text.match(/^(?:\s+|[\p{Script=Han}]+|[\p{L}\p{N}_'-]+[\s.,;:!?，。；：！？]*)/u);
  if (!match) return text.charAt(0);
  if (/^[\p{Script=Han}]+$/u.test(match[0])) return [...match[0]].slice(0, 2).join("");
  return match[0];
}

function moveOutsideClosingDelimiter(target: EditorView): boolean {
  const selection = target.state.selection.main;
  if (!selection.empty) return false;
  const delimiter = closingInlineDelimiter(target.state.doc.toString(), selection.head);
  if (!delimiter) return false;
  target.dispatch({ selection: EditorSelection.cursor(selection.head + delimiter.length) });
  return true;
}

function insertOutsideClosingDelimiter(target: EditorView, text: string): boolean {
  const selection = target.state.selection.main;
  if (!selection.empty) return false;
  const delimiter = closingInlineDelimiter(target.state.doc.toString(), selection.head);
  if (!delimiter) return false;
  const at = selection.head + delimiter.length;
  target.dispatch({
    changes: { from: at, insert: text },
    selection: EditorSelection.cursor(at + text.length),
    scrollIntoView: true,
    userEvent: "input.type",
  });
  return true;
}

function applySmartBlockEnter(target: EditorView): boolean {
  const selection = target.state.selection.main;
  if (!selection.empty) return false;
  const edit = smartBlockEnter(target.state.doc.toString(), selection.head);
  if (!edit) return false;
  target.dispatch({
    changes: { from: edit.from, to: edit.to, insert: edit.insert },
    selection: EditorSelection.cursor(edit.cursor),
    scrollIntoView: true,
    userEvent: "input.type",
  });
  return true;
}

function runFormatCommand(command: FormatCommand): void {
  const selection = view.state.selection.main;
  const edit = formatSelection(view.state.doc.toString(), selection.from, selection.to, command);
  view.dispatch({
    changes: { from: edit.from, to: edit.to, insert: edit.insert },
    selection: EditorSelection.range(edit.selectionAnchor, edit.selectionHead),
    scrollIntoView: true,
  });
  view.focus();
}

window.InkWispEditor = {
  setDocument(content, revision) {
    if (view.state.doc.toString() === content) {
      nativeRevision = Math.max(nativeRevision, revision);
      return;
    }
    nativeRevision = revision;
    view.dispatch({
      changes: { from: 0, to: view.state.doc.length, insert: content },
      selection: EditorSelection.cursor(Math.min(view.state.selection.main.head, content.length)),
      effects: [externalDocument.of(true), clearPredictionEffect.of()],
    });
  },
  setMode(mode) {
    view.dispatch({
      effects: modeCompartment.reconfigure(
        mode === "instant"
          ? [instantRender, EditorView.editorAttributes.of({ class: "iw-instant" })]
          : [EditorView.editorAttributes.of({ class: "iw-source" })],
      ),
    });
  },
  setAppearance(theme, reducedMotion) {
    document.documentElement.dataset.theme = theme;
    document.documentElement.dataset.reducedMotion = String(reducedMotion);
  },
  setLocale(locale) {
    const chinese = locale === "zh-CN";
    document.documentElement.lang = locale;
    view.dispatch({
      effects: localeCompartment.reconfigure([
        placeholder(chinese ? "开始写作…" : "Begin writing…"),
        EditorView.contentAttributes.of({
          spellcheck: "true",
          autocapitalize: "sentences",
          "aria-label": chinese ? "Markdown 编辑器" : "Markdown editor",
        }),
      ]),
    });
  },
  runCommand: runFormatCommand,
  setPrediction(text) {
    view.dispatch({ effects: setPredictionEffect.of(text) });
  },
  acceptPrediction(amount = "all") {
    acceptPrediction(view, amount);
  },
  requestAssistedEdit(action) {
    const selection = view.state.selection.main;
    if (view.state.doc.length === 0) {
      window.InkWispNative?.command(JSON.stringify({ type: "error", code: "emptyDocument" }));
      return;
    }
    const from = selection.empty ? 0 : selection.from;
    const to = selection.empty ? view.state.doc.length : selection.to;
    window.InkWispNative?.command(JSON.stringify({
      type: "assistedEdit",
      action,
      from,
      to,
      text: view.state.sliceDoc(from, to),
    }));
  },
  insertText(text) {
    const selection = view.state.selection.main;
    view.dispatch({
      changes: { from: selection.from, to: selection.to, insert: text },
      selection: EditorSelection.cursor(selection.from + text.length),
      scrollIntoView: true,
    });
    view.focus();
  },
  focus() {
    view.focus();
    keepCursorAboveKeyboard();
  },
  blur() {
    view.contentDOM.blur();
    (document.activeElement as HTMLElement | null)?.blur?.();
    window.getSelection()?.removeAllRanges();
  },
  getDocument() {
    return view.state.doc.toString();
  },
};

window.InkWispNative?.ready();
