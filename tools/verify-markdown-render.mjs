const wsUrl = process.env.CDP_WS;
if (!wsUrl) throw new Error("CDP_WS is required");

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
    const markdown = [
      "# Heading", "", "**Bold**", "", "*Italic*", "", "~~Strike~~", "",
      "`Code`", "", "[Link](https://example.test)", "", "- Bullet", "- [ ] Task",
      "", "> Quote", "", "```js", "const x = 1", "```", "", "---",
    ].join("\n");
    await send("Runtime.evaluate", {
      expression: `window.InkWispEditor.setDocument(${JSON.stringify(markdown)}, 1600); window.InkWispEditor.setMode('instant'); window.InkWispEditor.focus(); true`,
    });
    await send("Input.dispatchKeyEvent", {
      type: "keyDown", key: "End", code: "End", windowsVirtualKeyCode: 35,
      nativeVirtualKeyCode: 35, modifiers: 2,
    });
    await send("Input.dispatchKeyEvent", {
      type: "keyUp", key: "End", code: "End", windowsVirtualKeyCode: 35,
      nativeVirtualKeyCode: 35, modifiers: 2,
    });
    await new Promise((resolve) => setTimeout(resolve, 700));
    const evaluated = await send("Runtime.evaluate", {
      expression: `JSON.stringify({text: document.querySelector('.cm-content')?.innerText ?? '', html: document.querySelector('.cm-content')?.innerHTML ?? ''})`,
      returnByValue: true,
    });
    const snapshot = JSON.parse(evaluated.result.value);
    const forbidden = ["**", "~~", "`", "https://example.test", "[Link]", "---"];
    const missing = ["Heading", "Bold", "Italic", "Strike", "Code", "Link", "• Bullet", "☐ Task", "Quote", "const x = 1"]
      .filter((value) => !snapshot.text.includes(value));
    const leaked = forbidden.filter((value) => snapshot.text.includes(value));
    const classes = ["iw-heading", "iw-strong", "iw-emphasis", "iw-strike", "iw-inline-code", "iw-link", "iw-list-marker", "iw-task-marker", "iw-quote-line", "iw-code-block", "iw-horizontal-rule"];
    const missingClasses = classes.filter((value) => !snapshot.html.includes(value));
    if (missing.length || leaked.length || missingClasses.length) {
      throw new Error(JSON.stringify({ missing, leaked, missingClasses, text: snapshot.text }));
    }
    process.stdout.write("Markdown instant render: PASS\n");
    ws.close();
  } catch (error) {
    process.stderr.write(`${error}\n`);
    process.exitCode = 1;
    ws.close();
  }
};
