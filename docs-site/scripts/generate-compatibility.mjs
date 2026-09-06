import { copyFile, readFile, writeFile } from 'node:fs/promises'
import { resolve } from 'node:path'
import { fileURLToPath } from 'node:url'

const docsRoot = resolve(fileURLToPath(new URL('.', import.meta.url)), '..')
const repositoryRoot = resolve(docsRoot, '..')
const csvPath = resolve(repositoryRoot, 'compatibility/diagram-families.csv')
const outputPath = resolve(docsRoot, 'reference/families.md')
const publishedCsvPath = resolve(docsRoot, 'public/reference/diagram-families.csv')
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
