/**
 * TypeScript port of acceptance/mermaid-canvas.js — replays a Mermaid Native canvas
 * draw-script (from the Wasm `renderMermaidCanvasJson`) onto a CanvasRenderingContext2D.
 * Kept dependency-free so the VitePress page can import it directly.
 */
export interface MermaidCanvasScript {
  width: number
  height: number
  ops: MermaidCanvasOp[]
}

export type MermaidCanvasOp =
  | { op: 'rect'; x: number; y: number; w: number; h: number; r: number; fill: string; stroke: string; sw: number }
  | { op: 'ellipse'; cx: number; cy: number; rx: number; ry: number; fill: string; fo: number; stroke: string; sw: number }
  | { op: 'line'; x1: number; y1: number; x2: number; y2: number; stroke: string; sw: number; dash: boolean }
  | { op: 'polyline'; pts: number[]; stroke: string; sw: number; dash: boolean }
  | { op: 'polygon'; pts: number[]; fill: string }
  | { op: 'text'; text: string; x: number; y: number; anchor: CanvasTextAlign; size: number; family: string; weight: number; fill: string }

export function drawMermaidCanvas(canvas: HTMLCanvasElement, script: MermaidCanvasScript): void {
  const dpr = typeof window !== 'undefined' ? window.devicePixelRatio || 1 : 1
  const width = script.width || canvas.clientWidth || 360
  const height = script.height || canvas.clientHeight || 180
  canvas.width = Math.ceil(width * dpr)
  canvas.height = Math.ceil(height * dpr)
  canvas.style.width = `${width}px`
  canvas.style.height = `${height}px`
  const ctx = canvas.getContext('2d')
  if (!ctx) return
  ctx.setTransform(dpr, 0, 0, dpr, 0, 0)
  ctx.clearRect(0, 0, width, height)

  for (const op of script.ops || []) {
    switch (op.op) {
      case 'rect':
        ctx.beginPath()
        if (op.r > 0 && typeof (ctx as any).roundRect === 'function') {
          ;(ctx as any).roundRect(op.x, op.y, op.w, op.h, op.r)
        } else {
          ctx.rect(op.x, op.y, op.w, op.h)
        }
        if (op.fill) {
          ctx.fillStyle = op.fill
          ctx.fill()
        }
        strokePath(ctx, op.stroke, op.sw, false)
        break
      case 'ellipse':
        ctx.beginPath()
        ctx.ellipse(op.cx, op.cy, op.rx, op.ry, 0, 0, Math.PI * 2)
        ctx.globalAlpha = op.fo ?? 1
        ctx.fillStyle = op.fill
        ctx.fill()
        ctx.globalAlpha = 1
        strokePath(ctx, op.stroke, op.sw, false)
        break
      case 'line':
        ctx.beginPath()
        ctx.moveTo(op.x1, op.y1)
        ctx.lineTo(op.x2, op.y2)
        strokePath(ctx, op.stroke, op.sw, op.dash)
        break
      case 'polyline':
        tracePath(ctx, op.pts, false)
        strokePath(ctx, op.stroke, op.sw, op.dash)
        break
      case 'polygon':
        tracePath(ctx, op.pts, true)
        ctx.fillStyle = op.fill
        ctx.fill()
        break
      case 'text':
        ctx.font = `${op.weight} ${op.size}px ${op.family}`
        ctx.fillStyle = op.fill
        ctx.textAlign = op.anchor
        ctx.textBaseline = 'alphabetic'
        ctx.fillText(op.text, op.x, op.y)
        break
      default:
        break
    }
  }
}

function tracePath(ctx: CanvasRenderingContext2D, pts: number[], close: boolean): void {
  ctx.beginPath()
  for (let i = 0; i + 1 < pts.length; i += 2) {
    if (i === 0) ctx.moveTo(pts[0], pts[1])
    else ctx.lineTo(pts[i], pts[i + 1])
  }
  if (close) ctx.closePath()
}

function strokePath(ctx: CanvasRenderingContext2D, stroke: string, sw: number, dash: boolean): void {
  if (!stroke || !sw) return
  ctx.strokeStyle = stroke
  ctx.lineWidth = sw
  ctx.setLineDash(dash ? [6, 4] : [])
  ctx.stroke()
  ctx.setLineDash([])
}
