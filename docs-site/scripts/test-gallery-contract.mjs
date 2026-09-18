import { readFile, readdir } from 'node:fs/promises'
import { resolve } from 'node:path'
import { fileURLToPath } from 'node:url'
import { DOMParser as XmlDomParser, XMLSerializer as XmlDomSerializer } from '@xmldom/xmldom'
import {
  ALLOWED_TAGS,
  ALLOWED_ATTRS,
  validateAttributeValue,
  validateSvgDomTree,
  sanitizeSvg
} from '../../acceptance/svg-sanitizer.js'

const docsRoot = resolve(fileURLToPath(new URL('.', import.meta.url)), '..')
const repoRoot = resolve(docsRoot, '..')
const samplesDir = resolve(repoRoot, 'samples')
const consumerJsPath = resolve(repoRoot, 'acceptance/consumer.js')
const galleryJsonPath = resolve(docsRoot, '.vitepress/theme/data/family-examples.json')
const vueComponentPath = resolve(docsRoot, '.vitepress/theme/components/MermaidGallery.vue')

console.log('--- Running Gallery Contract and Security Regression Tests ---')

// 1. Verify family-examples.json has exactly 32 families matching samples
const galleryJson = JSON.parse(await readFile(galleryJsonPath, 'utf8'))
if (galleryJson.length !== 32) {
  throw new Error(`Expected 32 gallery examples in JSON, got ${galleryJson.length}`)
}

for (const card of galleryJson) {
  const mmdPath = resolve(samplesDir, card.sampleFile.replace(/\.svg$/, '.mmd'))
  const expectedSource = (await readFile(mmdPath, 'utf8')).trim()
  if (card.source.trim() !== expectedSource) {
    throw new Error(`Source mismatch in family "${card.family}"`)
  }
  const svgPath = resolve(samplesDir, card.sampleFile)
  const expectedSvg = (await readFile(svgPath, 'utf8')).trim()
  if (card.svg.trim() !== expectedSvg) {
    throw new Error(`SVG mismatch in family "${card.family}"`)
  }
}
console.log('✓ All 32 families in family-examples.json match samples/*.mmd and samples/*.svg byte-for-byte')

// 2. Verify specific blocker regressions
const ishikawa = galleryJson.find(c => c.family === 'Ishikawa')
if (!ishikawa.source.includes('Subject moved too quickly')) {
  throw new Error('Ishikawa source missing Environment section')
}
if (!ishikawa.svg.includes('Subject moved too quickly')) {
  throw new Error('Ishikawa SVG missing Environment section')
}
console.log('✓ Ishikawa Environment section matched across source and SVG golden')

const packet = galleryJson.find(c => c.family === 'Packet')
if (!packet.source.includes('64-95: "Data"')) {
  throw new Error('Packet source missing Data range')
}
if (!packet.svg.includes('>64<') || !packet.svg.includes('>95<') || !packet.svg.includes('>Data<')) {
  throw new Error('Packet SVG missing Data start/end bit indexes')
}
console.log('✓ Packet Data field matched across source and SVG golden')

const treeview = galleryJson.find(c => c.family === 'TreeView')
const lines = treeview.source.split('\n')
if (!lines[1].startsWith('    project/')) {
  throw new Error('TreeView missing 4-space positive indentation')
}
console.log('✓ TreeView 4-space positive indentation verified')

// 3. Verify acceptance/consumer.js has all 32 examples matching samples/*.mmd
const consumerJs = await readFile(consumerJsPath, 'utf8')
const markerStart = 'const familyExamples = ['
const markerEnd = '];\nif (!(familyExamples.length === 32))'
const startIdx = consumerJs.indexOf(markerStart)
const endIdx = consumerJs.indexOf(markerEnd)
const slice = consumerJs.substring(startIdx + markerStart.length, endIdx)
const familyTuples = new Function(`return [${slice}]`)()

if (familyTuples.length !== 32) {
  throw new Error(`Expected 32 family examples in consumer.js, got ${familyTuples.length}`)
}

for (const [family, source] of familyTuples) {
  const card = galleryJson.find(c => c.family === family)
  if (!card) throw new Error(`Family ${family} not in galleryJson`)
  if (source.trim() !== card.source.trim()) {
    throw new Error(`consumer.js source mismatch for family ${family}`)
  }
}
console.log('✓ acceptance/consumer.js matches all 32 canonical samples/*.mmd')

