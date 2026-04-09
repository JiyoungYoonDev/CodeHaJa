import { useCallback, useEffect, useRef } from 'react';
import { AlertTriangle, Copy, ChevronLeft, ChevronRight } from 'lucide-react';
import LessonXpBadge from './LessonXpBadge';
import LessonHintSection from './LessonHintSection';
import LessonRating from './LessonRating';
import QuizPanel from './QuizPanel';
import SectionedContent from './SectionedContent';
import CheckpointStepper from './CheckpointStepper';
import { tiptapToHtml, applyMathToHtml } from '@/lib/tiptap-renderer';
import { apiFetch } from '@/lib/api-client';
import { renderNumberLine, hydrateGraphBlocks } from './graph-utils';

/** Normalise answer for comparison: strip whitespace, LaTeX commands, unicode math symbols */
function normaliseAnswer(str) {
  return str
    .replace(/\\frac\{([^}]*)\}\{([^}]*)\}/g, '($1)/($2)')  // \frac{a}{b} → (a)/(b)
    .replace(/\\sqrt\{([^}]*)\}/g, 'sqrt($1)')                // \sqrt{a} → sqrt(a)
    .replace(/\\(?:cdot|times)/g, '*')                         // \cdot \times → *
    .replace(/\\leq?\b/g, '<=')                                // \le \leq → <=
    .replace(/\\geq?\b/g, '>=')                                // \ge \geq → >=
    .replace(/\\neq?\b/g, '!=')                                // \ne \neq → !=
    .replace(/\\pi\b/g, 'pi')
    .replace(/\\infty\b/g, 'inf')
    .replace(/\\left|\\right/g, '')
    .replace(/[{}\\]/g, '')
    .replace(/\u2212/g, '-')                                   // unicode minus
    .replace(/\u00b2/g, '^2')                                  // ²
    .replace(/\u00b3/g, '^3')                                  // ³
    .replace(/\s+/g, '')
    .toLowerCase();
}

let _mathLivePromise = null;
function ensureMathLive() {
  if (!_mathLivePromise) {
    _mathLivePromise = import('mathlive').then((mod) => {
      // MathLive registers <math-field> web component on import
      return mod;
    });
  }
  return _mathLivePromise;
}

