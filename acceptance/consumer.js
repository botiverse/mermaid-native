// Kotlin/Wasm webpack exports an async module under the generated global name.
// Normalize it to the small host contract used by this page before rendering.
const wasmExport = window['mermaid-web'];
if (wasmExport?.then) wasmExport.then(exports => { window.mermaidNative = exports; window.dispatchEvent(new Event('mermaid-native-ready')); }).catch(() => {});
else if (wasmExport) window.mermaidNative = wasmExport;

const examples = [
  { family: 'Unsupported example', slug: 'unsupported', source: 'graph TD\n  A ==> B', note: 'Unsupported syntax returns a typed diagnostic.' }
];
const familyExamples = [
  ['Architecture','architecture-beta\n  service api(server)[API]\n  service db(database)[DB]\n  api:R --> L:db','Service nodes and directed edges (bounded).'],
  ['Block','block-beta\n  columns 2\n  A[Client]\n  B[Server]\n  A --> B','Blocks and connections (bounded).'],
  ['C4','C4Context\n  Person(user, "User")\n  System(app, "App")\n  Rel(user, app, "uses")','Context nodes and relationships (bounded).'],
  ['Entity Relationship','erDiagram\n  CUSTOMER ||--o{ ORDER : places\n  CUSTOMER {\n    string id\n  }','Entities, cardinality and attributes.'],
  ['Gantt','gantt\n  title Release\n  section Build\n  API :done, api, 2025-01-01, 2d','Sections and dated tasks.'],
  ['GitGraph','gitGraph\n  commit\n  branch feature\n  checkout feature\n  commit\n  checkout main\n  merge feature','Commits, branches and merge.'],
  ['Ishikawa','fishbone\nEffect\n  Cause\n    People\n    Process','Cause/effect analysis (bounded).'],
  ['Kanban','kanban\n  Todo\n    task1[Write docs]\n  Done\n    task2[Ship]','Columns and cards.'],
  ['Mindmap','mindmap\n  root((Product))\n    Docs\n      Quickstart\n    Demo','Hierarchical topics.'],
  ['Packet','packet-beta\n  0-7: "Header"\n  8-15: "Payload"','Bit ranges and labels.'],
  ['Pie','pie title Browser share\n  "Chrome" : 65\n  "Safari" : 20\n  "Other" : 15','A small data visualization.'],
  ['Quadrant Chart','quadrantChart\n  title Product portfolio\n  x-axis Effort --> Value\n  y-axis Low --> High\n  "Docs": [0.3, 0.8]','Axes and plotted points.'],
  ['Radar','radar-beta\n  title Team skills\n  axis Docs,Code,UX\n  curve Team{8,7,6}','Axes and a bounded series.'],
  ['Railroad','railroad-beta\n  start=>start: Request\n  end=>end: Done\n  start->end','A compact grammar flow.'],
  ['Requirement Diagram','requirementDiagram\n  requirement req {\n    id: 1\n    text: Safe render\n    risk: low\n  }','Requirement metadata.'],
  ['Sankey','sankey-beta\n  Solar,Grid,40\n  Grid,App,35\n  Solar,App,20','Flow quantities (bounded).'],
  ['Swimlane','flowchart LR\n  subgraph Support\n    A[Ticket] --> B[Resolve]\n  end','Swimlane via subgraph.'],
  ['Timeline','timeline\n  title Product history\n  2024 : Launch\n  2025 : Teams','Dates and events.'],
  ['TreeView','flowchart TD\n  Root --> ChildA\n  Root --> ChildB','Tree-shaped hierarchy.'],
  ['Treemap','treemap-beta\n  "Product"\n    "Docs": 40\n    "Runtime": 60','Nested weighted leaves.'],
  ['Usecase','usecaseDiagram\n  actor User\n  RenderDiagram("Render diagram")\n  User --> RenderDiagram','Actors and use cases.'],
  ['User Journey','journey\n  title Checkout\n  User: Open cart: 5\n  User: Pay: 3','Actor steps and scores.'],
  ['Venn','venn-beta\n  title Team overlap\n  A: 10\n  B: 8\n  A&B: 3','Set overlap (bounded).'],
  ['Wardley','wardley-beta\n  title Tea shop\n  anchor: Customer\n  component: Tea [0.6, 0.8]','Anchor and component evolution.'],
  ['XY Chart','xychart-beta\n  title Quarterly sales\n  x-axis [Q1, Q2, Q3]\n  y-axis Sales --> 100\n  bar [30, 55, 80]','Categorical bars.'],
  ['ZenUML','zenuml\n  @Actor User\n  User->API: Render\n  API-->User: SVG','Sequence messages (bounded).'],
  ['Event Modeling','eventmodeling\n  command Submit\n  event Submitted\n  view Receipt','Command, event and view.'],
  ['Cynefin','cynefin\n  title Incident response\n  clear\n  "Known fix"\n  complicated\n  "Expert analysis"','Domains and labels.'],
  ['Class','classDiagram\n  Animal <|-- Duck\n  Animal : +String name\n  Duck : +swim()','Typed relationships and members.'],
  ['State','stateDiagram-v2\n  [*] --> Draft\n  Draft --> Published\n  Published --> [*]','A compact lifecycle.'],
  ['Sequence','sequenceDiagram\n  Alice->>Bob: Hello Bob\n  Bob-->>Alice: Hi Alice','A minimal request/response.'],
  ['Flowchart','flowchart TD\n  A[Start] --> B{Ready?}\n  B -->|Yes| C[Ship]','Nodes, labels and branching.']
];
familyExamples.forEach(([family, source, note]) => examples.push({
  family, slug: family.toLowerCase().replace(/[^a-z]+/g, '-'), source,
  note: `${note} Support is intentionally bounded; unsupported constructs return typed diagnostics.`
}));
const gallery = document.querySelector('#gallery');
const filter = document.querySelector('#filter');
const status = document.querySelector('#status');
const copyLink = document.querySelector('#copy-link');
const editor = document.querySelector('#editor');
const renderButton = document.querySelector('#render');
const editorPreview = document.querySelector('#editor-preview');
const editorStatus = document.querySelector('#editor-status');
const copyEditor = document.querySelector('#copy-editor');

