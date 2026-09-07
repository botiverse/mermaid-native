/**
 * Shared fail-closed SVG sanitizer for Mermaid Native web consumers and gallery.
 *
 * Enforces strict tag allowlists, attribute allowlists, and per-attribute value grammar.
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
 * Parses and sanitizes SVG using standard DOMParser in browser environments,
 * or structural XML verification in non-DOM environments.
 */
export function sanitizeSvg(svgString) {
  if (!svgString || typeof svgString !== 'string') {
    return { ok: false, error: 'Empty or missing SVG payload' }
  }

  // If DOMParser is available (browser / jsdom)
  if (typeof DOMParser !== 'undefined') {
    const parsed = new DOMParser().parseFromString(svgString, 'image/svg+xml')
    if (parsed.querySelector('parsererror')) {
      return { ok: false, error: 'Malformed XML in SVG payload' }
    }
    const root = parsed.documentElement
    if (!root || root.localName.toLowerCase() !== 'svg') {
      return { ok: false, error: 'Root element must be <svg>' }
    }

    const allElements = [root, ...Array.from(root.querySelectorAll('*'))]
    for (const node of allElements) {
      const tagName = node.localName.toLowerCase()
      if (!ALLOWED_TAGS.has(tagName)) {
        return { ok: false, error: `Forbidden element <${tagName}> in SVG output` }
      }
      for (const attr of Array.from(node.attributes)) {
        const attrName = attr.name.toLowerCase()
        if (attrName.startsWith('on')) {
          return { ok: false, error: `Forbidden event handler attribute "${attr.name}" in SVG output` }
        }
        if (attrName === 'href' || attrName.endsWith(':href') || attrName === 'src' || attrName === 'style' || attrName === 'id' || attrName === 'class') {
          return { ok: false, error: `Forbidden attribute "${attr.name}" in SVG output` }
        }
        if (!ALLOWED_ATTRS.has(attrName)) {
          return { ok: false, error: `Disallowed attribute "${attr.name}" in SVG output` }
        }
        if (!validateAttributeValue(attrName, attr.value)) {
          return { ok: false, error: `Attribute "${attr.name}" rejected by strict value grammar: "${attr.value}"` }
        }
      }
    }
    return { ok: true, svg: root.outerHTML, element: root }
  }

  // Pure textual/regex verification for Node.js build-time / test environments
  return sanitizeSvgText(svgString)
}

/**
 * Textual / regex structural sanitizer for Node environments without DOM.
 */
export function sanitizeSvgText(svgString) {
  if (!svgString || typeof svgString !== 'string' || !svgString.trim()) {
    return { ok: false, error: 'Empty SVG payload' }
  }

  // Validate root tag is <svg ...>
  const trimmed = svgString.trim()
  if (!/^<svg\b/i.test(trimmed)) {
    return { ok: false, error: 'SVG payload must start with <svg>' }
  }

  // Check all opening / self-closing tags
  const tagMatches = [...svgString.matchAll(/<([a-zA-Z0-9:-]+)/g)]
    .map(m => m[1].toLowerCase())
    .filter(t => !t.startsWith('?') && !t.startsWith('/'))

  for (const tag of tagMatches) {
    if (!ALLOWED_TAGS.has(tag)) {
      return { ok: false, error: `Forbidden element <${tag}> in SVG output` }
    }
  }

  // Check all attributes
  const attrMatches = [...svgString.matchAll(/\s+([a-zA-Z0-9:-]+)=["']([^"']*)["']/g)]
  for (const match of attrMatches) {
    const attrName = match[1].toLowerCase()
    const attrVal = match[2]

    if (attrName.startsWith('on')) {
      return { ok: false, error: `Forbidden event handler attribute "${attrName}"` }
    }
    if (attrName === 'href' || attrName.endsWith(':href') || attrName === 'src' || attrName === 'style' || attrName === 'id' || attrName === 'class') {
      return { ok: false, error: `Forbidden attribute "${attrName}" in SVG output` }
    }
    if (!ALLOWED_ATTRS.has(attrName)) {
      return { ok: false, error: `Disallowed attribute "${attrName}" in SVG output` }
    }
    if (!validateAttributeValue(attrName, attrVal)) {
      return { ok: false, error: `Attribute "${attrName}" rejected by strict value grammar: "${attrVal}"` }
    }
  }

  return { ok: true, svg: trimmed }
}
