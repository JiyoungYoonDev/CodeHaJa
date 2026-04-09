import katex from 'katex';

/**
 * Post-processes an HTML string and renders $...$ / $$...$$ patterns with KaTeX.
 * Only matches math outside of HTML tags (excludes < and > in the pattern).
 */
function decodeMathEntities(expr) {
  return expr
    // Restore LaTeX commands corrupted by JSON escape interpretation
    // \t(ab) was \text/\times/\theta, \b(ackspace) was \begin/\beta, etc.
    .replace(/\t/g, '\\t')
    .replace(/\x08/g, '\\b')
    .replace(/\f/g, '\\f')
    .replace(/\r/g, '\\r')
    // Corrupted \neq etc: \<newline>suffix → \n+suffix (backslash was preserved, \n became newline)
    .replace(/\\\n(eq|abla|eg|ot(?:in)?|u(?![a-z])|i(?![a-z])|ewline|less|geq|leq|mid|sim|cong|parallel|vdash|subset|supset|prec|succ|atural|ormalsize|otin|ightarrow|leftarrow|Rightarrow|Leftarrow|vert|cap|cup|circ|cdot|ell|hbar)/g, '\\n$1')
    // Corrupted \neq etc without preceding backslash: <newline>suffix → \n+suffix
    .replace(/\n(eq|abla|eg|ot(?:in)?|u(?![a-z])|i(?![a-z])|ewline|less|geq|leq|mid|sim|cong|parallel|vdash|subset|supset|prec|succ|atural|ormalsize|otin|ightarrow|leftarrow|Rightarrow|Leftarrow|vert|cap|cup|circ|cdot|ell|hbar)/g, '\\n$1')
    // remaining \n = actual whitespace (e.g., line breaks inside \begin{aligned})
    .replace(/\n/g, ' ')
    // HTML entity decoding
    .replace(/&amp;gt;/g, '>')
    .replace(/&amp;lt;/g, '<')
    .replace(/&amp;/g, '&')
    .replace(/&gt;/g, '>')
    .replace(/&lt;/g, '<')
    .replace(/&quot;/g, '"');
}

export function applyMathToHtml(html) {
  if (!html) return html;

  // Pre-fix: merge multi-line $$\begin{env}...\end{env}$$ split across <p> tags
  // (old content where ContentConverter didn't handle multi-line math blocks)
  html = html.replace(
    /<p>([^<]*\$\$\s*\\begin\{(\w+)\}[^<]*)<\/p>([\s\S]*?)<p>([^<]*\\end\{\2\}\s*\$\$[^<]*)<\/p>/g,
    (_, open, env, middle, close) => {
      const lines = middle.replace(/<p>/g, '').replace(/<\/p>/g, '\n');
      return `<p>${open}\n${lines}\n${close}</p>`;
    }
  );

  // Pre-fix: merge LaTeX commands split across HTML elements by \n corruption.
  // When \neq/\nabla/etc. had \n interpreted as newline, ContentConverter split them
  // into separate elements: "$c </p>...</p><p>eq 0$).</p>" → merge back together.
  html = html.replace(
    /(\$[^$<>]{1,80})<\/p>((?:\s*<\/li>\s*<\/[uo]l>\s*)?)<p>\s*((?:eq|abla|eg|u\b|ot(?:in)?\b|i\b|less|geq|leq|mid|ewline|sim|cong|parallel|vdash|subset|supset|prec|succ)[^<]{0,80}\$[^<]{0,20})\s*<\/p>/g,
    '$1\\n$3</p>$2'
  );

  // Block math $$...$$ first — exclude < > to prevent matching across HTML tags
  let result = html.replace(/\$\$([^$<>]+?)\$\$/g, (_, expr) => {
    try { return katex.renderToString(decodeMathEntities(expr).trim(), { displayMode: true, throwOnError: false }); }
    catch { return `$$${expr}$$`; }
  });
  // Inline math $...$ — exclude < > to prevent matching across HTML tags
  // Note: \n is allowed because corrupted LaTeX (\neq → newline+eq) needs to be matched;
  // decodeMathEntities() restores \n → \\n inside the captured expression.
  result = result.replace(/\$([^$<>]+?)\$/g, (_, expr) => {
    try { return katex.renderToString(decodeMathEntities(expr).trim(), { displayMode: false, throwOnError: false }); }
    catch { return `$${expr}$`; }
  });
  return result;
}