// 4. Verify MermaidGallery.vue imports shared sanitizer, implements debounce watch, and handles galleryOnly navigation
const vueCode = await readFile(vueComponentPath, 'utf8')
if (!vueCode.includes("from '../utils/svg-sanitizer'") && !vueCode.includes('sanitizeSvg')) {
  throw new Error('MermaidGallery.vue missing shared sanitizer import')
}
if (!vueCode.includes('debounceTimer') || !vueCode.includes('watch(editorSource')) {
  throw new Error('MermaidGallery.vue missing debounced editor watch')
}
if (!vueCode.includes('encodeSourceHash') || !vueCode.includes('props.galleryOnly') || !vueCode.includes('playground#source=')) {
  throw new Error('MermaidGallery.vue missing galleryOnly transfer to playground')
}
if (!vueCode.includes('max-width: 100%') || !vueCode.includes('minmax(0, 1fr)')) {
  throw new Error('MermaidGallery.vue missing overflow containment (svg max-width / shrinking grid tracks)')
}
console.log('✓ MermaidGallery.vue imports shared sanitizer, implements debounced input watch, and routes galleryOnly Try-in-editor to /playground')
console.log('✓ MermaidGallery.vue constrains wide SVGs and grid tracks so gallery cards cannot overflow the page')

const officialPreviewPath = resolve(docsRoot, '.vitepress/theme/components/OfficialMermaidPreview.vue')
const officialPreview = await readFile(officialPreviewPath, 'utf8')
if (!officialPreview.includes("import('mermaid')") || !officialPreview.includes("securityLevel: 'strict'")) {
  throw new Error('OfficialMermaidPreview.vue must lazy-load mermaid with securityLevel strict')
}
if (!vueCode.includes('OfficialMermaidPreview') || !vueCode.includes('Official Mermaid') || !vueCode.includes('comparison only')) {
  throw new Error('MermaidGallery.vue missing official Mermaid comparison pane')
}
if (!vueCode.includes('card-compare-grid') || !vueCode.includes('playground-compare-grid') || !vueCode.includes('-official')) {
  throw new Error('Official comparison pane must be full-width with its own zoom controls')
}
if (vueCode.includes('fallback') && vueCode.includes('mermaid.render') && vueCode.includes('editorPreviewHtml')) {
  throw new Error('Official Mermaid must not replace the Native preview path')
}
console.log('✓ Official Mermaid comparison pane is present, lazy-loaded, strict, and not a Native fallback')

// 5. Test SVG Security Sanitizer logic using real XML DOMParser and XMLSerializer with strict error reporting
function createStrictXmlParser() {
  let parseError = null
  const parser = new XmlDomParser({
    onError: (level, msg) => {
      // Treat any warning, error or fatalError as fail-closed XML error
      if (!parseError) parseError = `XML ${level}: ${msg}`
    }
  })
  return {
    parseFromString: (str, type) => {
      parseError = null
      const doc = parser.parseFromString(str, type)
      if (parseError) throw new Error(parseError)
      return doc
    }
  }
}

const strictParser = createStrictXmlParser()
const strictSerializer = new XmlDomSerializer()

