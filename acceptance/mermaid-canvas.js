/**
 * Reference host-side executor for the Mermaid Native canvas export path.
 *
 * `mermaid-web` (Wasm) exposes `renderMermaidCanvasJson(source)` returning a JSON draw-script
 * `{width, height, ops:[...]}` produced from the same renderer-agnostic LayoutScene that drives
 * the SVG and native Kuikly Canvas outputs. This ~60-line layer replays it onto a
 * CanvasRenderingContext2D. Hosts own sizing, devicePixelRatio scaling and accessibility.
 *
 * Usage:
 *   const script = JSON.parse(runtime().renderMermaidCanvasJson(source));
 *   if (script.ops) drawMermaidCanvas(canvas, script);
 */
export function drawMermaidCanvas(canvas, script) {
  const dpr = window.devicePixelRatio || 1;
  const width = script.width || canvas.clientWidth || 360;
  const height = script.height || canvas.clientHeight || 180;
  canvas.width = Math.ceil(width * dpr);
  canvas.height = Math.ceil(height * dpr);
  canvas.style.width = `${width}px`;
  canvas.style.height = `${height}px`;
  const ctx = canvas.getContext('2d');
  if (!ctx) return;
  ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
  ctx.clearRect(0, 0, width, height);

  for (const op of script.ops || []) {
    switch (op.op) {
      case 'rect':
        ctx.beginPath();
        if (op.r > 0 && typeof ctx.roundRect === 'function') {
          ctx.roundRect(op.x, op.y, op.w, op.h, op.r);
        } else {
          ctx.rect(op.x, op.y, op.w, op.h);
        }
        fillStroke(ctx, op.fill, op.stroke, op.sw);
        break;
      case 'ellipse':
        ctx.beginPath();
        ctx.ellipse(op.cx, op.cy, op.rx, op.ry, 0, 0, Math.PI * 2);
        ctx.globalAlpha = op.fo ?? 1;
        ctx.fillStyle = op.fill;
        ctx.fill();
        ctx.globalAlpha = 1;
        strokePath(ctx, op.stroke, op.sw);
        break;
      case 'line':
        ctx.beginPath();
        ctx.moveTo(op.x1, op.y1);
        ctx.lineTo(op.x2, op.y2);
        strokePath(ctx, op.stroke, op.sw, op.dash);
        break;
      case 'polyline':
        tracePath(ctx, op.pts);
        strokePath(ctx, op.stroke, op.sw, op.dash);
        break;
      case 'polygon':
        tracePath(ctx, op.pts, true);
        ctx.fillStyle = op.fill;
        ctx.fill();
        break;
      case 'text':
        ctx.font = `${op.weight} ${op.size}px ${op.family}`;
        ctx.fillStyle = op.fill;
        ctx.textAlign = op.anchor;
        ctx.textBaseline = 'alphabetic';
        ctx.fillText(op.text, op.x, op.y);
        break;
      default:
        break;
    }
  }
}

function tracePath(ctx, pts, close) {
  ctx.beginPath();
  for (let i = 0; i + 1 < pts.length; i += 2) {
    if (i === 0) ctx.moveTo(pts[0], pts[1]);
    else ctx.lineTo(pts[i], pts[i + 1]);
  }
  if (close) ctx.closePath();
}

function fillStroke(ctx, fill, stroke, sw) {
  if (fill) {
    ctx.fillStyle = fill;
    ctx.fill();
  }
  strokePath(ctx, stroke, sw);
}

function strokePath(ctx, stroke, sw, dash) {
  if (!stroke || !sw) return;
  ctx.strokeStyle = stroke;
  ctx.lineWidth = sw;
  ctx.setLineDash(dash ? [6, 4] : []);
  ctx.stroke();
  ctx.setLineDash([]);
}
