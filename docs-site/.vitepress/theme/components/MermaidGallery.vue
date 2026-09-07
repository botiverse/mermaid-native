<script setup lang="ts">
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import { useData } from 'vitepress'
import familyExamples from '../data/family-examples.json'

const { site } = useData()
const base = site.value.base || '/'

const defaultSource = `flowchart TD
  A[Write Mermaid] ==> B[Render SVG]
  B --> C[Share permalink]`

const editorSource = ref(defaultSource)
const editorPreviewHtml = ref('')
const editorError = ref('')
const editorStatus = ref('')
const wasmStatus = ref<'loading' | 'ready' | 'offline'>('loading')
const filterQuery = ref('')
const copiedSlug = ref('')

const filteredExamples = computed(() => {
  const q = filterQuery.value.trim().toLowerCase()
  if (!q) return familyExamples
  return familyExamples.filter((item: any) =>
    item.family.toLowerCase().includes(q) ||
    item.slug.toLowerCase().includes(q) ||
    item.source.toLowerCase().includes(q) ||
    item.note.toLowerCase().includes(q)
  )
})

const ALLOWED_TAGS = new Set([
  'svg',
  'g',
  'rect',
  'circle',
  'ellipse',
  'line',
  'polyline',
  'polygon',
  'path',
  'text',
  'tspan',
])

const ALLOWED_ATTRS = new Set([
  'xmlns',
  'width',
  'height',
  'viewbox',
  'role',
  'aria-label',
  'aria-hidden',
  'x',
  'y',
  'x1',
  'y1',
  'x2',
  'y2',
  'cx',
  'cy',
  'r',
  'rx',
  'ry',
  'd',
  'points',
  'fill',
  'fill-opacity',
  'stroke',
  'stroke-width',
  'stroke-dasharray',
  'stroke-linecap',
  'stroke-linejoin',
  'stroke-miterlimit',
  'stroke-opacity',
  'opacity',
  'transform',
  'text-anchor',
  'font-family',
  'font-size',
  'font-weight',
  'font-style',
  'letter-spacing',
  'dominant-baseline',
  'alignment-baseline',
  'class',
  'id',
])

function safeSvg(payload: any): { ok: true; svg: string } | { ok: false; error: string } {
  if (!payload?.svg || typeof payload.svg !== 'string') {
    return { ok: false, error: 'Empty or missing SVG payload' }
  }
  const parsed = new DOMParser().parseFromString(payload.svg, 'image/svg+xml')
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
      if (attrName === 'href' || attrName.endsWith(':href') || attrName === 'src' || attrName === 'style') {
        return { ok: false, error: `Forbidden attribute "${attr.name}" in SVG output` }
      }
      if (!ALLOWED_ATTRS.has(attrName)) {
        return { ok: false, error: `Forbidden attribute "${attr.name}" in SVG output` }
      }
      const val = attr.value.toLowerCase().replace(/[\s\x00-\x1f]+/g, '')
      if (
        val.includes('javascript:') ||
        val.includes('vbscript:') ||
        val.includes('data:text') ||
        val.includes('data:image') ||
        val.includes('url(')
      ) {
        return { ok: false, error: `Potentially unsafe script/URL scheme in attribute "${attr.name}"` }
      }
    }
  }
  return { ok: true, svg: root.outerHTML }
}

