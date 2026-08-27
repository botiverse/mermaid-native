const examples = [
  { family: 'Flowchart', slug: 'flowchart', source: 'flowchart TD\n  A[Start] --> B{Ready?}\n  B -->|Yes| C[Ship]\n  B -->|No| A', note: 'Nodes, labels and branching.' },
  { family: 'Sequence', slug: 'sequence', source: 'sequenceDiagram\n  Alice->>Bob: Hello Bob\n  Bob-->>Alice: Hi Alice', note: 'A minimal request/response.' },
  { family: 'Class', slug: 'class', source: 'classDiagram\n  Animal <|-- Duck\n  Animal : +String name\n  Duck : +swim()', note: 'Typed relationships and members.' },
  { family: 'State', slug: 'state', source: 'stateDiagram-v2\n  [*] --> Draft\n  Draft --> Published\n  Published --> [*]', note: 'A compact lifecycle.' },
  { family: 'Pie', slug: 'pie', source: 'pie title Browser share\n  "Chrome" : 65\n  "Safari" : 20\n  "Other" : 15', note: 'A bounded data visualization.' },
  { family: 'Unsupported example', slug: 'unsupported', source: 'graph TD\n  A ==> B', note: 'Unsupported syntax returns a typed diagnostic.' }
];
const families = ['Architecture','Block','C4','Entity Relationship','Gantt','GitGraph','Ishikawa','Kanban','Mindmap','Packet','Quadrant Chart','Radar','Railroad','Requirement Diagram','Sankey','Swimlane','Timeline','TreeView','Treemap','Usecase','User Journey','Venn','Wardley','XY Chart','ZenUML','Event Modeling','Cynefin'];
families.forEach((family, index) => examples.splice(index + 4, 0, {
  family, slug: family.toLowerCase().replace(/[^a-z]+/g, '-'),
  source: `%% ${family} bounded example\n${family.toLowerCase()}\n  example`,
  note: 'Bounded family entry; unsupported syntax remains a typed diagnostic.'
}));
const gallery = document.querySelector('#gallery');
const filter = document.querySelector('#filter');
const status = document.querySelector('#status');
const copyLink = document.querySelector('#copy-link');

function runtime() { return window.mermaidNative; }
function safeSvg(payload) {
  if (!payload?.svg) return null;
  const parsed = new DOMParser().parseFromString(payload.svg, 'image/svg+xml');
  const svg = parsed.documentElement;
  if (svg.localName !== 'svg' || parsed.querySelector('parsererror,script,iframe,foreignObject')) return null;
  parsed.querySelectorAll('*').forEach(node => [...node.attributes].forEach(a => { if (a.name.toLowerCase().startsWith('on')) node.removeAttribute(a.name); }));
  return document.importNode(svg, true);
}
function renderCard(card) {
  const article = document.createElement('article'); article.className = 'example'; article.id = card.slug;
  article.innerHTML = `<div class="example-head"><div><p class="eyebrow">${card.family}</p><h2>${card.family}</h2><p>${card.note}</p></div><button type="button" data-copy>Copy source</button></div><pre><code></code></pre><div class="preview" aria-label="Rendered ${card.family}"><p class="muted">Rendering…</p></div>`;
  article.querySelector('code').textContent = card.source;
  article.querySelector('[data-copy]').addEventListener('click', async () => { await navigator.clipboard?.writeText(card.source); status.textContent = `Copied ${card.family} source`; });
  gallery.append(article);
  const draw = () => { const rt = runtime(); if (!rt?.renderMermaidResultJson) { article.querySelector('.preview').textContent = 'Wasm module unavailable'; return; } const payload = JSON.parse(rt.renderMermaidResultJson(card.source)); const svg = safeSvg(payload); const preview = article.querySelector('.preview'); preview.replaceChildren(); if (svg) preview.append(svg); else { const error = document.createElement('p'); error.className = 'error'; error.textContent = payload.diagnostics?.map(d => `${d.code}: ${d.message}`).join('\n') || 'Render rejected'; preview.append(error); } };
  if (runtime()) draw(); else window.addEventListener('mermaid-native-ready', draw, { once: true });
}
function update() { const q = filter.value.trim().toLowerCase(); document.querySelectorAll('.example').forEach(el => { el.hidden = q && !el.textContent.toLowerCase().includes(q); }); status.textContent = `${[...document.querySelectorAll('.example')].filter(e => !e.hidden).length} examples`; }
examples.forEach(renderCard); filter.addEventListener('input', update); copyLink.addEventListener('click', async () => { await navigator.clipboard?.writeText(location.href); status.textContent = 'Permalink copied'; });
update();
