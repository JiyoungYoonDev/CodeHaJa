/**
 * Shared graph rendering utilities used by LessonContentPanel and CheckpointStepper.
 */

export function normalizeMathExpression(expr) {
  let result = expr;
  result = result.replace(/\|([^|]+)\|/g, 'abs($1)');
  result = result.replace(/\bln\s*\(/g, 'log(');
  result = result.replace(/√\(([^)]+)\)/g, 'sqrt($1)');
  result = result.replace(/√(\w+)/g, 'sqrt($1)');
  result = result.replace(/π/g, 'PI');
  return result;
}

function toJsExpression(expr) {
  let result = normalizeMathExpression(expr).replace(/\^/g, '**');
  result = result.replace(/(\d)([a-zA-Z(])/g, '$1*$2');
  result = result.replace(/([a-zA-Z)])(\d)/g, '$1*$2');
  result = result.replace(/(\))([a-zA-Z(])/g, '$1*$2');
  result = result.replace(/([a-zA-Z])(\()/g, '$1*$2');
  return result;
}

export function computeYRange(expr, xMin, xMax) {
  try {
    const jsExpr = toJsExpression(expr);
    const fn = new Function('x', `with(Math){return (${jsExpr})}`);
    const steps = 200;
    const dx = (xMax - xMin) / steps;
    let yLo = Infinity, yHi = -Infinity;
    for (let i = 0; i <= steps; i++) {
      const x = xMin + i * dx;
      const y = fn(x);
      if (Number.isFinite(y)) {
        if (y < yLo) yLo = y;
        if (y > yHi) yHi = y;
      }
    }
    if (!Number.isFinite(yLo) || !Number.isFinite(yHi)) return null;
    const pad = Math.max((yHi - yLo) * 0.1, 1);
    return [yLo - pad, yHi + pad];
  } catch {
    return null;
  }
}

export function renderNumberLine(container, value, closed, shadeRight) {
  const width = Math.min(container.offsetWidth || 500, 500);
  const height = 60;
  const pad = 40;
  const lineY = 30;
  const radius = 7;

  const step = 1;
  const tickCount = 5;
  const minVal = value - tickCount;
  const maxVal = value + tickCount;
  const usable = width - pad * 2;
  const toX = (v) => pad + ((v - minVal) / (maxVal - minVal)) * usable;

  let svg = `<svg xmlns="http://www.w3.org/2000/svg" width="${width}" height="${height}" viewBox="0 0 ${width} ${height}">`;

  const valX = toX(value);
  if (shadeRight) {
    svg += `<defs><linearGradient id="shade-r"><stop offset="0%" stop-color="#7c3aed" stop-opacity="0.35"/><stop offset="100%" stop-color="#7c3aed" stop-opacity="0.05"/></linearGradient></defs>`;
    svg += `<rect x="${valX}" y="${lineY - 12}" width="${width - pad - valX}" height="24" fill="url(#shade-r)" rx="4"/>`;
  } else {
    svg += `<defs><linearGradient id="shade-l"><stop offset="0%" stop-color="#7c3aed" stop-opacity="0.05"/><stop offset="100%" stop-color="#7c3aed" stop-opacity="0.35"/></linearGradient></defs>`;
    svg += `<rect x="${pad}" y="${lineY - 12}" width="${valX - pad}" height="24" fill="url(#shade-l)" rx="4"/>`;
  }

  svg += `<line x1="${pad - 10}" y1="${lineY}" x2="${width - pad + 10}" y2="${lineY}" stroke="#5a5a72" stroke-width="1.5"/>`;
  svg += `<polyline points="${pad - 10},${lineY} ${pad - 3},${lineY - 4} ${pad - 3},${lineY + 4}" fill="#5a5a72" stroke="none"/>`;
  svg += `<polyline points="${width - pad + 10},${lineY} ${width - pad + 3},${lineY - 4} ${width - pad + 3},${lineY + 4}" fill="#5a5a72" stroke="none"/>`;

  for (let v = minVal; v <= maxVal; v += step) {
    const x = toX(v);
    const isTarget = Math.abs(v - value) < 0.001;
    const tickH = isTarget ? 8 : 5;
    const color = isTarget ? '#a78bfa' : '#5a5a72';
    svg += `<line x1="${x}" y1="${lineY - tickH}" x2="${x}" y2="${lineY + tickH}" stroke="${color}" stroke-width="${isTarget ? 2 : 1}"/>`;
    svg += `<text x="${x}" y="${lineY + tickH + 14}" text-anchor="middle" fill="${isTarget ? '#c4b5fd' : '#6a6a82'}" font-size="${isTarget ? 13 : 11}" font-family="ui-monospace,monospace" font-weight="${isTarget ? 600 : 400}">${v}</text>`;
  }

  if (closed) {
    svg += `<circle cx="${valX}" cy="${lineY}" r="${radius}" fill="#a78bfa" stroke="#c4b5fd" stroke-width="2"/>`;
  } else {
    svg += `<circle cx="${valX}" cy="${lineY}" r="${radius}" fill="#12121f" stroke="#a78bfa" stroke-width="2.5"/>`;
  }

  const arrowY = lineY;
  if (shadeRight) {
    const ax = Math.min(valX + 40, width - pad - 5);
    svg += `<line x1="${valX + radius + 4}" y1="${arrowY}" x2="${ax}" y2="${arrowY}" stroke="#a78bfa" stroke-width="2.5" stroke-linecap="round"/>`;
    svg += `<polyline points="${ax - 6},${arrowY - 5} ${ax},${arrowY} ${ax - 6},${arrowY + 5}" fill="none" stroke="#a78bfa" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"/>`;
  } else {
    const ax = Math.max(valX - 40, pad + 5);
    svg += `<line x1="${valX - radius - 4}" y1="${arrowY}" x2="${ax}" y2="${arrowY}" stroke="#a78bfa" stroke-width="2.5" stroke-linecap="round"/>`;
    svg += `<polyline points="${ax + 6},${arrowY - 5} ${ax},${arrowY} ${ax + 6},${arrowY + 5}" fill="none" stroke="#a78bfa" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"/>`;
  }

  svg += '</svg>';
  container.innerHTML = svg;
}

/**
 * Hydrate all .graph-block elements inside a container with function-plot graphs.
 * @param {HTMLElement} containerEl - DOM element to search for .graph-block elements
 * @param {Function} functionPlot - the function-plot library default export
 */
export function hydrateGraphBlocks(containerEl, functionPlot) {
  if (!containerEl) return;
  const graphs = containerEl.querySelectorAll('.graph-block');
  if (graphs.length === 0) return;

  graphs.forEach((graphEl) => {
    const container = graphEl.querySelector('.graph-block-container');
    if (!container) return;
    if (graphEl.offsetParent === null) return;
    if (container.querySelector('svg') || container.querySelector('canvas')) return;
    const expr = graphEl.dataset.expression;
    if (!expr) return;

    const cleanExpr = expr.replace(/\([^)]*[A-Za-z]{4,}[^)]*\)/g, '').replace(/\[.*?\]/g, '').trim();
    const ineqMatch = cleanExpr.match(/^x\s*(<=|>=|<|>)\s*(-?\d+(?:\.\d+)?)\s*$|^(-?\d+(?:\.\d+)?)\s*(<=|>=|<|>)\s*x\s*$/);
    if (ineqMatch) {
      let op, val;
      if (ineqMatch[1]) { op = ineqMatch[1]; val = Number(ineqMatch[2]); }
      else { val = Number(ineqMatch[3]); op = ({ '<': '>', '>': '<', '<=': '>=', '>=': '<=' })[ineqMatch[4]]; }
      renderNumberLine(container, val, op.includes('='), op.startsWith('>'));
      return;
    }
    if (/=/.test(expr) || /[<>]/.test(expr) || /\([A-Za-z]+\s+[A-Za-z]/.test(expr) || /\by\b/.test(expr)) {
      container.innerHTML = `<p style="color:#9090a8;font-size:13px;text-align:center;padding:12px 0;font-style:italic">This equation cannot be plotted as y = f(x)</p>`;
      return;
    }
    try {
      container.innerHTML = '';
      const xMin = Number(graphEl.dataset.xMin) || -10;
      const xMax = Number(graphEl.dataset.xMax) || 10;
      const autoY = computeYRange(expr, xMin, xMax);
      let plotExpr = normalizeMathExpression(expr);
      plotExpr = plotExpr.replace(/(\d)([a-zA-Z(])/g, '$1*$2');
      plotExpr = plotExpr.replace(/(\))([a-zA-Z\d(])/g, '$1*$2');
      functionPlot({
        target: container,
        width: Math.min(Number(graphEl.dataset.width) || 600, container.offsetWidth || 600),
        height: Number(graphEl.dataset.height) || 400,
        xAxis: { domain: [xMin, xMax] },
        yAxis: { domain: autoY || [-10, 10] },
        grid: true,
        data: [{ fn: plotExpr, sampler: 'builtIn', graphType: 'polyline' }],
      });
    } catch (err) {
      container.innerHTML = `<p style="color:#5a5a72;font-size:12px;text-align:center;padding:8px">Graph unavailable</p>`;
    }
  });
}