function decodeSource(): string | null {
  if (typeof window === 'undefined') return null
  const match = location.hash.match(/^#source=([^&]+)/)
  if (!match) return null
  try {
    return decodeURIComponent(escape(atob(match[1].replace(/-/g, '+').replace(/_/g, '/'))))
  } catch {
    return null
  }
}

let debounceTimer: ReturnType<typeof setTimeout> | null = null

watch(editorSource, () => {
  if (debounceTimer) {
    clearTimeout(debounceTimer)
  }
  debounceTimer = setTimeout(() => {
    if (wasmStatus.value === 'ready') {
      renderEditor()
    }
  }, 300)
})

function renderEditor() {
  if (debounceTimer) {
    clearTimeout(debounceTimer)
    debounceTimer = null
  }
  if (typeof window === 'undefined') return
  const rt = (window as any).mermaidNative
  if (!rt?.renderMermaidResultJson) {
    if (wasmStatus.value !== 'offline') {
      editorStatus.value = 'Wasm engine unavailable'
    }
    return
  }
  try {
    const payload = JSON.parse(rt.renderMermaidResultJson(editorSource.value))
    if (payload.ok && payload.svg) {
      const sanitized = safeSvg(payload)
      if (sanitized.ok) {
        editorPreviewHtml.value = sanitized.svg
        editorError.value = ''
        editorStatus.value = 'Rendered successfully'
      } else {
        editorPreviewHtml.value = ''
        editorError.value = `SVG rejected by security policy: ${sanitized.error}`
        editorStatus.value = 'Render rejected (security policy)'
      }
    } else {
      editorPreviewHtml.value = ''
      const details = payload.diagnostics?.map((d: any) =>
        `${d.code}: ${d.message} (line ${d.line ?? '?'}, column ${d.column ?? '?'})`
      ).join('\n') || 'Render rejected'
      editorError.value = details
      editorStatus.value = 'Render failed (typed diagnostics)'
    }
  } catch (err: any) {
    editorPreviewHtml.value = ''
    editorError.value = String(err?.message || err)
    editorStatus.value = 'Render exception'
  }
}

async function copySource(source: string, slug?: string) {
  if (navigator?.clipboard?.writeText) {
    await navigator.clipboard.writeText(source)
    if (slug) {
      copiedSlug.value = slug
      setTimeout(() => {
        if (copiedSlug.value === slug) copiedSlug.value = ''
      }, 2000)
    } else {
      editorStatus.value = 'Source copied to clipboard'
    }
  }
}

async function copyPermalink() {
  if (typeof window === 'undefined') return
  const bytes = unescape(encodeURIComponent(editorSource.value || ''))
  const encoded = btoa(bytes).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '')
  const permalink = `${location.origin}${location.pathname}#source=${encoded}`
  if (navigator?.clipboard?.writeText) {
    await navigator.clipboard.writeText(permalink)
    history.replaceState(null, '', `#source=${encoded}`)
    editorStatus.value = 'Permalink copied to clipboard'
  }
}

function tryInEditor(card: any) {
  editorSource.value = card.source
  if (wasmStatus.value === 'ready') {
    renderEditor()
  } else {
    editorPreviewHtml.value = card.svg
    editorError.value = ''
    editorStatus.value = `Loaded ${card.family} example`
  }
  const el = document.getElementById('playground-editor')
  if (el) {
    el.scrollIntoView({ behavior: 'smooth', block: 'center' })
    el.focus()
  }
}

function initWasm() {
  if (typeof window === 'undefined') return

  const globalExport = (window as any)['mermaid-web']
  if (globalExport) {
    if (globalExport.then) {
      globalExport
        .then((exports: any) => {
          ;(window as any).mermaidNative = exports
          wasmStatus.value = 'ready'
          renderEditor()
        })
        .catch(() => {
          wasmStatus.value = 'offline'
        })
    } else {
      ;(window as any).mermaidNative = globalExport
      wasmStatus.value = 'ready'
      renderEditor()
    }
    return
  }

  wasmStatus.value = 'loading'
  const basePath = base.endsWith('/') ? base : base + '/'
  const primaryUrl = `${basePath}wasm/mermaid-web.js`

  const script = document.createElement('script')
  script.src = primaryUrl
  script.async = true
  script.onload = () => {
    const loaded = (window as any)['mermaid-web']
    if (loaded?.then) {
      loaded
        .then((exports: any) => {
          ;(window as any).mermaidNative = exports
          wasmStatus.value = 'ready'
          renderEditor()
        })
        .catch(() => {
          wasmStatus.value = 'offline'
        })
    } else if (loaded) {
      ;(window as any).mermaidNative = loaded
      wasmStatus.value = 'ready'
      renderEditor()
    } else {
      wasmStatus.value = 'offline'
    }
  }

  script.onerror = () => {
    // Fallback: check playground directory if wasm was not copied to /wasm/
    const fallbackScript = document.createElement('script')
    fallbackScript.src = `${basePath}playground/mermaid-web.js`
    fallbackScript.async = true
    fallbackScript.onload = () => {
      const loaded = (window as any)['mermaid-web']
      if (loaded?.then) {
        loaded
          .then((exports: any) => {
            ;(window as any).mermaidNative = exports
            wasmStatus.value = 'ready'
            renderEditor()
          })
          .catch(() => {
            wasmStatus.value = 'offline'
          })
      } else if (loaded) {
        ;(window as any).mermaidNative = loaded
        wasmStatus.value = 'ready'
        renderEditor()
      } else {
        wasmStatus.value = 'offline'
      }
    }
    fallbackScript.onerror = () => {
      wasmStatus.value = 'offline'
    }
    document.head.appendChild(fallbackScript)
  }

  document.head.appendChild(script)
}

