import katex from 'katex';

/**
 * Post-processes an HTML string and renders $...$ / $$...$$ patterns with KaTeX.
 * Only matches math outside of HTML tags (excludes < and > in the pattern).
 */
export function applyMathToHtml(html) {
  if (!html) return html;
  // Block math $$...$$ first
  let result = html.replace(/\$\$([^$<>]+)\$\$/g, (_, expr) => {
    try { return katex.renderToString(expr.trim(), { displayMode: true, throwOnError: false }); }
    catch { return `$$${expr}$$`; }
  });
  // Inline math $...$
  result = result.replace(/\$([^$\n<>]+)\$/g, (_, expr) => {
    try { return katex.renderToString(expr.trim(), { displayMode: false, throwOnError: false }); }
    catch { return `$${expr}$`; }
  });
  return result;
}

function escapeHtml(str) {
  return str
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;');
}

function renderMarks(text, marks = []) {
  let result = escapeHtml(text);
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

    case 'image':
      return `<img src="${node.attrs?.src ?? ''}" alt="${node.attrs?.alt ?? ''}" />`;

    default:
      return children();
  }
}

export function tiptapToHtml(doc) {
  if (!doc || doc.type !== 'doc') return null;
  try {
    return renderNode(doc);
  } catch {
    return null;
  }
}
