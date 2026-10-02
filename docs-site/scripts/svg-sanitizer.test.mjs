import test from 'node:test'
import assert from 'node:assert/strict'
import { DOMParser, XMLSerializer } from '@xmldom/xmldom'
import { sanitizeSvg } from '../../acceptance/svg-sanitizer.js'

const sanitize = body => sanitizeSvg(`<svg xmlns="http://www.w3.org/2000/svg" role="img">${body}</svg>`, new DOMParser(), new XMLSerializer())

test('consumer preserves escaped accessibility title and description', () => {
  const result = sanitize('<title>Accounts &amp; &lt;orders&gt;</title><desc>Two entities\nand their relation</desc><rect x="0" y="0" width="10" height="10"/>')
  assert.equal(result.ok, true, result.error)
  assert.equal(result.element.getElementsByTagName('title')[0].textContent, 'Accounts & <orders>')
  assert.equal(result.element.getElementsByTagName('desc')[0].textContent, 'Two entities\nand their relation')
})

test('accessibility metadata cannot admit active content or child elements', () => {
  for (const body of ['<title onload="alert(1)">x</title>', '<desc><script>alert(1)</script></desc>', '<title><text>x</text></title>', '<desc><image href="https://example.com/x"/></desc>']) {
    assert.equal(sanitize(body).ok, false, body)
  }
})

test('consumer accepts numeric weights emitted by entity styles', () => {
  assert.equal(sanitize('<text font-weight="550">Account</text>').ok, true)
  assert.equal(sanitize('<text font-weight="1001">Account</text>').ok, false)
})


test('consumer preserves scoped Native accessibility references', () => {
  const input = '<svg xmlns="http://www.w3.org/2000/svg" role="graphics-document document" aria-roledescription="flowchart" aria-labelledby="chart-title-first" aria-describedby="chart-desc-first"><title id="chart-title-first">Accounts</title><desc id="chart-desc-first">Two accounts</desc></svg>'
  const result = sanitizeSvg(input, new DOMParser(), new XMLSerializer())
  assert.equal(result.ok, true, result.error)
  assert.equal(result.element.getAttribute('aria-labelledby'), 'chart-title-first')
  assert.equal(result.element.getAttribute('aria-describedby'), 'chart-desc-first')
  assert.equal(result.element.getAttribute('role'), 'graphics-document document')
  for (const bad of [
    input.replace('aria-labelledby="chart-title-first"', 'aria-labelledby="chart-title-outside"'),
    input.replace('aria-labelledby="chart-title-first"', 'aria-labelledby="chart-desc-first"'),
    input.replace('<title id="chart-title-first">', '<title id="arbitrary-host-id">'),
    input.replace('</title>', '</title><title id="chart-title-first">duplicate</title>'),
    input.replace('<title id="chart-title-first">', '<title id="chart-title-first" onclick="bad()">'),
    input.replace('<desc id="chart-desc-first">Two accounts</desc>', '<rect id="chart-desc-first"/>'),
    input.replace('aria-labelledby="chart-title-first"', 'aria-labelledby="chart-title-first chart-title-outside"'),
    input.replace('<title id="chart-title-first">', '<g><title id="chart-title-first">').replace('</title>', '</title></g>'),
  ]) assert.equal(sanitizeSvg(bad, new DOMParser(), new XMLSerializer()).ok, false, bad)
})


test('consumer accepts only inert locally resolved Sankey gradients', () => {
  const body = '<defs><linearGradient id="native-gradient-0" gradientUnits="userSpaceOnUse" x1="10" y1="0" x2="590" y2="0"><stop offset="0" stop-color="#4e79a7"/><stop offset="1" stop-color="#f28e2c"/></linearGradient></defs><polygon points="10,0 590,0 590,10" fill="url(#native-gradient-0)" fill-opacity="0.5"/>'
  const good = sanitize(body)
  assert.equal(good.ok, true, good.error)
  for (const bad of [
    body.replace('url(#native-gradient-0)', 'url(https://example.com/a.svg#x)'),
    body.replace('url(#native-gradient-0)', 'url(#outside-gradient-0)'),
    body.replace('<stop offset="0"', '<stop onclick="alert(1)" offset="0"'),
    body.replace('gradientUnits="userSpaceOnUse"', 'href="#other"'),
    body.replace('</defs>', '<linearGradient id="native-gradient-0"/></defs>'),
    body.replace('<defs>', '<g><defs>').replace('</defs>', '</defs></g>'),
    body.replace('<stop offset="0"', '<animate offset="0"'),
    body.replace('stop-color="#4e79a7"', 'stop-color="url(#native-gradient-0)"'),
  ]) assert.equal(sanitize(bad).ok, false, bad)
})