onMounted(() => {
  // Preload initial preview from flowchart thick-arrow sample
  const initialFlowchart = familyExamples.find((e: any) => e.family === 'Flowchart')
  const permalinkSource = decodeSource()
  if (permalinkSource) {
    editorSource.value = permalinkSource
  } else if (initialFlowchart) {
    editorPreviewHtml.value = initialFlowchart.svg
  }

  initWasm()
})

onUnmounted(() => {
  if (debounceTimer) {
    clearTimeout(debounceTimer)
    debounceTimer = null
  }
})
</script>

<template>
  <div class="mermaid-gallery-container">
    <!-- Interactive Live Playground -->
    <section class="playground-card" aria-labelledby="playground-heading">
      <div class="playground-header">
        <div>
          <h2 id="playground-heading" class="playground-title">Interactive Playground</h2>
          <p class="playground-subtitle">
            Type or edit Mermaid source code below to render live using the Kotlin/Wasm engine.
          </p>
        </div>
        <div class="engine-badge" :class="wasmStatus">
          <span class="badge-dot"></span>
          <span v-if="wasmStatus === 'ready'">Kotlin/Wasm engine ready</span>
          <span v-else-if="wasmStatus === 'loading'">Initializing Wasm engine...</span>
          <span v-else>Wasm offline (static previews active)</span>
        </div>
      </div>

      <div class="playground-grid">
        <div class="editor-pane">
          <label for="playground-editor" class="pane-label">Mermaid Source</label>
          <textarea
            id="playground-editor"
            v-model="editorSource"
            class="code-editor"
            rows="7"
            aria-label="Mermaid source code editor"
            placeholder="Enter Mermaid diagram syntax..."
          ></textarea>
          <div class="editor-toolbar">
            <button
              type="button"
              class="action-button primary"
              @click="renderEditor"
            >
              Render
            </button>
            <button
              type="button"
              class="action-button secondary"
              @click="copySource(editorSource)"
            >
              Copy Source
            </button>
            <button
              type="button"
              class="action-button secondary"
              @click="copyPermalink"
            >
              Copy Permalink
            </button>
            <span
              id="editor-status"
              class="status-text"
              role="status"
              aria-live="polite"
            >
              {{ editorStatus }}
            </span>
          </div>
        </div>

        <div class="preview-pane">
          <div class="pane-label">Rendered Preview</div>
          <div
            id="editor-preview"
            class="editor-preview-surface"
            aria-live="polite"
          >
            <div
              v-if="editorPreviewHtml"
              class="rendered-svg-wrap"
              v-html="editorPreviewHtml"
            ></div>
            <div v-else-if="editorError" class="render-error">
              <p class="error-title">Diagnostic error:</p>
              <pre>{{ editorError }}</pre>
            </div>
            <div v-else class="preview-placeholder">
              Click <strong>Render</strong> to preview output.
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- Toolbar for Gallery -->
    <div class="gallery-toolbar" role="search">
      <div class="search-input-wrap">
        <svg class="search-icon" viewBox="0 0 20 20" fill="currentColor">
          <path
            fill-rule="evenodd"
            d="M8 4a4 4 0 100 8 4 4 0 000-8zM2 8a6 6 0 1110.89 3.476l4.817 4.817a1 1 0 01-1.414 1.414l-4.816-4.816A6 6 0 012 8z"
            clip-rule="evenodd"
          />
        </svg>
        <input
          v-model="filterQuery"
          type="search"
          class="gallery-filter-input"
          placeholder="Filter 32 diagram families (e.g. flowchart, sequence, git, gantt)..."
          aria-label="Filter diagram families"
          autocomplete="off"
        />
        <button
          v-if="filterQuery"
          type="button"
          class="clear-filter-btn"
          aria-label="Clear filter"
          @click="filterQuery = ''"
        >
          ✕
        </button>
      </div>
      <div class="gallery-counter">
        Showing {{ filteredExamples.length }} of {{ familyExamples.length }} diagram families
      </div>
    </div>

    <!-- 32 Families Gallery -->
    <section class="gallery-list" aria-label="Mermaid diagram families gallery">
      <article
        v-for="card in filteredExamples"
        :id="card.slug"
        :key="card.slug"
        class="diagram-card"
      >
        <div class="card-header">
          <div class="card-title-group">
            <span class="family-eyebrow">{{ card.family }}</span>
            <h3 class="card-title">
              <a :href="`#${card.slug}`" class="header-anchor">#</a>
              {{ card.family }}
            </h3>
            <p class="card-note">{{ card.note }}</p>
          </div>
          <div class="card-actions">
            <button
              type="button"
              class="card-btn try-btn"
              @click="tryInEditor(card)"
            >
              Try in editor
            </button>
            <button
              type="button"
              class="card-btn copy-btn"
              @click="copySource(card.source, card.slug)"
            >
              {{ copiedSlug === card.slug ? 'Copied!' : 'Copy source' }}
            </button>
          </div>
        </div>

        <div class="card-content-grid">
          <div class="card-code-col">
            <div class="code-badge">Mermaid</div>
            <pre class="card-source"><code>{{ card.source }}</code></pre>
          </div>
          <div class="card-preview-col">
            <div
              class="diagram-preview-canvas"
              :aria-label="`Rendered ${card.family} diagram`"
              v-html="card.svg"
            ></div>
          </div>
        </div>
      </article>

      <div v-if="filteredExamples.length === 0" class="no-results-notice">
        <p>No diagram families match <strong>"{{ filterQuery }}"</strong>.</p>
        <button type="button" class="action-button secondary" @click="filterQuery = ''">
          Reset filter
        </button>
      </div>
    </section>
  </div>
