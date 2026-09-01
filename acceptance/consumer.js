// Kotlin/Wasm webpack exports an async module under the generated global name.
// Normalize it to the small host contract used by this page before rendering.
const wasmExport = window['mermaid-web'];
if (wasmExport?.then) wasmExport.then(exports => { window.mermaidNative = exports; window.dispatchEvent(new Event('mermaid-native-ready')); }).catch(() => {});
else if (wasmExport) window.mermaidNative = wasmExport;

const examples = [];
const familyExamples = [
  ['Architecture',`architecture-beta
  group api(cloud)[API & gateway]
  service db(database)[Database] in api
  service server(server)[Application server] in api
  db:B --> T:server`,'Service nodes and directed edges (bounded).'],
  ['Block',`block
  columns 3
  api[Public & partner API]:2
  db[Database]
  worker[Worker]:2
  api --> worker
  db --> worker`,'Blocks and connections (bounded).'],
  ['C4',`C4Context
  title Banking context
  Person(customer, "Customer & partner", "Uses the app")
  System(bank, "Banking system", "Shows balances")
  Rel(customer, bank, "Uses", "HTTPS")`,'Context nodes and relationships (bounded).'],
  ['Entity Relationship',`erDiagram
  CUSTOMER {
    int id PK
    string name
  }
  ORDER {
    int id PK
    int customerId FK
  }
  CUSTOMER ||--o{ ORDER : places`,'Entities, cardinality and attributes.'],
  ['Gantt',`gantt
  title Release plan
  dateFormat YYYY-MM-DD
  section Build
  Parser :done, parse, 2026-08-19, 2d
  Renderer :active, render, 2026-08-21, 3d`,'Sections and dated tasks.'],
  ['GitGraph',`gitGraph
  commit id: "base" tag: "v1.0"
  branch develop
  commit id: "feature" type: HIGHLIGHT
  checkout main
  commit id: "release" type: REVERSE
  merge develop id: "merge" tag: "v2 & stable"`,'Commits, branches and merge.'],
  ['Ishikawa',`ishikawa-beta
  Blurry Photo
  Process
    Out of focus
    Shutter speed too slow
  Equipment
    LENS
      Dirty lens
    SENSOR
      Damaged sensor`,'Cause/effect analysis (bounded).'],
  ['Kanban',`kanban
todo[Todo]
  spec[Write & review spec]
  tests[Add tests]
done[Done]
  release[Ship release]`,'Columns and cards.'],
  ['Mindmap',`mindmap
  root((Project plan))
    Discovery
      [Requirements]
      Research
    Delivery
      ((Native SVG))`,'Hierarchical topics.'],
  ['Packet',`packet
  title UDP Packet
  0-15: "Source Port"
  16-31: "Destination Port"
  32-47: "Length"
  48-63: "Checksum"`,'Bit ranges and labels.'],
  ['Pie',`pie showData title Pets adopted
  "Dogs" : 386
  "Cats" : 85
  "Rats" : 15`,'A small data visualization.'],
  ['Quadrant Chart',`quadrantChart
  title Product portfolio
  x-axis Low reach --> High reach
  y-axis Low engagement --> High engagement
  quadrant-1 Expand
  quadrant-2 Promote
  quadrant-3 Re-evaluate
  quadrant-4 Improve
  Campaign A: [0.3, 0.6]
  Campaign B: [0.75, 0.25]`,'Axes and plotted points.'],
  ['Radar',`radar-beta
  title Team skill matrix
  axis m["Math"], s["Science"], e["English"]
  curve alice["Alice"]{85, 78, 92}
  curve bob["Bob"]{62, 84, 55}
  max 100`,'Axes and a bounded series.'],
  ['Railroad',`railroad-beta
Diagram(
  Sequence(
    'token',
    Choice(0,
      NonTerminal('session'),
      Optional('refresh')
    ),
    Stack('validate', 'store')
  )
)`,'A compact grammar flow.'],
  ['Requirement Diagram',`requirementDiagram
  requirement secure_login {
    id: AUTH-1
    text: Users authenticate securely
    risk: high
    verifymethod: test
  }
  element mobile_client {
    type: application
    docref: docs/auth.md
  }
  mobile_client - satisfies -> secure_login`,'Requirement metadata.'],
  ['Sankey',`sankey
Grid,Industry,12.5
Grid,"Heating, homes",7.25
Industry,Losses & exports,2.5`,'Flow quantities (bounded).'],
  ['Swimlane',`swimlane-beta LR
  subgraph customer [Customer & partner]
    request[Request service]
    receive((Receive update))
  end
  subgraph support [Support team]
    triage{Known issue?}
    answer[Send answer]
  end
  request -->|handoff & review| triage
  triage --> answer --> receive`,'Swimlane lanes and edges (bounded).'],
  ['Timeline',`timeline
  title Product history
  2024 : Launch : First users
  2025 : Scale`,'Dates and events.'],
  ['TreeView',`treeView-beta
  project/
    src/
      index.ts
    "README file.md"
  package.json`,'Tree-shaped hierarchy.'],
  ['Treemap',`treemap-beta
"Products & services"
  "Mobile": 45
  "Web": 35
  "API": 20`,'Nested weighted leaves.'],
  ['Usecase',`usecase-beta
direction LR
actor Customer("Customer")
Checkout("Place order")
Receipt[Create receipt]
Customer -- "starts" --> Checkout
Checkout --> Receipt`,'Actors and use cases.'],
  ['User Journey',`journey
  title Checkout journey
  section Discover
  Find product: 4: Shopper
  Review & compare: 3: Shopper, Advisor
  section Purchase
  Add to cart: 5: Shopper
  Pay securely: 4: Shopper, Payment service`,'Sections, actor steps, and scores.'],
  ['Venn',`venn-beta
title "Team overlap"
set Frontend["Frontend & design"]:20
set Backend:16
set Platform:12
union Frontend,Backend["APIs"]:5
union Frontend,Backend,Platform["Shared tooling"]:2`,'Set overlap (bounded).'],
  ['Wardley',`wardley-beta
title Tea Shop Value Chain
anchor Business [0.95, 0.63]
component Cup of Tea [0.79, 0.61]
component Tea [0.63, 0.81]
component Hot Water [0.52, 0.80]
component Kettle [0.43, 0.35]
component Power [0.10, 0.70]
Business -> Cup of Tea
Cup of Tea -> Tea
Cup of Tea -> Hot Water
Hot Water -> Kettle
Kettle -> Power
evolve Kettle 0.62
evolve Power 0.89
note "Standardising power allows Kettles to evolve faster" [0.30, 0.49]`,'Anchor and component evolution.'],
  ['XY Chart',`xychart-beta
  title "Quarterly sales"
  x-axis "Quarter" [Q1, Q2, Q3, Q4]
  y-axis "Revenue" 0 --> 100
  bar [20, 45, 70, 85]
  line [25, 40, 75, 90]`,'Categorical bars.'],
  ['ZenUML',`zenuml
title Token handshake
Client
Store as Token store
Client->Gateway.submit()
Gateway->Store.lookup
Client->Gateway: cancel`,'Sequence messages (bounded).'],
  ['Event Modeling',`eventmodeling
title Cart & inventory
tf 01 ui CartUI
tf 02 cmd AddItem
tf 03 evt ItemAdded
rf 04 evt External.InventoryChanged
tf 05 pcr InventoryProcessor
tf 06 rmo InventoryView ->> 03 ->> 04`,'Command, event and view.'],
  ['Cynefin',`cynefin-beta
  title Incident response
  complex
    "Investigate & learn"
    "Run chaos experiment"
  complicated
    "Expert analysis"
  clear
    "Apply known fix"
  chaotic
    "Page on-call"
  confusion
    "Unknown failure"
  complex --> complicated : "Pattern found"
  chaotic --> complex : "Stabilized"`,'Domains and labels.'],
  ['Class',`classDiagram
class Animal
Animal : +String name
Animal : +eat()
Animal <|-- Duck
class Duck
Duck : +swim()`,'Typed relationships and members.'],
  ['State',`stateDiagram-v2
  direction LR
  [*] --> Idle
  state "Processing request" as Working
  Idle --> Working: start
  Working --> [*]: finish`,'A compact lifecycle.'],
  ['Sequence',`sequenceDiagram
  Alice->>Bob: Hello Bob!
  Bob-->>Alice: Hi Alice!`,'A minimal request/response.'],
  ['Flowchart',`graph TD
  A[Start] ==> B[Process]
  B --> C[End]`,'Nodes, labels and branching.']
];
if (!(familyExamples.length === 32)) throw new Error(`Expected 32 positive family examples, got ${familyExamples.length}`);
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
function renderCard(card, target = gallery) {
  const article = document.createElement('article'); article.className = 'example'; article.id = card.slug;
  article.innerHTML = `<div class="example-head"><div><p class="eyebrow">${card.family}</p><h2>${card.family}</h2><p>${card.note}</p></div><button type="button" data-copy>Copy source</button></div><pre><code></code></pre><div class="preview" aria-label="Rendered ${card.family}"><p class="muted">Rendering…</p></div>`;
  article.querySelector('code').textContent = card.source;
  article.querySelector('[data-copy]').addEventListener('click', async () => { await navigator.clipboard?.writeText(card.source); status.textContent = `Copied ${card.family} source`; });
  target.append(article);
  const draw = () => { const rt = runtime(); if (!rt?.renderMermaidResultJson) { article.querySelector('.preview').textContent = 'Wasm module unavailable'; return; } const payload = JSON.parse(rt.renderMermaidResultJson(card.source)); const svg = safeSvg(payload); const preview = article.querySelector('.preview'); preview.replaceChildren(); if (svg) preview.append(svg); else { const error = document.createElement('p'); error.className = 'error'; error.textContent = payload.diagnostics?.map(d => `${d.code}: ${d.message}`).join('\n') || 'Render rejected'; preview.append(error); } };
  if (runtime()) draw(); else window.addEventListener('mermaid-native-ready', draw, { once: true });
}
function update() { const q = filter.value.trim().toLowerCase(); document.querySelectorAll('#gallery .example').forEach(el => { el.hidden = q && !el.textContent.toLowerCase().includes(q); }); status.textContent = `${[...document.querySelectorAll('#gallery .example')].filter(e => !e.hidden).length} examples`; }
examples.forEach(card => renderCard(card));
filter.addEventListener('input', update); copyLink.addEventListener('click', async () => {
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
