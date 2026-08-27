/*
 * Long-lived browser consumer. The host supplies the generated Wasm module as
 * window.mermaidNative; this shell never parses Mermaid or trusts HTML input.
 */
const source = document.querySelector('#source');
const render = document.querySelector('#render');
const result = document.querySelector('#result');
const status = document.querySelector('#status');

function showFailure(payload) {
  result.replaceChildren();
  const error = document.createElement('p');
  error.className = 'error';
  error.textContent = payload.diagnostics.map(d => `${d.code}: ${d.message} (${d.line}:${d.column})`).join('\n');
  result.append(error);
  status.textContent = 'Render failed';
}

function showSuccess(payload) {
  // Keep parsing detached, then enforce an SVG-only, script-free boundary.
  const parsed = new DOMParser().parseFromString(payload.svg, 'image/svg+xml');
  const svg = parsed.documentElement;
  if (svg.localName !== 'svg' || parsed.querySelector('parsererror, script, iframe, foreignObject')) {
    status.textContent = 'Render rejected by SVG safety gate';
    result.replaceChildren();
    return;
  }
  parsed.querySelectorAll('*').forEach(node => {
    [...node.attributes].forEach(attribute => {
      if (attribute.name.toLowerCase().startsWith('on')) node.removeAttribute(attribute.name);
    });
  });
  result.replaceChildren(document.importNode(svg, true));
  status.textContent = 'Rendered successfully';
}

render.addEventListener('click', async () => {
  const runtime = window.mermaidNative ?? await window['mermaid-web'];
  if (runtime) window.mermaidNative = runtime;
  if (!window.mermaidNative?.renderMermaidResultJson) {
    status.textContent = 'Wasm module unavailable';
    return;
  }
  const payload = JSON.parse(window.mermaidNative.renderMermaidResultJson(source.value));
  payload.ok ? showSuccess(payload) : showFailure(payload);
});