</template>

<style scoped>
.mermaid-gallery-container {
  margin: 2rem 0;
  display: flex;
  flex-direction: column;
  gap: 2.5rem;
}

/* Playground Card */
.playground-card {
  border: 1px solid var(--vp-c-divider);
  border-radius: 12px;
  background: var(--vp-c-bg-soft);
  padding: 1.5rem;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.04);
}

.playground-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  flex-wrap: wrap;
  gap: 1rem;
  margin-bottom: 1.25rem;
}

.playground-title {
  margin: 0;
  font-size: 1.35rem;
  font-weight: 700;
  letter-spacing: -0.02em;
  color: var(--vp-c-text-1);
}

.playground-subtitle {
  margin: 0.25rem 0 0;
  font-size: 0.9rem;
  color: var(--vp-c-text-2);
}

.engine-badge {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.35rem 0.75rem;
  border-radius: 9999px;
  font-size: 0.75rem;
  font-weight: 600;
  border: 1px solid transparent;
}

.engine-badge .badge-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

.engine-badge.ready {
  background: rgba(16, 185, 129, 0.1);
  color: #10b981;
  border-color: rgba(16, 185, 129, 0.2);
}
.engine-badge.ready .badge-dot {
  background: #10b981;
}

.engine-badge.loading {
  background: rgba(245, 158, 11, 0.1);
  color: #f59e0b;
  border-color: rgba(245, 158, 11, 0.2);
}
.engine-badge.loading .badge-dot {
  background: #f59e0b;
  animation: pulse 1.5s infinite;
}

