/**
 * Shared fail-closed SVG sanitizer for Mermaid Native web consumers and gallery.
 *
 * Enforces strict tag allowlists, attribute allowlists, duplicate attribute detection,
 * per-attribute value grammar, and document structure validation.
 * Rejects doctypes, processing instructions (e.g. xml-stylesheet), external references,
 * CSS escapes, backslashes, controls, event handlers, and URL/resource schemes.
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
 * Validates an instantiated DOM Document / Element tree against strict allowlists and document structure.
 * Rejects DocumentType, ProcessingInstruction, and non-whitelisted node types.
 */
export function validateSvgDomTree(docOrElement) {
  let doc = null
  let rootElement = null

  if (docOrElement && docOrElement.nodeType === 9) { // DOCUMENT_NODE
    doc = docOrElement
    rootElement = doc.documentElement
  } else if (docOrElement && docOrElement.nodeType === 1) { // ELEMENT_NODE
    rootElement = docOrElement
    doc = rootElement.ownerDocument || null
  } else {
    return { ok: false, error: 'Invalid document or element node' }
  }

  // 1. If we have Document context, validate top-level nodes (prolog / epilog)
  if (doc) {
    if (doc.doctype) {
      return { ok: false, error: 'Forbidden DocumentType declaration (DOCTYPE) in SVG' }
    }
    const topChildren = doc.childNodes || []
    const topLen = topChildren.length ?? 0
    let elementCount = 0
    for (let i = 0; i < topLen; i++) {
      const node = topChildren.item ? topChildren.item(i) : topChildren[i]
      if (node.nodeType === 10) { // DOCUMENT_TYPE_NODE
        return { ok: false, error: 'Forbidden DocumentType declaration in SVG document' }
      }
      if (node.nodeType === 7) { // PROCESSING_INSTRUCTION_NODE
        return { ok: false, error: `Forbidden ProcessingInstruction <${node.nodeName}> in SVG document` }
      }
      if (node.nodeType === 1) { // ELEMENT_NODE
        elementCount++
        if (elementCount > 1) {
          return { ok: false, error: 'Multiple root elements in SVG document' }
        }
      } else if (node.nodeType === 3) { // TEXT_NODE
        if (node.nodeValue && node.nodeValue.trim() !== '') {
          return { ok: false, error: 'Non-whitespace text outside root SVG element' }
        }
      } else if (node.nodeType === 8) { // COMMENT_NODE
        // Comments outside root are benign
      } else {
        return { ok: false, error: `Disallowed top-level node type ${node.nodeType} in SVG document` }
      }
    }
  }

  if (!rootElement || (rootElement.localName || rootElement.nodeName || '').toLowerCase() !== 'svg') {
    return { ok: false, error: 'Root element must be <svg>' }
  }

  // 2. Validate tree structure and node types inside root element
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
      } else if (child.nodeType === 3 || child.nodeType === 8) {
        // TEXT_NODE (3) and COMMENT_NODE (8) are allowed
      } else if (child.nodeType === 7) { // PROCESSING_INSTRUCTION_NODE
        return { ok: false, error: `Forbidden ProcessingInstruction <${child.nodeName}> inside SVG element` }
      } else if (child.nodeType === 4) { // CDATA_SECTION_NODE
        return { ok: false, error: 'Forbidden CDATA section inside SVG element' }
      } else {
        return { ok: false, error: `Forbidden node type ${child.nodeType} inside SVG element` }
      }
    }
  }

  // 3. Validate tag and attribute allowlists on all elements
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
 * Universal SVG sanitizer that parses, verifies, and serializes XML via DOMParser and XMLSerializer.
 * Never falls back to unvalidated raw input strings.
 */
export function sanitizeSvg(svgString, customParser, customSerializer) {
  if (!svgString || typeof svgString !== 'string' || !svgString.trim()) {
    return { ok: false, error: 'Empty or missing SVG payload' }
  }

  let parser = customParser
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

  // Validate entire document tree (including top-level nodes, DOCTYPE, and PIs)
  const validation = validateSvgDomTree(doc)
  if (!validation.ok) {
    return validation
  }

  // Serialize strictly the validated root element using XMLSerializer or Element.outerHTML
  let serializedSvg = ''
  let serializer = customSerializer
  if (!serializer && typeof XMLSerializer !== 'undefined') {
    serializer = new XMLSerializer()
  }

  if (serializer && typeof serializer.serializeToString === 'function') {
    serializedSvg = serializer.serializeToString(doc.documentElement)
  } else if (typeof doc.documentElement.outerHTML === 'string' && doc.documentElement.outerHTML) {
    serializedSvg = doc.documentElement.outerHTML
  } else {
    return { ok: false, error: 'No XMLSerializer or outerHTML available to serialize sanitized SVG' }
  }

  return {
    ok: true,
    svg: serializedSvg,
    element: doc.documentElement
  }
}
