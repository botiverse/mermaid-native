import { copyFile, mkdir, readdir, readFile, writeFile } from 'node:fs/promises'
import { existsSync } from 'node:fs'
import { resolve } from 'node:path'
import { fileURLToPath } from 'node:url'

const docsRoot = resolve(fileURLToPath(new URL('.', import.meta.url)), '..')
const repositoryRoot = resolve(docsRoot, '..')
const csvPath = resolve(repositoryRoot, 'compatibility/diagram-families.csv')
const outputPath = resolve(docsRoot, 'reference/families.md')
const publishedCsvPath = resolve(docsRoot, 'public/reference/diagram-families.csv')
const consumerJsPath = resolve(repositoryRoot, 'acceptance/consumer.js')
const samplesDir = resolve(repositoryRoot, 'samples')
const galleryJsonPath = resolve(docsRoot, '.vitepress/theme/data/family-examples.json')

const lines = (await readFile(csvPath, 'utf8')).trim().split('\n').slice(1)

const rows = lines.map((line) => {
  const fields = line.split(',')
  return {
    family: fields[0],
    syntax: fields[1],
    status: fields[2],
    parser: fields[3],
    ast: fields[4],
    layout: fields[5],
    svg: fields[6],
    notes: fields.slice(7).join(',').trim(),
  }
})

const statusLabel = (value) => value === 'in_progress' ? 'In progress' : value.replaceAll('_', ' ')
const syntaxReference = {
  usecase: 'https://mermaid.js.org/intro/syntax-reference.html',
}
const table = rows.map((row) => {
  const url = syntaxReference[row.syntax] ?? `https://mermaid.js.org/syntax/${row.syntax}.html`
  return `| \`${row.family}\` | [${row.syntax}](${url}) | ${statusLabel(row.status)} | ${row.parser} | ${row.layout} | ${row.svg} |`
}).join('\n')

const body = `# Diagram family matrix

This page is generated from the repository's machine-readable compatibility registry. Every listed family has a bounded vertical slice; **in progress** does not mean full Mermaid parity.

| Family | Mermaid syntax reference | Status | Parser | Layout | SVG |
| --- | --- | --- | --- | --- | --- |
${table}

## Reading the matrix

- **In progress** means a tested vertical slice exists, while unsupported syntax still fails closed.
- A family is not considered fully compatible until parser, typed AST, layout, rendering, positive and negative fixtures, and platform evidence are complete.
- Detailed syntax boundaries remain in [compatibility/diagram-families.csv](https://github.com/botiverse/mermaid-native/blob/main/compatibility/diagram-families.csv).

_Last generated from ${rows.length} registry entries._
`

await writeFile(outputPath, body)
await copyFile(csvPath, publishedCsvPath)

// Extract canonical family examples from acceptance/consumer.js to keep a single source of truth
const consumerContent = await readFile(consumerJsPath, 'utf8')
const markerStart = 'const familyExamples = ['
const markerEnd = '];\nif (!(familyExamples.length === 32))'
const startIdx = consumerContent.indexOf(markerStart)
const endIdx = consumerContent.indexOf(markerEnd)
if (startIdx === -1 || endIdx === -1) {
  throw new Error('Could not find familyExamples declaration in acceptance/consumer.js')
}

const slice = consumerContent.substring(startIdx + markerStart.length, endIdx)
// Safe evaluation of the literal array tuple
const familyTuples = new Function(`return [${slice}]`)()
if (familyTuples.length !== 32) {
  throw new Error(`Expected exactly 32 family examples, got ${familyTuples.length}`)
}

const sampleMapping = {
  'Architecture': 'architecture-api-stack.svg',
  'Block': 'block-service-map.svg',
  'C4': 'c4-banking-context.svg',
  'Entity Relationship': 'entity-customer-order.svg',
  'Gantt': 'gantt-release-plan.svg',
  'GitGraph': 'gitgraph-release-flow.svg',
  'Ishikawa': 'ishikawa-photo-quality.svg',
  'Kanban': 'kanban-release-board.svg',
  'Mindmap': 'mindmap-project-plan.svg',
  'Packet': 'packet-udp.svg',
  'Pie': 'pie-pets.svg',
  'Quadrant Chart': 'quadrant-product-portfolio.svg',
  'Radar': 'radar-team-skills.svg',
  'Railroad': 'railroad-auth-flow.svg',
  'Requirement Diagram': 'requirement-login.svg',
  'Sankey': 'sankey-energy-flow.svg',
  'Swimlane': 'swimlane-support-escalation.svg',
  'Timeline': 'timeline-product-history.svg',
  'TreeView': 'treeview-project.svg',
  'Treemap': 'treemap-product-mix.svg',
  'Usecase': 'usecase-order-flow.svg',
  'User Journey': 'user-journey-checkout.svg',
  'Venn': 'venn-team-overlap.svg',
  'Wardley': 'wardley-tea-shop.svg',
  'XY Chart': 'xy-quarterly-sales.svg',
  'ZenUML': 'zenuml-token-handshake.svg',
  'Event Modeling': 'eventmodeling-cart-flow.svg',
  'Cynefin': 'cynefin-incident-response.svg',
  'Class': 'class-animal.svg',
  'State': 'state-lifecycle.svg',
  'Sequence': 'sequence-request-response.svg',
  'Flowchart': 'flowchart-thick-arrow.svg',
}

const galleryExamples = []
for (const [family, source, note] of familyTuples) {
  const sampleFile = sampleMapping[family]
  if (!sampleFile) {
    throw new Error(`No sample SVG mapping defined for family: ${family}`)
  }
  const svgPath = resolve(samplesDir, sampleFile)
  if (!existsSync(svgPath)) {
    throw new Error(`Sample SVG file missing: ${svgPath}`)
  }
  const svg = (await readFile(svgPath, 'utf8')).trim()
  const slug = family.toLowerCase().replace(/[^a-z0-9]+/g, '-')
  galleryExamples.push({
    family,
    slug,
    source,
    note: `${note} Support is intentionally bounded; unsupported constructs return typed diagnostics.`,
    svg,
    sampleFile,
  })
}

await mkdir(resolve(docsRoot, '.vitepress/theme/data'), { recursive: true })
await writeFile(galleryJsonPath, JSON.stringify(galleryExamples, null, 2))

// If local Wasm executable exists, copy to public/wasm for local dev
const wasmOutputDir = resolve(docsRoot, 'public/wasm')
const wasmSourceDir = resolve(repositoryRoot, 'mermaid-web/build/kotlin-webpack/wasmJs/productionExecutable')
if (existsSync(wasmSourceDir)) {
  await mkdir(wasmOutputDir, { recursive: true })
  const files = await readdir(wasmSourceDir)
  for (const file of files) {
    if (file.endsWith('.wasm') || file.endsWith('.js')) {
      await copyFile(resolve(wasmSourceDir, file), resolve(wasmOutputDir, file))
    }
  }
}