.engine-badge.offline {
  background: var(--vp-c-bg-elv);
  color: var(--vp-c-text-3);
  border-color: var(--vp-c-divider);
}
.engine-badge.offline .badge-dot {
  background: var(--vp-c-text-3);
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.3; }
}

.playground-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1.25rem;
}

@media (max-width: 860px) {
  .playground-grid {
    grid-template-columns: 1fr;
  }
}

.pane-label {
  display: block;
  font-size: 0.8rem;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  color: var(--vp-c-text-2);
  margin-bottom: 0.5rem;
}

.code-editor {
  width: 100%;
  min-height: 160px;
  font-family: var(--vp-font-family-mono);
  font-size: 0.875rem;
  line-height: 1.5;
  padding: 0.75rem;
  border-radius: 8px;
  border: 1px solid var(--vp-c-divider);
  background: var(--vp-c-bg);
  color: var(--vp-c-text-1);
  box-sizing: border-box;
  resize: vertical;
}

.code-editor:focus {
  outline: none;
  border-color: var(--vp-c-brand-1);
}

.editor-toolbar {
  display: flex;
  align-items: center;
  gap: 0.6rem;
  flex-wrap: wrap;
  margin-top: 0.75rem;
}

.action-button {
  padding: 0.4rem 0.85rem;
  border-radius: 6px;
  font-size: 0.85rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.15s ease;
  border: 1px solid transparent;
}

.action-button.primary {
  background: var(--vp-c-brand-1);
  color: #fff;
}
.action-button.primary:hover {
  background: var(--vp-c-brand-2);
}

.action-button.secondary {
  background: var(--vp-c-bg-elv);
  color: var(--vp-c-text-1);
  border-color: var(--vp-c-divider);
}
.action-button.secondary:hover {
  border-color: var(--vp-c-brand-1);
  color: var(--vp-c-brand-1);
}

.status-text {
  font-size: 0.8rem;
  color: var(--vp-c-text-2);
  margin-left: auto;
}

.editor-preview-surface {
  min-height: 215px;
  border: 1px solid var(--vp-c-divider);
  border-radius: 8px;
  background: #ffffff; /* keep diagram canvas white for sharp SVG contrast */
  padding: 1rem;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: auto;
  box-sizing: border-box;
}

.rendered-svg-wrap {
  width: 100%;
  display: flex;
  justify-content: center;
  overflow: auto;
}

:deep(.rendered-svg-wrap svg) {
  max-width: 100%;
  height: auto;
  display: block;
}

.render-error {
  width: 100%;
  padding: 0.75rem;
  background: rgba(239, 68, 68, 0.08);
  border-left: 3px solid #ef4444;
  border-radius: 4px;
}

.render-error .error-title {
  margin: 0 0 0.25rem;
  font-weight: 700;
  color: #dc2626;
  font-size: 0.85rem;
}

.render-error pre {
  margin: 0;
  font-family: var(--vp-font-family-mono);
  font-size: 0.8rem;
  color: #b91c1c;
  white-space: pre-wrap;
}

.preview-placeholder {
  color: #64748b;
  font-size: 0.9rem;
}

/* Gallery Toolbar */
.gallery-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 1rem;
  padding: 0.75rem 0;
  border-bottom: 1px solid var(--vp-c-divider);
}

.search-input-wrap {
  position: relative;
  flex: 1;
  max-width: 480px;
  display: flex;
  align-items: center;
}

.search-icon {
  position: absolute;
  left: 0.75rem;
  width: 18px;
  height: 18px;
  color: var(--vp-c-text-3);
  pointer-events: none;
}

.gallery-filter-input {
  width: 100%;
  padding: 0.5rem 2rem 0.5rem 2.25rem;
  border-radius: 8px;
  border: 1px solid var(--vp-c-divider);
  background: var(--vp-c-bg-soft);
  color: var(--vp-c-text-1);
  font-size: 0.9rem;
}

.gallery-filter-input:focus {
  outline: none;
  border-color: var(--vp-c-brand-1);
}

