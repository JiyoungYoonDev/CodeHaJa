import { AlertTriangle, Copy } from 'lucide-react';
import LessonXpBadge from './LessonXpBadge';
import LessonHintSection from './LessonHintSection';
import LessonRating from './LessonRating';
import { tiptapToHtml } from '@/lib/tiptap-renderer';

export default function LessonContentPanel({ item }) {
  const isCoding = item?.itemType === 'CODING_SET';
  const hints = item?.contentJson?.hints ?? [];
  const expectedOutput = item?.contentJson?.expectedOutput ?? null;

  // Coding items: new format wraps the doc in { description: {...}, language, starterCode, ... }
  // Legacy format: bare Tiptap doc stored directly as contentJson
  const tiptapDoc = isCoding
    ? (item?.contentJson?.type === 'doc' ? item.contentJson : item?.contentJson?.description)
    : item?.contentJson;
  const tiptapHtml = tiptapToHtml(tiptapDoc);
  // Use || not ?? so that empty string ("") from an empty Tiptap doc falls back to item.description
  const description = tiptapHtml || item?.description || null;

  if (process.env.NODE_ENV === 'development') {
    // console.log('[LessonContentPanel] item.contentJson:', item?.contentJson);
    // console.log('[LessonContentPanel] tiptapDoc:', tiptapDoc, '| tiptapHtml:', tiptapHtml, '| description:', description);
  }

  return (
    <div className='px-8 py-8'>
      {/* XP badge */}
      <LessonXpBadge points={item?.points ?? 0} />

      {/* Title */}
      <h1 className='mt-4 text-2xl font-bold text-white'>
        {item?.title ?? 'Lesson Content'}
      </h1>

      {/* Description */}
      {description && (
        <div
          className='mt-4 tiptap-content'
          dangerouslySetInnerHTML={{ __html: tiptapHtml ? description : formatPrompt(description) }}
        />
      )}

      {/* Expected output block */}
      {expectedOutput && (
        <div className='mt-5 relative'>
          <div className='bg-[#0d1117] border border-[#2a2a3e] rounded-lg p-4 pr-10'>
            <pre className='text-sm text-[#c9d1d9] font-mono whitespace-pre-wrap'>
              {expectedOutput}
            </pre>
          </div>
          <button
            className='absolute top-3 right-3 text-[#5a5a72] hover:text-[#9090a8] transition-colors'
            onClick={() => navigator.clipboard.writeText(expectedOutput)}
            title='Copy'
          >
            <Copy size={14} />
          </button>
        </div>
      )}

      {/* Auto-grading warning (coding only) */}
      {isCoding && (
        <div className='mt-5 flex gap-3 bg-[#1e1e32] border-l-2 border-amber-400/60 rounded-lg px-4 py-3'>
          <AlertTriangle size={16} className='text-amber-400 shrink-0 mt-0.5' />
          <p className='text-sm text-[#b0b0c8] leading-relaxed'>
            <strong>Auto-grading warning:</strong> This is an auto-graded assignment, so please carefully review the instructions.
            Pay attention to line breaks, punctuation, typos, and spacing!
          </p>
        </div>
      )}

      {/* Hints (coding only) */}
      {isCoding && <LessonHintSection hints={hints} totalHints={hints.length || 2} />}

      {/* Rating */}
      <LessonRating />

      {/* Related questions */}
      <div className='mt-8'>
        <h2 className='text-base font-semibold text-white'>Related Questions</h2>
        <p className='mt-2 text-sm text-[#5a5a72]'>No related questions yet.</p>
      </div>

      <div className='h-16' />
    </div>
  );
}

// Inline code formatting
function formatPrompt(text) {
  return text.replace(
    /`([^`]+)`/g,
    '<code class="bg-[#2a2a44] text-violet-300 px-1.5 py-0.5 rounded text-[13px] font-mono">$1</code>'
  );
}
