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

const evaluate = async (expression) => {
  const result = await send("Runtime.evaluate", { expression, returnByValue: true });
  return result.result.value;
};

const key = async (value, code, virtualKeyCode) => {
  await send("Input.dispatchKeyEvent", {
    type: "keyDown", key: value, code, windowsVirtualKeyCode: virtualKeyCode,
    nativeVirtualKeyCode: virtualKeyCode,
  });
  await send("Input.dispatchKeyEvent", {
    type: "keyUp", key: value, code, windowsVirtualKeyCode: virtualKeyCode,
    nativeVirtualKeyCode: virtualKeyCode,
  });
};

const snapshot = () => evaluate(`JSON.stringify({
  text: document.querySelector('.cm-content')?.innerText ?? '',
  html: document.querySelector('.cm-content')?.innerHTML ?? ''
})`).then(JSON.parse);

ws.onopen = async () => {
  try {
    await evaluate(`window.InkWispEditor.setDocument('', 2100); window.InkWispEditor.setMode('instant'); window.InkWispEditor.runCommand('bold'); window.InkWispEditor.insertText('draft'); true`);
    await send("Input.insertText", { text: "\n" });
    await new Promise((resolve) => setTimeout(resolve, 100));
    const bold = await snapshot();
    const boldSource = await evaluate(`window.InkWispEditor.getDocument()`);
    if (bold.text.includes("**") || boldSource !== "**draft**\n") {
      throw new Error(`Bold Enter crossed the closing marker: ${JSON.stringify({ text: bold.text, source: boldSource })}`);
    }

    await evaluate(`window.InkWispEditor.setDocument('', 2200); window.InkWispEditor.runCommand('code'); window.InkWispEditor.insertText('snippet'); true`);
    await send("Input.insertText", { text: " " });
    await send("Input.insertText", { text: "outside" });
    await new Promise((resolve) => setTimeout(resolve, 100));
    const code = await snapshot();
    const probe = await evaluate(`document.querySelector('.iw-inline-code')?.textContent ?? ''`);
    const codeSource = await evaluate(`window.InkWispEditor.getDocument()`);
    if (probe.includes("outside") || codeSource !== "`snippet` outside") {
      throw new Error(`Inline code did not exit at the closing marker: ${JSON.stringify({ probe, text: code.text, source: codeSource })}`);
    }

    await evaluate(`window.__iwEvents = []; document.querySelector('.cm-content')?.addEventListener('beforeinput', (event) => window.__iwEvents.push({type:event.inputType, data:event.data}), true); window.InkWispEditor.setDocument('- item', 2300); window.InkWispEditor.focus(); true`);
    await key("End", "End", 35);
    await send("Input.insertText", { text: "\n" });
    await send("Input.insertText", { text: "next" });
    const bulletSource = await evaluate(`window.InkWispEditor.getDocument()`);
    if (bulletSource !== "- item\n- next") {
      const events = await evaluate(`JSON.stringify(window.__iwEvents)`);
      throw new Error(`Bullet continuation failed: ${JSON.stringify({ source: bulletSource, events: JSON.parse(events) })}`);
    }

    process.stdout.write("Markdown editing boundaries: PASS\n");
    ws.close();
  } catch (error) {
    process.stderr.write(`${error}\n`);
    process.exitCode = 1;
    ws.close();
  }
};