.clear-filter-btn {
  position: absolute;
  right: 0.5rem;
  background: none;
  border: none;
  color: var(--vp-c-text-3);
  cursor: pointer;
  font-size: 0.85rem;
  padding: 0.25rem;
}

.gallery-counter {
  font-size: 0.85rem;
  font-weight: 600;
  color: var(--vp-c-text-2);
}

/* Gallery Cards */
.gallery-list {
  display: flex;
  flex-direction: column;
  gap: 2rem;
}

.diagram-card {
  border: 1px solid var(--vp-c-divider);
  border-radius: 12px;
  background: var(--vp-c-bg);
  padding: 1.5rem;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.03);
  transition: border-color 0.2s ease;
}

.diagram-card:hover {
  border-color: var(--vp-c-brand-soft);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 1rem;
  flex-wrap: wrap;
  margin-bottom: 1.25rem;
}

.family-eyebrow {
  display: block;
  font-size: 0.75rem;
  text-transform: uppercase;
  letter-spacing: 0.08em;
  font-weight: 800;
  color: var(--vp-c-brand-1);
  margin-bottom: 0.2rem;
}

.card-title {
  margin: 0;
  font-size: 1.35rem;
  font-weight: 700;
  letter-spacing: -0.02em;
  color: var(--vp-c-text-1);
  display: flex;
  align-items: center;
  gap: 0.35rem;
}

.header-anchor {
  opacity: 0;
  color: var(--vp-c-brand-1);
  text-decoration: none;
  font-weight: 400;
  transition: opacity 0.15s ease;
}

.card-title:hover .header-anchor {
  opacity: 1;
}

.card-note {
  margin: 0.35rem 0 0;
  font-size: 0.875rem;
  color: var(--vp-c-text-2);
  max-width: 680px;
}

.card-actions {
  display: flex;
  gap: 0.5rem;
  align-items: center;
}

.card-btn {
  padding: 0.35rem 0.75rem;
  border-radius: 6px;
  font-size: 0.8rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.15s ease;
  border: 1px solid var(--vp-c-divider);
  background: var(--vp-c-bg-soft);
  color: var(--vp-c-text-1);
}

.card-btn.try-btn {
  background: var(--vp-c-brand-soft);
  color: var(--vp-c-brand-1);
  border-color: transparent;
}
.card-btn.try-btn:hover {
  background: var(--vp-c-brand-1);
  color: #ffffff;
}

.card-btn.copy-btn:hover {
  border-color: var(--vp-c-brand-1);
  color: var(--vp-c-brand-1);
}

.card-content-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1.25rem;
  align-items: stretch;
}

@media (max-width: 860px) {
  .card-content-grid {
    grid-template-columns: 1fr;
  }
}

.card-code-col {
  display: flex;
  flex-direction: column;
}

.code-badge {
  font-size: 0.7rem;
  font-weight: 700;
  text-transform: uppercase;
  color: var(--vp-c-text-3);
  letter-spacing: 0.05em;
  margin-bottom: 0.35rem;
}

.card-source {
  margin: 0;
  padding: 1rem;
  border-radius: 8px;
  background: var(--vp-c-bg-soft);
  border: 1px solid var(--vp-c-divider);
  font-family: var(--vp-font-family-mono);
  font-size: 0.825rem;
  line-height: 1.5;
  color: var(--vp-c-text-1);
  overflow-x: auto;
  flex: 1;
  box-sizing: border-box;
}

.card-preview-col {
  display: flex;
  flex-direction: column;
}

.diagram-preview-canvas {
  flex: 1;
  border: 1px solid var(--vp-c-divider);
  border-radius: 8px;
  background: #ffffff; /* keep diagram canvas white for clean contrast */
  padding: 1.25rem;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: auto;
  box-sizing: border-box;
  min-height: 200px;
}

:deep(.diagram-preview-canvas svg) {
  max-width: 100%;
  height: auto;
  display: block;
}

.no-results-notice {
  text-align: center;
  padding: 3rem 1rem;
  border: 1px dashed var(--vp-c-divider);
  border-radius: 12px;
  color: var(--vp-c-text-2);
}
</style>
