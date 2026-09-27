<script setup lang="ts">
import { nextTick, onMounted, onUnmounted, ref, watch } from 'vue'

const props = defineProps<{
  source: string
  renderKey: string
}>()

const host = ref<HTMLElement | null>(null)
const status = ref<'loading' | 'ready' | 'error'>('loading')
const errorText = ref('')
let generation = 0
let zenumlRegistered = false

function svgId(token: number): string {
  const slug = props.renderKey.replace(/[^a-z0-9]+/gi, '-').replace(/^-+|-+$/g, '') || 'diagram'
  return `official-${slug}-${token}`
}

async function renderOfficial(): Promise<void> {
  const token = ++generation
  status.value = 'loading'
  errorText.value = ''
  await nextTick()
  if (!host.value || token !== generation) return
  host.value.innerHTML = ''
  try {
    if (import.meta.env.SSR) return
    const mermaid = (await import('mermaid')).default
    if (token !== generation) return
    if (!zenumlRegistered) {
      const zenuml = (await import('@mermaid-js/mermaid-zenuml')).default
      if (token !== generation) return
      await mermaid.registerExternalDiagrams([zenuml])
      zenumlRegistered = true
    }
    mermaid.initialize({
      startOnLoad: false,
      securityLevel: 'strict',
      theme: 'neutral',
    })
    const { svg, bindFunctions } = await mermaid.render(svgId(token), props.source)
    if (token !== generation || !host.value) return
    host.value.innerHTML = svg
    bindFunctions?.(host.value)
    const svgEl = host.value.querySelector('svg')
    if (svgEl) {
      svgEl.removeAttribute('height')
      svgEl.style.maxWidth = '100%'
      svgEl.style.height = 'auto'
      svgEl.style.display = 'block'
    }
    status.value = 'ready'
  } catch (err) {
    if (token !== generation) return
    if (host.value) host.value.innerHTML = ''
    status.value = 'error'
    errorText.value = err instanceof Error ? err.message : String(err)
  }
}

onMounted(() => {
  void renderOfficial()
})
watch(() => props.source, () => {
  void renderOfficial()
})
onUnmounted(() => {
  generation += 1
})
</script>

<template>
  <div class="official-preview">
    <div
      ref="host"
      class="official-host"
      :aria-label="`Official Mermaid render of ${renderKey}`"
    />
    <p v-if="status === 'loading'" class="official-status">Rendering official Mermaid…</p>
    <div v-else-if="status === 'error'" class="official-error">
      <p class="error-title">Official Mermaid could not render this source</p>
      <pre>{{ errorText }}</pre>
    </div>
  </div>
</template>

<style scoped>
.official-preview {
  width: 100%;
  max-width: 100%;
  min-width: 0;
}

.official-host {
  width: 100%;
  max-width: 100%;
  min-width: 0;
  overflow: auto;
}

.official-host :deep(svg) {
  max-width: 100%;
  height: auto;
  display: block;
}

.official-status {
  margin: 0.75rem 0 0;
  font-size: 0.8rem;
  color: var(--vp-c-text-3);
}

.official-error {
  margin-top: 0.75rem;
  padding: 0.75rem;
  background: rgba(239, 68, 68, 0.08);
  border-left: 3px solid #ef4444;
  border-radius: 4px;
}

.official-error .error-title {
  margin: 0 0 0.25rem;
  font-weight: 700;
  color: #dc2626;
  font-size: 0.85rem;
}

.official-error pre {
  margin: 0;
  font-family: var(--vp-font-family-mono);
  font-size: 0.8rem;
  color: #b91c1c;
  white-space: pre-wrap;
}
</style>
