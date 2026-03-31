import { AlertTriangle, Copy, ChevronLeft, ChevronRight } from 'lucide-react';
import LessonXpBadge from './LessonXpBadge';
import LessonHintSection from './LessonHintSection';
import LessonRating from './LessonRating';
import QuizPanel from './QuizPanel';
import { tiptapToHtml, applyMathToHtml } from '@/lib/tiptap-renderer';

export default function LessonContentPanel({ item, onComplete, isCompleted, currentProblem, problemIndex, totalProblems, onPrevProblem, onNextProblem }) {
  const isCoding = item?.itemType === 'CODING_SET';
  const isQuiz = item?.itemType === 'QUIZ_SET' || item?.itemType === 'TEST_BLOCK';
  const isProject = item?.itemType === 'PROJECT';

  // For coding/project items, use currentProblem (or contentJson) as the source
  const codingSource = currentProblem ?? item?.contentJson ?? null;
  const hints = isCoding ? (codingSource?.hints ?? []) : (item?.contentJson?.hints ?? []);
  const expectedOutput = isCoding ? (codingSource?.expectedOutput ?? null) : (item?.contentJson?.expectedOutput ?? null);

  // PROJECT: description lives in contentJson.description
  // CODING: new format wraps the doc in { description: {...}, ... }; legacy: bare Tiptap doc
  // Other: contentJson is the doc itself
  const tiptapDoc = (isCoding || isProject)
    ? (codingSource?.type === 'doc' ? codingSource : codingSource?.description)
    : item?.contentJson;
  const tiptapHtml = applyMathToHtml(tiptapToHtml(tiptapDoc));
  // Use || not ?? so that empty string ("") from an empty Tiptap doc falls back to item.description
  const description = tiptapHtml || item?.description || null;

  const requirements = isProject ? (item?.contentJson?.requirements ?? []) : [];

  if (process.env.NODE_ENV === 'development') {
    // console.log('[LessonContentPanel] item.contentJson:', item?.contentJson);
    // console.log('[LessonContentPanel] tiptapDoc:', tiptapDoc, '| tiptapHtml:', tiptapHtml, '| description:', description);
  }

  if (isQuiz) {
    return (
      <div className='px-8 py-8'>
        <LessonXpBadge points={item?.points ?? 0} />
        <h1 className='mt-4 text-2xl font-bold text-white'>{item?.title ?? 'Quiz'}</h1>
        <div className='mt-6'>
          <QuizPanel
            contentJson={item?.contentJson}
            onComplete={onComplete}
            isCompleted={isCompleted}
          />
        </div>
        <LessonRating />
        <div className='h-16' />
      </div>
    );
  }

  return (
    <div className='px-8 py-8'>
      {/* XP badge */}
      <LessonXpBadge points={item?.points ?? 0} />

      {/* Title */}
      <h1 className='mt-4 text-2xl font-bold text-white'>
        {item?.title ?? 'Lesson Content'}
      </h1>

      {/* Multi-problem navigation */}
      {totalProblems > 1 && (
        <div className='mt-4 flex items-center gap-3'>
          <button
            onClick={onPrevProblem}
            disabled={problemIndex === 0}
            className='p-1.5 rounded-md bg-[#1e1e32] text-[#9090a8] hover:text-white disabled:opacity-30 transition-colors'
          >
            <ChevronLeft size={16} />
          </button>
          <span className='text-sm text-[#9090a8]'>
            Problem <span className='text-white font-semibold'>{problemIndex + 1}</span> / {totalProblems}
            {currentProblem?.title && (
              <span className='ml-2 text-[#7070a0]'>— {currentProblem.title}</span>
            )}
          </span>
          <button
            onClick={onNextProblem}
            disabled={problemIndex === totalProblems - 1}
            className='p-1.5 rounded-md bg-[#1e1e32] text-[#9090a8] hover:text-white disabled:opacity-30 transition-colors'
          >
            <ChevronRight size={16} />
          </button>
        </div>
      )}

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

      {/* Requirements (project only) */}
      {isProject && requirements.length > 0 && (
        <div className='mt-5 space-y-2'>
          <p className='text-xs font-black uppercase tracking-widest text-[#5a5a72]'>Requirements</p>
          <ul className='space-y-1.5'>
            {requirements.map((req, i) => (
              <li key={i} className='flex items-start gap-2 text-sm text-[#b0b0c8]'>
                <span className='mt-0.5 shrink-0 w-4 h-4 rounded-full border border-[#3a3a52] flex items-center justify-center text-[10px] text-[#5a5a72] font-bold'>
                  {i + 1}
                </span>
                {req}
              </li>
            ))}
          </ul>
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