function decodeHtmlEntities(str) {
  // Decode double-encoded first: &amp;gt; → &gt; → >
  str = str.replace(/&amp;gt;/g, '>');
  str = str.replace(/&amp;lt;/g, '<');
  str = str.replace(/&amp;amp;/g, '&');
  str = str.replace(/&amp;quot;/g, '"');
  // Then single-encoded
  str = str.replace(/&gt;/g, '>');
  str = str.replace(/&lt;/g, '<');
  str = str.replace(/&amp;/g, '&');
  str = str.replace(/&quot;/g, '"');
  str = str.replace(/&#39;/g, "'");
  str = str.replace(/&nbsp;/g, ' ');
  return str;
}

function escapeHtml(str) {
  return str
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;');
}

function applyInlineMarkdown(html) {
  // **bold** → <strong>bold</strong> (before italic to avoid conflict)
  html = html.replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>');
  // *italic* → <em>italic</em>
  html = html.replace(/\*(.+?)\*/g, '<em>$1</em>');
  // `code` → <code>code</code>
  html = html.replace(/`([^`]+)`/g, '<code>$1</code>');
  // Clean up orphan * from incomplete markdown (e.g., "every* term" → "every term")
  html = html.replace(/(\w)\*(\s)/g, '$1$2');
  html = html.replace(/(\s)\*(\w)/g, '$1$2');
  return html;
}

function renderMarks(text, marks = []) {
  let result = escapeHtml(decodeHtmlEntities(text));
  // If no Tiptap marks, apply markdown formatting from plain text
  if (marks.length === 0) {
    result = applyInlineMarkdown(result);
  }
  for (const mark of marks) {
    switch (mark.type) {
      case 'bold':      result = `<strong>${result}</strong>`; break;
      case 'italic':    result = `<em>${result}</em>`; break;
      case 'code':      result = `<code>${result}</code>`; break;
      case 'strike':    result = `<s>${result}</s>`; break;
      case 'underline': result = `<u>${result}</u>`; break;
      case 'link':      result = `<a href="${mark.attrs?.href ?? '#'}" target="_blank" rel="noopener">${result}</a>`; break;
    }
  }
  return result;
}

function renderNode(node) {
  if (!node) return '';
  const children = () => (node.content ?? []).map(renderNode).join('');

  switch (node.type) {
    case 'doc':
      return children();

    case 'paragraph': {
      const align = node.attrs?.textAlign ? ` style="text-align:${node.attrs.textAlign}"` : '';
      const inner = children();
      return inner ? `<p${align}>${inner}</p>` : '<p><br></p>';
    }

    case 'heading': {
      const level = node.attrs?.level ?? 2;
      const align = node.attrs?.textAlign ? ` style="text-align:${node.attrs.textAlign}"` : '';
      return `<h${level}${align}>${children()}</h${level}>`;
    }

    case 'text':
      return renderMarks(node.text ?? '', node.marks);

    case 'codeBlock': {
      const lang = node.attrs?.language ?? '';
      const code = escapeHtml((node.content ?? []).map(n => n.text ?? '').join(''));
      return `<pre data-language="${lang}"><code>${code}</code></pre>`;
    }

    case 'bulletList':
      return `<ul>${children()}</ul>`;

    case 'orderedList':
      return `<ol>${children()}</ol>`;

    case 'listItem':
      return `<li>${children()}</li>`;

    case 'taskList':
      return `<ul class="task-list">${children()}</ul>`;

    case 'taskItem': {
      const checked = node.attrs?.checked ? ' checked' : '';
      return `<li class="task-item"><input type="checkbox"${checked} disabled />${children()}</li>`;
    }

    case 'blockquote':
      return `<blockquote>${children()}</blockquote>`;

    case 'hardBreak':
      return '<br>';

    case 'horizontalRule':
      return '<hr>';

    case 'image': {
      const imgSrc = node.attrs?.src ?? '';
      const imgAlt = escapeHtml(node.attrs?.alt ?? '');
      const imgTitle = node.attrs?.title ? ` title="${escapeHtml(node.attrs.title)}"` : '';
      return `<img src="${imgSrc}" alt="${imgAlt}"${imgTitle} style="max-width:100%;height:auto;border-radius:8px;" loading="lazy" />`;
    }

    case 'mathBlock': {
      const rawLatex = node.attrs?.latex ?? '';
      if (!rawLatex) return '';
      const latex = decodeMathEntities(rawLatex);
      try {
        return `<div class="math-block">${katex.renderToString(latex, { displayMode: true, throwOnError: false })}</div>`;
      } catch {
        return `<div class="math-block"><code>${escapeHtml(latex)}</code></div>`;
      }
    }

    case 'checkpointBlock': {
      const cpT = node.attrs?.title ?? '';
      const cpQ = node.attrs?.question ?? '';
      const cpA = node.attrs?.answer ?? '';
      const cpAlt = node.attrs?.alternatives ?? '';
      const cpH = node.attrs?.hint ?? '';
      const cpId = node.attrs?.blockId ?? node.attrs?.id ?? '';
      // Render question with KaTeX
      let cpQuestionHtml = escapeHtml(cpQ);
      try {
        cpQuestionHtml = cpQuestionHtml
          .replace(/\$\$([^$]+)\$\$/g, (_, e) => katex.renderToString(decodeMathEntities(e).trim(), { displayMode: true, throwOnError: false }))
          .replace(/\$([^$]+)\$/g, (_, e) => katex.renderToString(decodeMathEntities(e).trim(), { displayMode: false, throwOnError: false }));
      } catch {}
      const titleHtml = cpT ? `<div class="checkpoint-title-label">${escapeHtml(cpT)}</div>` : '';
      return `<div class="checkpoint-block" data-answer="${escapeHtml(cpA)}" data-alternatives="${escapeHtml(cpAlt)}" data-hint="${escapeHtml(cpH)}" data-block-id="${escapeHtml(cpId)}"><div class="checkpoint-header"><span class="checkpoint-icon">✏️</span><span class="checkpoint-title">Your Turn</span></div>${titleHtml}<div class="checkpoint-question">${cpQuestionHtml}</div><div class="checkpoint-input-area"><input type="text" class="checkpoint-input" placeholder="답을 입력하세요..." /><button class="checkpoint-btn" type="button">Check</button></div><div class="checkpoint-hint" style="display:none"></div><div class="checkpoint-feedback" style="display:none"></div></div>`;
    }

    case 'numberLineBlock': {
      const nlExpr = node.attrs?.expression ?? '';
      const nlValue = node.attrs?.value ?? 0;
      const nlClosed = node.attrs?.closed ?? false;
      const nlRight = node.attrs?.shadeRight ?? false;
      // Render label as KaTeX
      const nlLatex = nlExpr.replace(/</g, '\\lt ').replace(/>=/g, '\\geq ').replace(/>/g, '\\gt ').replace(/<=/g, '\\leq ');
      let nlLabel;
      try { nlLabel = katex.renderToString(nlLatex, { displayMode: false, throwOnError: false }); }
      catch { nlLabel = escapeHtml(nlExpr); }
      return `<div class="number-line-block" data-value="${nlValue}" data-closed="${nlClosed}" data-shade-right="${nlRight}"><div class="number-line-label">${nlLabel}</div><div class="number-line-container"></div></div>`;
    }

    case 'graphBlock': {
      const expr = node.attrs?.expression ?? '';
      const xMin = node.attrs?.xMin ?? -10;
      const xMax = node.attrs?.xMax ?? 10;
      const yMin = node.attrs?.yMin ?? -10;
      const yMax = node.attrs?.yMax ?? 10;
      const w = node.attrs?.width ?? 600;
      const h = node.attrs?.height ?? 400;
      // Render label as KaTeX math
      const latexExpr = expr.replace(/\*/g, '').replace(/abs\(([^)]+)\)/g, '|$1|');
      let labelHtml;
      try {
        labelHtml = katex.renderToString(`f(x) = ${latexExpr}`, { displayMode: false, throwOnError: false });
      } catch {
        labelHtml = `f(x) = ${escapeHtml(expr)}`;
      }
      return `<div class="graph-block" data-expression="${escapeHtml(expr)}" data-x-min="${xMin}" data-x-max="${xMax}" data-y-min="${yMin}" data-y-max="${yMax}" data-width="${w}" data-height="${h}"><div class="graph-block-label">${labelHtml}</div><div class="graph-block-container"></div></div>`;
    }

    default:
      return children();
  }
}

/**
 * Post-process: convert <p>* text</p> or <p>- text</p> paragraphs into <ul><li>...</li></ul>.
 * Handles old content where the backend stored bullet lines as plain paragraphs.
 */
function fixBulletParagraphs(html) {
  if (!html) return html;
  return html.replace(
    /(?:<p>[*\u2022\-]\s+[\s\S]*?<\/p>\s*)+/g,
    (block) => {
      const items = [...block.matchAll(/<p>[*\u2022\-]\s+([\s\S]*?)<\/p>/g)];
      if (items.length === 0) return block;
      return '<ul>' + items.map(m => `<li><p>${m[1]}</p></li>`).join('') + '</ul>';
    }
  );
}

/**
 * Post-process: convert <p>#### text</p> headings stored as plain paragraphs.
 */
function fixMarkdownHeadings(html) {
  if (!html) return html;
  return html
    .replace(/<p>####\s+(.+?)<\/p>/g, '<h4>$1</h4>')
    .replace(/<p>###\s+(.+?)<\/p>/g, '<h3>$1</h3>')
    .replace(/<p>##\s+(.+?)<\/p>/g, '<h2>$1</h2>')
    .replace(/<p>#\s+(.+?)<\/p>/g, '<h1>$1</h1>');
}

/**
 * Post-process: convert <p>&gt; text</p> blockquotes stored as plain paragraphs.
 */
function fixBlockquotes(html) {
  if (!html) return html;
  return html.replace(
    /(?:<p>&gt;\s+[\s\S]*?<\/p>\s*)+/g,
    (block) => {
      const items = [...block.matchAll(/<p>&gt;\s+([\s\S]*?)<\/p>/g)];
      if (items.length === 0) return block;
      return '<blockquote>' + items.map(m => `<p>${m[1]}</p>`).join('') + '</blockquote>';
    }
  );
}

/**
 * Wrap each Solution section in its own collapsible accordion.
 * Each Solution's content ends at the next heading (<h1>-<h4>) or end of HTML.
 * Must run BEFORE styleLabels so Solution pattern is still raw.
 */
function wrapSolutionAccordion(html) {
  if (!html) return html;
  // Match each Solution marker + its content until the next heading or end
  return html.replace(
    /<p><strong>Solution[:\s]*<\/strong>([\s\S]*?)<\/p>([\s\S]*?)(?=<h[1234][ >]|<div class="edu-label|$)/gi,
    (_, trailingText, content) => {
      let solutionContent = content || '';
      const trimmed = (trailingText || '').trim();
      if (trimmed) {
        solutionContent = `<p>${trimmed}</p>` + solutionContent;
      }

      // Add step badges (a), b), c), d)) and dividers between steps
      let stepCount = 0;
      solutionContent = solutionContent.replace(/<p>([a-z])\)\s*/gi, () => {
        stepCount++;
        const letter = String.fromCharCode(96 + stepCount);
        return (stepCount > 1 ? '<hr class="step-divider">' : '') +
          `<div class="step-header"><span class="step-badge">${letter}</span></div><p>`;
      });

      return '<details class="solution-accordion">' +
        '<summary class="solution-trigger">' +
        '<span class="solution-icon">💡</span>' +
        '<span class="solution-text">Solution</span>' +
        '<span class="solution-hint">Try solving it yourself first!</span>' +
        '<span class="solution-chevron">▶</span>' +
        '</summary>' +
        '<div class="solution-content">' + solutionContent + '</div>' +
        '</details>';
    }
  );
}

/**
 * Wrap "Common Mistakes and Pitfalls" / "Key Takeaway" in styled sections.
 */
function wrapEducationalSections(html) {
  if (!html) return html;

  // ── Common Mistakes and Pitfalls ──
  html = html.replace(
    /<h[34][^>]*>\s*Common Mistakes and Pitfalls\s*<\/h[34]>([\s\S]*?)(?=<h[1234][ >]|<details |$)/gi,
    (_, content) => {
      let inner = content || '';

      // The AI generates each mistake as a single bullet:
      //   - **Title:** description
      //   ✗ Wrong: ...
      //   ✓ Correct: ...
      //   Why: ...
      // ContentConverter turns this into:
      //   <ul><li><p><strong>Title:</strong> desc</p></li></ul>
      //   <p>✗ Wrong: ...</p>  ← separate paragraphs OUTSIDE the <li>
      //   <p>✓ Correct: ...</p>
      //   <p>Why: ...</p>
      //
      // Strategy: match each bullet + trailing content until the next bullet or section end,
      // then wrap the whole thing into one accordion card.

      // Match: <ul><li> with <strong>Title</strong>, the closing </li></ul>,
      // then all content until the next <ul> bullet or end of section.
      // Must consume the opening <ul> to prevent unclosed tags causing nesting.
      inner = inner.replace(
        /<ul>\s*<li>(?:<p>)?\s*<strong>([\s\S]*?)<\/strong>([\s\S]*?)<\/li>\s*<\/ul>([\s\S]*?)(?=<ul>|$)/g,
        (_, title, liBody, afterList) => {
          let cardBody = (liBody || '') + (afterList || '');

          // Transform ✗ Wrong / ✓ Correct / Why markers inside this card
          cardBody = cardBody.replace(
            /<p>\s*[✗✘×Xx]\s*Wrong[:\s]*([\s\S]*?)(?=<p>\s*[✓✔]|<div class="mistake-correct"|$)/gi,
            (__, b) => '<div class="mistake-wrong"><div class="mistake-wrong-header"><span class="mistake-mark-wrong">✗</span> WRONG</div><div class="mistake-wrong-body">' + b.trim() + '</div></div>'
          );
          cardBody = cardBody.replace(
            /<p>\s*[✓✔]\s*Correct[:\s]*([\s\S]*?)(?=<p>\s*Why\b|<div class="mistake-wrong"|<div class="mistake-why"|$)/gi,
            (__, b) => '<div class="mistake-correct"><div class="mistake-correct-header"><span class="mistake-mark-correct">✓</span> CORRECT</div><div class="mistake-correct-body">' + b.trim() + '</div></div>'
          );
          cardBody = cardBody.replace(
            /<p>\s*Why[:\s]+([\s\S]*?)<\/p>/gi,
            '<div class="mistake-why"><span class="mistake-why-tag">Why?</span> $1</div>'
          );

          return '<div class="mistake-card"><details class="mistake-details">' +
            '<summary class="mistake-title"><span class="mistake-title-icon">⚠️</span> <strong>' + title + '</strong><span class="mistake-chevron">▶</span></summary>' +
            '<div class="mistake-body">' + cardBody + '</div>' +
            '</details></div>';
        }
      );

      // Fallback: transform any remaining WRONG/CORRECT/Why markers not captured above
      inner = inner.replace(
        /<p>\s*[✗✘×Xx]\s*Wrong[:\s]*([\s\S]*?)(?=<p>\s*[✓✔]|<div class="mistake-correct"|$)/gi,
        (_, body) => '<div class="mistake-wrong"><div class="mistake-wrong-header"><span class="mistake-mark-wrong">✗</span> WRONG</div><div class="mistake-wrong-body">' + body.trim() + '</div></div>'
      );
      inner = inner.replace(
        /<p>\s*[✓✔]\s*Correct[:\s]*([\s\S]*?)(?=<p>\s*Why\b|<div class="mistake-wrong"|<div class="mistake-why"|$)/gi,
        (_, body) => '<div class="mistake-correct"><div class="mistake-correct-header"><span class="mistake-mark-correct">✓</span> CORRECT</div><div class="mistake-correct-body">' + body.trim() + '</div></div>'
      );
      inner = inner.replace(
        /<p>\s*Why[:\s]+([\s\S]*?)<\/p>/gi,
        '<div class="mistake-why"><span class="mistake-why-tag">Why?</span> $1</div>'
      );

      return '<details class="mistakes-accordion" open>' +
        '<summary class="mistakes-trigger">' +
        '<span class="mistakes-icon">⚠️</span>' +
        '<span class="mistakes-text">Common Mistakes and Pitfalls</span>' +
        '<span class="mistakes-chevron">▶</span>' +
        '</summary>' +
        '<div class="mistakes-content">' + inner + '</div>' +
        '</details>';
    }
  );

  // ── Key Takeaway ──
  html = html.replace(
    /<h[34][^>]*>\s*Key Takeaway[s]?\s*<\/h[34]>([\s\S]*?)(?=<h[1234][ >]|<details |$)/gi,
    (_, content) => {
      return '<div class="key-takeaway">' +
        '<div class="key-takeaway-header">' +
        '<span class="key-takeaway-icon">🔑</span>' +
        '<span class="key-takeaway-title">Key Takeaway</span>' +
        '</div>' +
        '<div class="key-takeaway-body">' + (content || '') + '</div>' +
        '</div>';
    }
  );

  return html;
}

/**
 * Style educational labels: Problem:, Answer:, Therefore, etc.
 * Solution is handled by wrapSolutionAccordion.
 */
function styleLabels(html) {
  if (!html) return html;
  // "Problem:" / "Answer:" as styled labels (NOT Solution — handled by accordion)
  html = html.replace(
    /<p><strong>(Problem|Answer|Proof)[:\s]*<\/strong>/gi,
    '<div class="edu-label edu-label--$1"><span class="edu-label__tag">$1</span></div><p>'
  );
  // "Therefore, ..." conclusions as highlighted blockquote
  html = html.replace(
    /<p>(Therefore,[\s\S]*?)<\/p>/g,
    '<blockquote class="edu-conclusion"><p>$1</p></blockquote>'
  );
  // Raw GRAPH: lines that weren't caught by backend — hide them
  html = html.replace(/<p>[a-zA-Z0-9]*[).]?\s*GRAPH:[\s\S]*?<\/p>/g, '');
  return html;
}

/**
 * Post-process: convert consecutive <p>| ... |</p> lines (markdown tables) into <table>.
 */
function fixMarkdownTables(html) {
  if (!html) return html;
  // Match 2+ consecutive pipe-delimited paragraphs
  return html.replace(
    /(?:<p>\s*\|[\s\S]*?\|[\s\S]*?<\/p>\s*){2,}/g,
    (block) => {
      const rows = [...block.matchAll(/<p>\s*\|([\s\S]*?)\|\s*<\/p>/g)];
      if (rows.length < 2) return block;

      // Parse cells from each row
      const parsed = rows.map(m =>
        m[1].split('|').map(c => c.trim())
      );

      // Detect and skip separator row (e.g., |:---|:---|)
      const sepIdx = parsed.findIndex(cells =>
        cells.every(c => /^[:\-\s]+$/.test(c))
      );

      const headerCells = parsed[0];
      const dataRows = parsed.filter((_, i) => i !== 0 && i !== sepIdx);

      let table = '<table><thead><tr>';
      for (const cell of headerCells) {
        table += `<th>${cell}</th>`;
      }
      table += '</tr></thead><tbody>';
      for (const row of dataRows) {
        table += '<tr>';
        for (let i = 0; i < headerCells.length; i++) {
          table += `<td>${row[i] ?? ''}</td>`;
        }
        table += '</tr>';
      }
      table += '</tbody></table>';
      return table;
    }
  );
}

function postProcess(html) {
  if (!html) return html;
  let result = fixBulletParagraphs(html);
  result = fixMarkdownHeadings(result);
  result = fixBlockquotes(result);
  result = fixMarkdownTables(result);
  result = wrapSolutionAccordion(result);
  result = wrapEducationalSections(result);
  result = styleLabels(result);
  return result;
}

export function tiptapToHtml(doc) {
  if (!doc || doc.type !== 'doc') return null;
  try {
    return postProcess(renderNode(doc));
  } catch {
    return null;
  }
}