function decodeSource() {
  const match = location.hash.match(/^#source=([^&]+)/);
  if (!match) return null;
  try { return decodeURIComponent(escape(atob(match[1].replace(/-/g, '+').replace(/_/g, '/')))); } catch { return null; }
}
const sharedSource = decodeSource();
if (sharedSource && editor) editor.value = sharedSource;

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
examples.forEach(renderCard); filter.addEventListener('input', update); copyLink.addEventListener('click', async () => {
  const bytes = unescape(encodeURIComponent(editor?.value || ''));
  const encoded = btoa(bytes).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
  const permalink = `${location.origin}${location.pathname}#source=${encoded}`;
  await navigator.clipboard?.writeText(permalink); history.replaceState(null, '', `#source=${encoded}`); status.textContent = 'Permalink copied';
});
function renderEditor() {
  const rt = runtime();
  if (!rt?.renderMermaidResultJson) { editorStatus.textContent = 'Wasm module unavailable'; return; }
  const payload = JSON.parse(rt.renderMermaidResultJson(editor.value));
  const svg = safeSvg(payload); editorPreview.replaceChildren();
  if (svg) { editorPreview.append(svg); editorStatus.textContent = 'Rendered successfully'; }
  else { const error = document.createElement('p'); error.className = 'error'; error.textContent = payload.diagnostics?.map(d => `${d.code}: ${d.message} (line ${d.line ?? '?'}, column ${d.column ?? '?'})`).join('\n') || 'Render rejected'; editorPreview.append(error); editorStatus.textContent = 'Render failed'; }
}
renderButton?.addEventListener('click', renderEditor);
copyEditor?.addEventListener('click', async () => { await navigator.clipboard?.writeText(editor.value); editorStatus.textContent = 'Source copied'; });
window.addEventListener('mermaid-native-ready', () => { if (editor) renderEditor(); }, { once: true });
update();
