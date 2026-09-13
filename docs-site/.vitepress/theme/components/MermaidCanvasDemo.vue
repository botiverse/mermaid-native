<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch } from 'vue'
import { useData } from 'vitepress'
import { drawMermaidCanvas } from '../utils/mermaid-canvas'

const { site } = useData()
const base = site.value.base || '/'

const defaultSource = `flowchart TD
  A[Write Mermaid] ==> B[Render on Canvas]
  B --> C[Zoom and pan]`

const source = ref(defaultSource)
const wasmStatus = ref<'loading' | 'ready' | 'offline'>('loading')
const status = ref('')
const canvasRef = ref<HTMLCanvasElement | null>(null)

// Zoom / pan state, applied as a CSS transform on the canvas element so the
// drawing itself stays crisp at device pixel ratio.
const scale = ref(1)
const offsetX = ref(0)
const offsetY = ref(0)

function zoomIn() {
  scale.value = Math.min(4, +(scale.value * 1.25).toFixed(3))
}
function zoomOut() {
  scale.value = Math.max(0.25, +(scale.value / 1.25).toFixed(3))
}
function resetView() {
  scale.value = 1
  offsetX.value = 0
  offsetY.value = 0
}
function zoomBy(delta: number) {
  if (delta < 0) zoomIn()
  else zoomOut()
}

let dragging = false
let dragStart = { x: 0, y: 0, ox: 0, oy: 0 }
function onPointerDown(e: PointerEvent) {
  dragging = true
  dragStart = { x: e.clientX, y: e.clientY, ox: offsetX.value, oy: offsetY.value }
  ;(e.currentTarget as HTMLElement).setPointerCapture(e.pointerId)
}
function onPointerMove(e: PointerEvent) {
  if (!dragging) return
  offsetX.value = dragStart.ox + (e.clientX - dragStart.x)
  offsetY.value = dragStart.oy + (e.clientY - dragStart.y)
}
function onPointerUp() {
  dragging = false
}

function render() {
  if (typeof window === 'undefined') return
  const rt = (window as any).mermaidNative
  const canvas = canvasRef.value
  if (!canvas) return
  if (!rt?.renderMermaidCanvasJson) {
    if (wasmStatus.value !== 'offline') status.value = 'Wasm engine unavailable'
    return
  }
  try {
    const script = JSON.parse(rt.renderMermaidCanvasJson(source.value))
    if (script.ops) {
      drawMermaidCanvas(canvas, script)
      status.value = 'Rendered on Canvas2D'
    } else {
      const details = (script.diagnostics || [])
        .map((d: any) => `${d.code}: ${d.message} (line ${d.line}, column ${d.column})`)
        .join('\n')
      const ctx = canvas.getContext('2d')
      if (ctx) ctx.clearRect(0, 0, canvas.width, canvas.height)
      status.value = 'Render failed'
      renderError.value = details || 'Render rejected'
      return
    }
    renderError.value = ''
  } catch (err: any) {
    renderError.value = String(err?.message || err)
    status.value = 'Render exception'
  }
}

const renderError = ref('')

let debounce: ReturnType<typeof setTimeout> | null = null
watch(source, () => {
  if (debounce) clearTimeout(debounce)
  debounce = setTimeout(() => {
    if (wasmStatus.value === 'ready') render()
  }, 300)
})

function initWasm() {
  if (typeof window === 'undefined') return
  const globalExport = (window as any)['mermaid-web']
  const basePath = base.endsWith('/') ? base : base + '/'
  const handle = (exports: any) => {
    ;(window as any).mermaidNative = exports
    wasmStatus.value = 'ready'
    render()
  }
  if (globalExport) {
    if (globalExport.then) globalExport.then(handle).catch(() => (wasmStatus.value = 'offline'))
    else handle(globalExport)
    return
  }
  const script = document.createElement('script')
  script.src = `${basePath}wasm/mermaid-web.js`
  script.async = true
  script.onload = () => {
    const loaded = (window as any)['mermaid-web']
    if (loaded?.then) loaded.then(handle).catch(() => (wasmStatus.value = 'offline'))
    else if (loaded) handle(loaded)
    else wasmStatus.value = 'offline'
  }
  script.onerror = () => (wasmStatus.value = 'offline')
  document.head.appendChild(script)
}

onMounted(() => initWasm())
onUnmounted(() => {
  if (debounce) clearTimeout(debounce)
})
</script>