function useCheckpointBlocks(contentRef, lectureItemId, deps, onWrongAnswer) {
  useEffect(() => {
    const el = contentRef.current;
    if (!el) return;
    let cancelled = false;

    // Phase 1: Upgrade inputs to MathLive math-field
    // Phase 2: Restore previous submissions AFTER math-fields exist
    ensureMathLive().then(() => {
      if (cancelled) return;

      // ── Phase 1: Initialize checkpoint blocks ──
      el.querySelectorAll('.checkpoint-block').forEach((block) => {
        if (block.dataset.init) return;
        block.dataset.init = 'true';

        const answer = block.dataset.answer || '';
        const alternatives = block.dataset.alternatives || '';
        const hint = block.dataset.hint || '';
        const blockId = block.dataset.blockId || '';
        const oldInput = block.querySelector('.checkpoint-input');
        const btn = block.querySelector('.checkpoint-btn');
        const feedbackEl = block.querySelector('.checkpoint-feedback');
        const hintEl = block.querySelector('.checkpoint-hint');

        // Build list of all accepted answers (main + alternatives)
        const acceptedAnswers = [answer, ...alternatives.split(',').map(s => s.trim())].filter(Boolean);

        // Replace <input> with <math-field>
        const mf = document.createElement('math-field');
        mf.className = 'checkpoint-mathfield';
        mf.mathVirtualKeyboardPolicy = 'auto';
        if (oldInput) oldInput.replaceWith(mf);

        let hintShown = false;
        let submitted = false;

        function check() {
          if (submitted) return;
          const userAnswer = (mf.value || '').trim();
          if (!userAnswer) return;
          const normUser = normaliseAnswer(userAnswer);
          const correct = acceptedAnswers.some(a => normaliseAnswer(a) === normUser);
          submitted = true;
          mf.readOnly = true;
          btn.disabled = true;
          feedbackEl.style.display = '';
          if (correct) {
            feedbackEl.className = 'checkpoint-feedback checkpoint-correct';
            feedbackEl.textContent = '✓ 정답입니다!';
            btn.textContent = '✓';
            btn.className = 'checkpoint-btn checkpoint-btn-correct';
          } else {
            feedbackEl.className = 'checkpoint-feedback checkpoint-wrong';
            feedbackEl.innerHTML = `✗ 오답입니다. 정답: <strong>${answer}</strong>`;
            btn.textContent = '✗';
            btn.className = 'checkpoint-btn checkpoint-btn-wrong';
            const retryBtn = document.createElement('button');
            retryBtn.type = 'button';
            retryBtn.className = 'checkpoint-retry';
            retryBtn.textContent = '다시 풀기';
            retryBtn.onclick = () => {
              submitted = false;
              mf.readOnly = false;
              btn.disabled = false;
              mf.value = '';
              btn.textContent = 'Check';
              btn.className = 'checkpoint-btn';
              feedbackEl.style.display = 'none';
              retryBtn.remove();
              mf.focus();
            };
            feedbackEl.appendChild(retryBtn);
          }

          if (lectureItemId && blockId) {
            apiFetch(`/api/checkpoint-submissions/${lectureItemId}`, {
              method: 'POST',
              body: JSON.stringify({ blockId, userAnswer, correctAnswer: answer, correct }),
            }).then((res) => {
              const data = res?.data;
              if (data?.currentHearts != null && !correct && onWrongAnswer) {
                onWrongAnswer(data.currentHearts);
              }
            }).catch((e) => {
              if (e?.status === 422 && onWrongAnswer) {
                onWrongAnswer('empty');
              }
            });
          }
        }

        btn.onclick = check;
        mf.addEventListener('keydown', (e) => { if (e.key === 'Enter') check(); });

        // Hint button
        if (hint) {
          const hintBtn = document.createElement('button');
          hintBtn.type = 'button';
          hintBtn.className = 'checkpoint-hint-btn';
          hintBtn.textContent = '💡 힌트 보기';
          hintBtn.onclick = () => {
            if (hintShown) return;
            hintShown = true;
            hintEl.style.display = '';
            hintEl.textContent = '💡 ' + hint;
            hintBtn.style.display = 'none';
          };
          block.querySelector('.checkpoint-input-area').appendChild(hintBtn);
        }

        // Expose a way to mark this block as submitted from outside the closure
        block._markSubmitted = () => { submitted = true; };
      });

      // ── Phase 2: Restore previous submissions (math-fields now exist) ──
      if (lectureItemId) {
        apiFetch(`/api/checkpoint-submissions/item/${lectureItemId}`)
          .then((res) => {
            if (cancelled) return;
            const subs = res?.data?.submissions;
            if (!Array.isArray(subs) || subs.length === 0) return;
            const latest = {};
            for (const s of subs) {
              if (!latest[s.blockId]) latest[s.blockId] = s;
            }
            el.querySelectorAll('.checkpoint-block').forEach((block) => {
              const prev = latest[block.dataset.blockId];
              if (!prev) return;
              const mf = block.querySelector('math-field');
              const input = mf || block.querySelector('.checkpoint-input');
              const btn = block.querySelector('.checkpoint-btn');
              const feedbackEl = block.querySelector('.checkpoint-feedback');
              if (!input || !btn || !feedbackEl) return;
              if (mf) { mf.value = prev.userAnswer || ''; mf.readOnly = true; }
              else { input.value = prev.userAnswer || ''; input.disabled = true; }
              btn.disabled = true;
              if (block._markSubmitted) block._markSubmitted();
              feedbackEl.style.display = '';
              if (prev.correct) {
                feedbackEl.className = 'checkpoint-feedback checkpoint-correct';
                feedbackEl.textContent = '✓ 정답입니다!';
                btn.textContent = '✓';
                btn.className = 'checkpoint-btn checkpoint-btn-correct';
              } else {
                feedbackEl.className = 'checkpoint-feedback checkpoint-wrong';
                feedbackEl.innerHTML = `✗ 오답입니다. 정답: <strong>${prev.correctAnswer}</strong>`;
                btn.textContent = '✗';
                btn.className = 'checkpoint-btn checkpoint-btn-wrong';
              }
            });
          })
          .catch(() => {});
      }
    });

    return () => { cancelled = true; };
  }, deps);
}

