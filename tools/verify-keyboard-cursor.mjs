const wsUrl = process.env.CDP_WS;
const mode = process.argv[2];
if (!wsUrl || !["setup", "measure"].includes(mode)) {
  throw new Error("Usage: CDP_WS=... node verify-keyboard-cursor.mjs setup|measure");
}

const ws = new WebSocket(wsUrl);
let nextId = 0;
const pending = new Map();
const send = (method, params = {}) => new Promise((resolve, reject) => {
  const id = ++nextId;
  pending.set(id, { resolve, reject });
  ws.send(JSON.stringify({ id, method, params }));
});
ws.onmessage = (event) => {
  const message = JSON.parse(event.data);
  const promise = pending.get(message.id);
  if (!promise) return;
  pending.delete(message.id);
  if (message.error) promise.reject(new Error(message.error.message));
  else promise.resolve(message.result);
};
ws.onopen = async () => {
  try {
    if (mode === "setup") {
      const doc = Array.from({ length: 55 }, (_, index) => `Line ${index + 1}: writing stays visible.`).join("\n");
      await send("Runtime.evaluate", {
        expression: `window.InkWispEditor.setDocument(${JSON.stringify(doc)}, 1701); window.InkWispEditor.focus(); true`,
      });
      for (const type of ["keyDown", "keyUp"]) {
        await send("Input.dispatchKeyEvent", {
          type, key: "End", code: "End", windowsVirtualKeyCode: 35,
          nativeVirtualKeyCode: 35, modifiers: 2,
        });
      }
      await new Promise((resolve) => setTimeout(resolve, 500));
      process.stdout.write("Keyboard cursor setup: READY\n");
    } else {
      const evaluated = await send("Runtime.evaluate", {
        expression: `JSON.stringify((() => { const selection = document.getSelection(); const selectionRect = selection?.rangeCount ? selection.getRangeAt(0).getBoundingClientRect().toJSON() : null; const scroller = document.querySelector('.cm-scroller'); const content = document.querySelector('.cm-content'); return { focused: document.querySelector('.cm-editor')?.classList.contains('cm-focused') ?? false, cursors: [...document.querySelectorAll('.cm-cursor')].map(node => node.getBoundingClientRect().toJSON()), selectionRect, innerHeight, visualHeight: visualViewport?.height ?? innerHeight, scrollTop: scroller?.scrollTop, scrollHeight: scroller?.scrollHeight, clientHeight: scroller?.clientHeight, contentBottom: content?.getBoundingClientRect().bottom, contentPaddingBottom: content ? getComputedStyle(content).paddingBottom : null }; })())`,
        returnByValue: true,
      });
      const snapshot = JSON.parse(evaluated.result.value);
      const cursor = snapshot.cursors[0] ?? snapshot.selectionRect;
      if (!snapshot.focused || !cursor) throw new Error(`Editor cursor is unavailable: ${JSON.stringify(snapshot)}`);
      if (Number.parseFloat(snapshot.contentPaddingBottom) < 220) {
        throw new Error(`Editor writing space is missing: ${JSON.stringify(snapshot)}`);
      }
      if (cursor.bottom > snapshot.innerHeight - 12) {
        throw new Error(`Cursor is obscured: ${JSON.stringify(snapshot)}`);
      }
      process.stdout.write(`Keyboard cursor: PASS (bottom=${cursor.bottom.toFixed(1)}, viewport=${snapshot.innerHeight})\n`);
    }
    ws.close();
  } catch (error) {
    process.stderr.write(`${error}\n`);
    process.exitCode = 1;
    ws.close();
  }
};