const maliciousPayloads = [
  { name: 'XSS <a> tag with javascript: link', payload: '<svg><a href="javascript:alert(1)"><text>Click</text></a></svg>' },
  { name: 'Inline <style> tag injection', payload: '<svg><style>body { display: none; }</style><rect width="10" height="10"/></svg>' },
  { name: '<script> tag injection', payload: '<svg><script>alert(1)</script></svg>' },
  { name: '<iframe> tag injection', payload: '<svg><iframe src="https://evil.com"></iframe></svg>' },
  { name: '<foreignObject> tag injection', payload: '<svg><foreignObject><body xmlns="http://www.w3.org/1999/xhtml"><script>alert(1)</script></body></foreignObject></svg>' },
  { name: '<use> tag with xlink:href', payload: '<svg><use xlink:href="#something"/></svg>' },
  { name: '<image> tag with external src', payload: '<svg><image href="http://evil.com/img.png"/></svg>' },
  { name: 'Event handler onclick attribute', payload: '<svg><rect onclick="alert(1)" width="10" height="10"/></svg>' },
  { name: 'Event handler onload attribute', payload: '<svg onload="alert(1)"><rect width="10" height="10"/></svg>' },
  { name: 'Unsafe url(javascript:) scheme', payload: '<svg><text fill="url(javascript:alert(1))">test</text></svg>' },
  { name: 'style attribute', payload: '<svg><rect style="fill:red" width="10" height="10"/></svg>' },
  { name: '<animate> tag', payload: '<svg><animate attributeName="x" from="0" to="10"/></svg>' },
  { name: 'CSS escape \\72 bypass vector', payload: '<svg><text fill="u\\72 l(#probe)">x</text></svg>' },
  { name: 'CSS escape backslash in stroke', payload: '<svg><line x1="0" y1="0" x2="10" y2="10" stroke="\\75 rl(http://evil.com)"/></svg>' },
  { name: 'External fragment url in fill', payload: '<svg><rect fill="url(#probe)" width="10" height="10"/></svg>' },
  { name: 'External resource url in stroke', payload: '<svg><rect stroke="url(https://evil.com/leak)" width="10" height="10"/></svg>' },
  { name: 'id attribute injection', payload: '<svg><rect id="custom-id" width="10" height="10"/></svg>' },
  { name: 'class attribute injection', payload: '<svg><rect class="custom-class" width="10" height="10"/></svg>' },
  { name: 'Unquoted onload attribute bypass', payload: '<svg><rect onload=alert(1) width="10" height="10"/></svg>' },
  { name: 'Unquoted href attribute bypass', payload: '<svg><rect href=javascript:alert(1) width="10" height="10"/></svg>' },
  { name: 'Mismatched closing tags', payload: '<svg><rect width="10" height="10"></svg>' },
  { name: 'Duplicate attribute injection', payload: '<svg><rect width="10" width="20" height="10"/></svg>' },
  // Document-level / Prolog malicious vectors
  { name: 'xml-stylesheet with external href', payload: '<?xml-stylesheet href="https://evil.example/a.css" type="text/css"?><svg xmlns="http://www.w3.org/2000/svg"/>' },
  { name: 'xml-stylesheet with data URI', payload: '<?xml-stylesheet href="data:text/css,body{background:red}" type="text/css"?><svg xmlns="http://www.w3.org/2000/svg"/>' },
  { name: 'Arbitrary processing instruction outside root', payload: '<?custom-pi content?><svg xmlns="http://www.w3.org/2000/svg"/>' },
  { name: 'Processing instruction inside SVG element', payload: '<svg xmlns="http://www.w3.org/2000/svg"><?pi inside?><rect width="10" height="10"/></svg>' },
  { name: 'External SYSTEM DOCTYPE declaration', payload: '<!DOCTYPE svg SYSTEM "https://evil.example/dtd"><svg xmlns="http://www.w3.org/2000/svg"/>' },
  { name: 'External PUBLIC DOCTYPE declaration', payload: '<!DOCTYPE svg PUBLIC "-//W3C//DTD SVG 1.1//EN" "http://www.w3.org/Graphics/SVG/1.1/DTD/svg11.dtd"><svg xmlns="http://www.w3.org/2000/svg"/>' },
  { name: 'Internal ENTITY declaration in DOCTYPE', payload: '<!DOCTYPE svg [ <!ENTITY xxe SYSTEM "http://evil.com"> ]><svg xmlns="http://www.w3.org/2000/svg"/>' },
  { name: 'CDATA section inside SVG element', payload: '<svg xmlns="http://www.w3.org/2000/svg"><![CDATA[<script>alert(1)</script>]]></svg>' }
]

for (const testCase of maliciousPayloads) {
  const result = sanitizeSvg(testCase.payload, strictParser, strictSerializer)
  if (result.ok) {
    throw new Error(`Sanitizer failed to reject malicious payload: ${testCase.name}`)
  }
}
console.log(`✓ DOM-based sanitizer successfully rejected all ${maliciousPayloads.length} malicious SVG vectors (including doctype, xml-stylesheet, PIs, CDATA, unquoted attrs, malformed XML & duplicate attrs)`)

// 6. Test all 34 golden samples pass the shared strict fail-closed sanitizer and are cleanly serialized
const sampleFiles = await readdir(samplesDir)
for (const file of sampleFiles.filter(f => f.endsWith('.svg'))) {
  const svgContent = await readFile(resolve(samplesDir, file), 'utf8')
  const result = sanitizeSvg(svgContent, strictParser, strictSerializer)
  if (!result.ok) {
    throw new Error(`Shared sanitizer falsely rejected valid sample ${file}: ${result.error}`)
  }
  if (!result.svg.startsWith('<svg') || !result.svg.endsWith('</svg>')) {
    throw new Error(`Serialized output for sample ${file} is not a valid root <svg> element`)
  }
}
console.log('✓ All 34 golden sample SVGs pass the DOM-based fail-closed sanitizer and serialize cleanly via XMLSerializer')

console.log('--- ALL GALLERY CONTRACT AND SECURITY TESTS PASSED ---')
