/**
 * Shared fail-closed SVG sanitizer for Mermaid Native web consumers and gallery.
 *
 * Enforces strict tag allowlists, attribute allowlists, duplicate attribute detection,
 * and per-attribute value grammar.
 * Rejects CSS escapes, backslashes, controls, event handlers, and URL/resource schemes.
 */

export const ALLOWED_TAGS = new Set([
  'svg', 'g', 'rect', 'circle', 'ellipse', 'line', 'polyline', 'polygon', 'path', 'text', 'tspan'
])

export const ALLOWED_ATTRS = new Set([
  'xmlns', 'width', 'height', 'viewbox', 'role', 'aria-label', 'aria-hidden',
  'x', 'y', 'x1', 'y1', 'x2', 'y2', 'cx', 'cy', 'r', 'rx', 'ry', 'd', 'points',
  'fill', 'fill-opacity', 'stroke', 'stroke-width', 'stroke-dasharray',
  'stroke-linecap', 'stroke-linejoin', 'stroke-miterlimit', 'stroke-opacity', 'opacity',
  'transform', 'text-anchor', 'font-family', 'font-size', 'font-weight', 'font-style',
  'letter-spacing', 'dominant-baseline', 'alignment-baseline'
])

/**
 * Validates a single attribute value against fail-closed grammar.
 * Returns true if valid, false if rejected.
 */
