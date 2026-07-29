import "./style.css";
import { defaultKeymap, history, historyKeymap, indentWithTab } from "@codemirror/commands";
import { markdown } from "@codemirror/lang-markdown";
import { syntaxHighlighting, defaultHighlightStyle, syntaxTree } from "@codemirror/language";
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
  runCommand(command: FormatCommand): void;
  setPrediction(text: string): void;
  acceptPrediction(amount?: "all" | "next"): void;
  requestAssistedEdit(action: string): void;
  insertText(text: string): void;
  focus(): void;
}

const externalDocument = StateEffect.define<boolean>();
const setPredictionEffect = StateEffect.define<string>();
const clearPredictionEffect = StateEffect.define<void>();
const modeCompartment = new Compartment();

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
  "HeaderMark",
  "EmphasisMark",
  "CodeMark",
  "LinkMark",
  "URL",
]);

function buildInstantDecorations(view: EditorView): DecorationSet {
  const ranges: Array<ReturnType<typeof Decoration.mark>["range"] extends never ? never : any> = [];
  const cursor = view.state.selection.main.head;
  const cursorLine = view.state.doc.lineAt(cursor).number;

  for (const visible of view.visibleRanges) {
    syntaxTree(view.state).iterate({
      from: visible.from,
      to: visible.to,
      enter(node) {
        const line = view.state.doc.lineAt(node.from);
        const activeLine = line.number === cursorLine;
        if (/^ATXHeading[1-6]$/.test(node.name)) {
          const level = Number(node.name.at(-1));
          ranges.push(Decoration.line({ class: `iw-heading iw-h${level}` }).range(line.from));
        }
        if (!activeLine && hiddenMarkerNames.has(node.name) && node.from < node.to) {
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
        }
      },
    });
  }
  return Decoration.set(ranges, true);
}

const instantRender = ViewPlugin.fromClass(
  class {
    decorations: DecorationSet;
    constructor(view: EditorView) {
      this.decorations = buildInstantDecorations(view);
    }
    update(update: ViewUpdate) {
      if (update.docChanged || update.selectionSet || update.viewportChanged) {
        this.decorations = buildInstantDecorations(update.view);
      }
    }
  },
  { decorations: (plugin) => plugin.decorations },
);

let nativeRevision = 0;
let pointerStart: { x: number; y: number } | null = null;

const state = EditorState.create({
  doc: "",
  extensions: [
    history(),
    markdown({ extensions: [GFM] }),
    syntaxHighlighting(defaultHighlightStyle, { fallback: true }),
    keymap.of([
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
    placeholder("Begin writing…"),
    EditorView.lineWrapping,
    EditorView.contentAttributes.of({
      spellcheck: "true",
      autocapitalize: "sentences",
      "aria-label": "Markdown editor",
    }),
    EditorView.updateListener.of((update) => {
      if (!update.docChanged) return;
      if (update.transactions.some((transaction) => transaction.effects.some((effect) => effect.is(externalDocument)))) {
        return;
      }
      nativeRevision += 1;
      window.InkWispNative?.changed(
        update.state.doc.toString(),
        nativeRevision,
        update.state.selection.main.head,
      );
    }),
    EditorView.domEventHandlers({
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
  runCommand: runFormatCommand,
  setPrediction(text) {
    view.dispatch({ effects: setPredictionEffect.of(text) });
  },
  acceptPrediction(amount = "all") {
    acceptPrediction(view, amount);
  },
  requestAssistedEdit(action) {
    const selection = view.state.selection.main;
    if (selection.empty) {
      window.InkWispNative?.command(JSON.stringify({ type: "error", message: "Select text first." }));
      return;
    }
    window.InkWispNative?.command(JSON.stringify({
      type: "assistedEdit",
      action,
      from: selection.from,
      to: selection.to,
      text: view.state.sliceDoc(selection.from, selection.to),
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
  },
};

window.InkWispNative?.ready();