<template>
  <div class="mermaid-canvas-demo">
    <section class="demo-card" aria-labelledby="canvas-demo-heading">
      <div class="demo-header">
        <div>
          <h2 id="canvas-demo-heading" class="demo-title">Canvas2D rendering</h2>
          <p class="demo-subtitle">
            The Kotlin/Wasm engine emits a device-independent draw-script that this page replays
            onto an HTML <code>&lt;canvas&gt;</code> — the same layout scene that drives the SVG and
            native Kuikly Canvas outputs.
          </p>
        </div>
        <div class="engine-badge" :class="wasmStatus">
          <span class="badge-dot"></span>
          <span v-if="wasmStatus === 'ready'">Kotlin/Wasm engine ready</span>
          <span v-else-if="wasmStatus === 'loading'">Initializing engine…</span>
          <span v-else>Wasm offline</span>
        </div>
      </div>

      <div class="demo-grid">
        <div class="editor-pane">
          <label for="canvas-demo-editor" class="pane-label">Mermaid Source</label>
          <textarea
            id="canvas-demo-editor"
            v-model="source"
            class="code-editor"
            rows="8"
            aria-label="Mermaid source code editor"
          ></textarea>
          <p class="status-text" role="status" aria-live="polite">{{ status }}</p>
          <pre v-if="renderError" class="render-error">{{ renderError }}</pre>
        </div>

        <div class="preview-pane">
          <div class="pane-label-row">
            <span class="pane-label">Canvas Preview</span>
            <div class="zoom-toolbar" role="toolbar" aria-label="Canvas zoom controls">
              <button type="button" class="zoom-btn" title="Zoom out" @click="zoomOut">−</button>
              <span class="zoom-level">{{ Math.round(scale * 100) }}%</span>
              <button type="button" class="zoom-btn" title="Zoom in" @click="zoomIn">+</button>
              <button type="button" class="zoom-btn zoom-reset" title="Reset view" @click="resetView">
                Reset
              </button>
            </div>
          </div>
          <div
            class="canvas-surface"
            @wheel.prevent="zoomBy($event.deltaY)"
            @pointerdown="onPointerDown"
            @pointermove="onPointerMove"
            @pointerup="onPointerUp"
            @pointercancel="onPointerUp"
          >
            <canvas
              ref="canvasRef"
              class="mermaid-canvas"
              :style="{
                transform: `translate(${offsetX}px, ${offsetY}px) scale(${scale})`,
              }"
            ></canvas>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<style scoped>
.mermaid-canvas-demo {
  margin: 2rem 0;
}
.demo-card {
  border: 1px solid var(--vp-c-divider);
  border-radius: 12px;
  background: var(--vp-c-bg-soft);
  padding: 1.5rem;
}
.demo-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  flex-wrap: wrap;
  gap: 1rem;
  margin-bottom: 1.25rem;
}
.demo-title {
  margin: 0;
  font-size: 1.35rem;
  font-weight: 700;
  color: var(--vp-c-text-1);
}
.demo-subtitle {
  margin: 0.25rem 0 0;
  font-size: 0.9rem;
  color: var(--vp-c-text-2);
  max-width: 640px;
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
}
.engine-badge.ready .badge-dot {
  background: #10b981;
}
.engine-badge.loading {
  background: rgba(245, 158, 11, 0.1);
  color: #f59e0b;
}
.engine-badge.loading .badge-dot {
  background: #f59e0b;
}
.engine-badge.offline {
  background: var(--vp-c-bg-elv);
  color: var(--vp-c-text-3);
}
.engine-badge.offline .badge-dot {
  background: var(--vp-c-text-3);
}
.demo-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1.25rem;
}
@media (max-width: 860px) {
  .demo-grid {
    grid-template-columns: 1fr;
  }
}
.pane-label,
.pane-label-row .pane-label {
  display: block;
  font-size: 0.8rem;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  color: var(--vp-c-text-2);
}
.pane-label-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 0.5rem;
}
.code-editor {
  width: 100%;
  min-height: 180px;
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
.status-text {
  font-size: 0.8rem;
  color: var(--vp-c-text-2);
  margin: 0.5rem 0 0;
}
.render-error {
  margin: 0.5rem 0 0;
  padding: 0.75rem;
  background: rgba(239, 68, 68, 0.08);
  border-left: 3px solid #ef4444;
  border-radius: 4px;
  font-size: 0.8rem;
  color: #b91c1c;
  white-space: pre-wrap;
}
.zoom-toolbar {
  display: flex;
  align-items: center;
  gap: 0.35rem;
}
.zoom-btn {
  min-width: 28px;
  height: 28px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 6px;
  border: 1px solid var(--vp-c-divider);
  background: var(--vp-c-bg-soft);
  color: var(--vp-c-text-1);
  font-size: 1rem;
  font-weight: 700;
  cursor: pointer;
  line-height: 1;
}
.zoom-btn:hover {
  border-color: var(--vp-c-brand-1);
  color: var(--vp-c-brand-1);
}
.zoom-btn.zoom-reset {
  font-size: 0.72rem;
  font-weight: 600;
  padding: 0 0.6rem;
}
.zoom-level {
  min-width: 48px;
  text-align: center;
  font-size: 0.75rem;
  font-weight: 600;
  color: var(--vp-c-text-2);
}
.canvas-surface {
  position: relative;
  overflow: hidden;
  border: 1px solid var(--vp-c-divider);
  border-radius: 8px;
  background: #ffffff;
  min-height: 260px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: grab;
}
.canvas-surface:active {
  cursor: grabbing;
}
.mermaid-canvas {
  transition: transform 0.05s linear;
  will-change: transform;
}
</style>