export function validateAttributeValue(attrName, attrVal) {
  if (typeof attrVal !== 'string') return false

  // Reject any backslash (CSS escape or JS escape attempt e.g. \72, \u0072)
  if (attrVal.includes('\\')) return false

  // Reject ASCII control characters and DEL
  if (/[\x00-\x1f\x7f]/.test(attrVal)) return false

  // Reject URL/resource schemes and function calls
  const stripped = attrVal.toLowerCase().replace(/\s+/g, '')
  if (
    stripped.includes('url(') ||
    stripped.includes('javascript:') ||
    stripped.includes('vbscript:') ||
    stripped.includes('data:') ||
    stripped.includes('expression(')
  ) {
    return false
  }

  // Strict grammar for color attributes: only #rgb, #rrggbb, #rgba, #rrggbbaa, or 'none'
  if (attrName === 'fill' || attrName === 'stroke') {
    const trimmed = attrVal.trim()
    if (trimmed === 'none') return true
    if (/^#[0-9a-fA-F]{3,8}$/.test(trimmed)) return true
    return false
  }

  // Strict grammar for single numeric/float attributes
  if (
    attrName === 'width' || attrName === 'height' ||
    attrName === 'x' || attrName === 'y' ||
    attrName === 'x1' || attrName === 'y1' || attrName === 'x2' || attrName === 'y2' ||
    attrName === 'cx' || attrName === 'cy' || attrName === 'r' || attrName === 'rx' || attrName === 'ry' ||
    attrName === 'fill-opacity' || attrName === 'stroke-width' || attrName === 'stroke-opacity' ||
    attrName === 'opacity' || attrName === 'font-size'
  ) {
    return /^-?[0-9]+(\.[0-9]+)?$/.test(attrVal.trim())
  }

  // viewBox: 4 numbers separated by whitespace
  if (attrName === 'viewbox') {
    return /^-?[0-9]+(\.[0-9]+)?\s+-?[0-9]+(\.[0-9]+)?\s+-?[0-9]+(\.[0-9]+)?\s+-?[0-9]+(\.[0-9]+)?$/.test(attrVal.trim())
  }

  // points: pairs of x,y coordinates
  if (attrName === 'points') {
    return /^(\s*-?[0-9]+(\.[0-9]+)?,-?[0-9]+(\.[0-9]+)?)+$/.test(attrVal.trim())
  }

  // stroke-dasharray: numbers separated by whitespace
  if (attrName === 'stroke-dasharray') {
    return /^([0-9]+(\.[0-9]+)?\s*)+$/.test(attrVal.trim())
  }

  // font-weight: normal, bold, bolder, lighter, or 100-900
  if (attrName === 'font-weight') {
    return /^(normal|bold|bolder|lighter|[1-9]00)$/.test(attrVal.trim())
  }

  // text-anchor: start, middle, end
  if (attrName === 'text-anchor') {
    return /^(start|middle|end)$/.test(attrVal.trim())
  }

  // font-family: identifier / family names separated by commas
  if (attrName === 'font-family') {
    return /^[a-zA-Z0-9\s,-]+$/.test(attrVal.trim())
  }

  // role: only 'img'
  if (attrName === 'role') {
    return attrVal.trim() === 'img'
  }

  // xmlns: only standard SVG namespace
  if (attrName === 'xmlns') {
    return attrVal.trim() === 'http://www.w3.org/2000/svg'
  }

  return true
}

/**
 * Validates an instantiated DOM Document / Element tree against strict allowlists.
 * Works uniformly with browser DOM, xmldom, and jsdom.
 */
export function validateSvgDomTree(rootElement) {
  if (!rootElement || (rootElement.localName || rootElement.nodeName || '').toLowerCase() !== 'svg') {
    return { ok: false, error: 'Root element must be <svg>' }
  }

  const elements = [rootElement]
  const stack = [rootElement]
  while (stack.length > 0) {
    const el = stack.pop()
    const children = el.childNodes || []
    const len = children.length ?? 0
    for (let i = 0; i < len; i++) {
      const child = children.item ? children.item(i) : children[i]
      if (child.nodeType === 1) { // ELEMENT_NODE
        elements.push(child)
        stack.push(child)
      }
    }
  }

  for (const el of elements) {
    const tagName = (el.localName || el.nodeName || '').toLowerCase()
    if (!ALLOWED_TAGS.has(tagName)) {
      return { ok: false, error: `Forbidden element <${tagName}> in SVG output` }
    }

    const attrs = el.attributes || []
    const attrLen = attrs.length ?? 0
    const seenAttrs = new Set()
    for (let i = 0; i < attrLen; i++) {
      const attr = attrs.item ? attrs.item(i) : attrs[i]
      const attrName = (attr.name || attr.localName || '').toLowerCase()
      if (seenAttrs.has(attrName)) {
        return { ok: false, error: `Duplicate attribute "${attrName}" in <${tagName}>` }
      }
      seenAttrs.add(attrName)

      if (attrName.startsWith('on')) {
        return { ok: false, error: `Forbidden event handler attribute "${attrName}" in SVG output` }
      }
      if (attrName === 'href' || attrName.endsWith(':href') || attrName === 'src' || attrName === 'style' || attrName === 'id' || attrName === 'class') {
        return { ok: false, error: `Forbidden attribute "${attrName}" in SVG output` }
      }
      if (!ALLOWED_ATTRS.has(attrName)) {
        return { ok: false, error: `Disallowed attribute "${attrName}" in SVG output` }
      }
      if (!validateAttributeValue(attrName, attr.value)) {
        return { ok: false, error: `Attribute "${attrName}" rejected by strict value grammar: "${attr.value}"` }
      }
    }
  }

  return { ok: true }
}

/**
 * Universal SVG sanitizer that parses and verifies XML via DOMParser (browser, xmldom, or passed customParser).
 */
export function sanitizeSvg(svgString, customParser) {
  if (!svgString || typeof svgString !== 'string' || !svgString.trim()) {
    return { ok: false, error: 'Empty or missing SVG payload' }
  }

  let parser = customParser
  let parseError = null

  if (!parser) {
    if (typeof DOMParser !== 'undefined') {
      parser = new DOMParser()
    } else {
      return { ok: false, error: 'DOMParser is not available in current environment' }
    }
  }

  let doc = null
  try {
    doc = parser.parseFromString(svgString, 'image/svg+xml')
    if (doc.querySelector && doc.querySelector('parsererror')) {
      return { ok: false, error: 'XML parser error in SVG payload' }
    }
  } catch (err) {
    return { ok: false, error: `XML parse exception: ${err.message}` }
  }

  if (!doc || !doc.documentElement) {
    return { ok: false, error: 'Failed to construct XML DOM document' }
  }

  const validation = validateSvgDomTree(doc.documentElement)
  if (!validation.ok) {
    return validation
  }

  return {
    ok: true,
    svg: doc.documentElement.outerHTML || svgString.trim(),
    element: doc.documentElement
  }
}