function useNumberLineBlocks(contentRef, deps) {
  useEffect(() => {
    const el = contentRef.current;
    if (!el) return;
    el.querySelectorAll('.number-line-block').forEach((block) => {
      const container = block.querySelector('.number-line-container');
      if (!container) return;
      const value = Number(block.dataset.value) || 0;
      const closed = block.dataset.closed === 'true';
      const shadeRight = block.dataset.shadeRight === 'true';
      renderNumberLine(container, value, closed, shadeRight);
    });
  }, deps);
}

function useGraphBlocks(contentRef, deps) {
  const fpRef = useRef(null);

  const renderVisibleGraphs = useCallback(() => {
    const el = contentRef.current;
    if (!el) return;
    if (fpRef.current) {
      hydrateGraphBlocks(el, fpRef.current);
    } else {
      import('function-plot').then(({ default: fp }) => {
        fpRef.current = fp;
        hydrateGraphBlocks(el, fp);
      });
    }
  }, []);

  useEffect(() => { renderVisibleGraphs(); }, deps);

  return renderVisibleGraphs;
}

export default function LessonContentPanel({ item, onComplete, isCompleted, currentProblem, problemIndex, totalProblems, onPrevProblem, onNextProblem, previousQuizSubmission, onSaveQuizProgress, onWrongAnswer }) {
  const contentRef = useRef(null);
  const renderGraphs = useGraphBlocks(contentRef, [item?.id, problemIndex]);
  useNumberLineBlocks(contentRef, [item?.id, problemIndex]);
  useCheckpointBlocks(contentRef, item?.id, [item?.id, problemIndex], onWrongAnswer);
  const isCoding = item?.itemType === 'CODING_SET';
  const isQuiz = item?.itemType === 'QUIZ_SET' || item?.itemType === 'TEST_BLOCK';
  const isProject = item?.itemType === 'PROJECT';
  const isCheckpoint = item?.itemType === 'CHECKPOINT';
  const isRichText = item?.itemType === 'RICH_TEXT';

  // CHECKPOINT with blocks format from CMS editor
  const checkpointBlocks = isCheckpoint && Array.isArray(item?.contentJson?.blocks)
    ? item.contentJson.blocks : null;

  // For coding/project items, use currentProblem (or contentJson) as the source
  const codingSource = currentProblem ?? item?.contentJson ?? null;
  const hints = isCoding ? (codingSource?.hints ?? []) : (item?.contentJson?.hints ?? []);
  const expectedOutput = isCoding ? (codingSource?.expectedOutput ?? null) : (item?.contentJson?.expectedOutput ?? null);

  // PROJECT: description lives in contentJson.description
  // CODING: new format wraps the doc in { description: {...}, ... }; legacy: bare Tiptap doc
  // CHECKPOINT with blocks: handled separately via checkpointBlocks
  // Other: contentJson is the doc itself
  const tiptapDoc = (isCoding || isProject)
    ? (codingSource?.type === 'doc' ? codingSource : codingSource?.description)
    : checkpointBlocks ? null : item?.contentJson;
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
            previousSubmission={previousQuizSubmission}
            itemId={item?.id}
            onSaveProgress={onSaveQuizProgress}
            onWrongAnswer={onWrongAnswer}
          />
        </div>
        <LessonRating lectureItemId={item?.id} />
        <div className='h-16' />
      </div>
    );
  }

  return (
    <div className={`px-8 py-8 mx-auto w-full ${isRichText ? 'max-w-5xl' : 'max-w-3xl'}`}>
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

      {/* Description — skip when CHECKPOINT blocks exist (rendered separately) */}
      {!checkpointBlocks && description && (
        isRichText
          ? <SectionedContent
              html={tiptapHtml ? description : formatPrompt(description)}
              contentRef={contentRef}
              itemId={item?.id}
              onReveal={renderGraphs}
            />
          : <div
              ref={contentRef}
              className='mt-4 tiptap-content'
              dangerouslySetInnerHTML={{ __html: tiptapHtml ? description : formatPrompt(description) }}
            />
      )}

      {/* CMS-created CHECKPOINT items — step-by-step */}
      {checkpointBlocks && (
        <CheckpointStepper
          blocks={checkpointBlocks}
          lectureItemId={item?.id}
          onWrongAnswer={onWrongAnswer}
          onComplete={onComplete}
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
      {isCoding && <LessonHintSection hints={hints} totalHints={hints.length || 2} lectureItemId={item?.id} />}

      {/* Rating */}
      <LessonRating lectureItemId={item?.id} />

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
