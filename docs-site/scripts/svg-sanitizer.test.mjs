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
