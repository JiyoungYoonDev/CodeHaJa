'use client';

import { useState, useEffect, useRef, useCallback } from 'react';
import { ChevronRight, Check, RotateCcw } from 'lucide-react';
import { tiptapToHtml, applyMathToHtml } from '@/lib/tiptap-renderer';
import { apiFetch } from '@/lib/api-client';
import { hydrateGraphBlocks } from './graph-utils';

// ─── Answer normalisation (same as LessonContentPanel) ──────────────────────

function normaliseAnswer(str) {
  return str
    .replace(/\\frac\{([^}]*)\}\{([^}]*)\}/g, '($1)/($2)')
    .replace(/\\sqrt\{([^}]*)\}/g, 'sqrt($1)')
    .replace(/\\(?:cdot|times)/g, '*')
    .replace(/\\leq?\b/g, '<=')
    .replace(/\\geq?\b/g, '>=')
    .replace(/\\neq?\b/g, '!=')
    .replace(/\\pi\b/g, 'pi')
    .replace(/\\infty\b/g, 'inf')
    .replace(/\\left|\\right/g, '')
    .replace(/[{}\\]/g, '')
    .replace(/\u2212/g, '-')
    .replace(/\u00b2/g, '^2')
    .replace(/\u00b3/g, '^3')
    .replace(/\s+/g, '')
    .toLowerCase();
}

let _mathLivePromise = null;
function ensureMathLive() {
  if (!_mathLivePromise) {
    _mathLivePromise = import('mathlive');
  }
  return _mathLivePromise;
}

// ─── Parse blocks into steps ────────────────────────────────────────────────

/**
 * Group checkpoint blocks into steps.
 * Each step = optional text blocks before + checkpoint + afterTexts (solution walkthrough).
 * Text blocks AFTER a checkpoint belong to that checkpoint (not the next one).
 */
function buildSteps(blocks) {
  const steps = [];
  let pendingTexts = [];

  for (const block of blocks) {
    if (block.type === 'text') {
      if (steps.length > 0) {
        steps[steps.length - 1].afterTexts.push(block);
      } else {
        pendingTexts.push(block);
      }
    } else if (block.type === 'checkpoint') {
      // Text blocks between two checkpoints contain both:
      //   - solution walkthrough (belongs to the previous question)
      //   - introduction text (belongs to the next question)
      // Split at the midpoint: first half stays as afterTexts (walkthrough),
      // second half becomes this step's intro texts.
      if (steps.length > 0) {
        const after = steps[steps.length - 1].afterTexts;
        const splitAt = Math.ceil(after.length / 2);
        pendingTexts = after.splice(splitAt);
      }
      steps.push({ texts: pendingTexts, checkpoint: block, afterTexts: [] });
      pendingTexts = [];
    }
  }
  const trailingTexts = steps.length > 0
    ? steps[steps.length - 1].afterTexts.splice(0)
    : pendingTexts;
  return { steps, trailingTexts };
}

// ─── Single checkpoint question ─────────────────────────────────────────────

/** Detect input type from answer when inputType is not set */
function detectInputType(answer) {
  if (/\\(frac|sqrt|cdot|times|leq|geq|neq|pi|infty)/.test(answer)) return 'math';
  if (/[\^]|<=|>=/.test(answer)) return 'math';
  if (/^[xyz]\s*[<>=!]+\s*-?\d/.test(answer.trim())) return 'math';
  if (/^-?\d+(\.\d+)?(\/\d+)?$/.test(answer.trim())) return 'math';
  return 'text';
}

function CheckpointQuestion({ step, stepIndex, lectureItemId, onCorrect, onWrongAnswer, previousSubmission }) {
  const mfRef = useRef(null);
  const inputRef = useRef(null);
  const containerRef = useRef(null);
  const [submitted, setSubmitted] = useState(false);
  const [correct, setCorrect] = useState(false);
  const [userAnswer, setUserAnswer] = useState('');
  const [hintShown, setHintShown] = useState(false);
  const [mathLiveReady, setMathLiveReady] = useState(false);
  const checkRef = useRef(null);

  const cp = step.checkpoint;
  const answer = cp.answer || '';
  const alternatives = cp.alternatives || '';
  const hint = cp.hint || '';
  const blockId = cp.id || '';
  const title = cp.title || '';
  const questionHtml = applyMathToHtml(cp.question || '');
  const acceptedAnswers = [answer, ...alternatives.split(',').map(s => s.trim())].filter(Boolean);
  const inputType = cp.inputType || detectInputType(answer);
  const isMathMode = inputType === 'math';

  // Init math-field (only for math mode)
  useEffect(() => {
    if (!isMathMode) return;
    ensureMathLive().then(() => {
      setMathLiveReady(true);
    });
  }, [isMathMode]);

  // Create math-field element after ready (math mode)
  useEffect(() => {
    if (!isMathMode || !mathLiveReady || !containerRef.current) return;
    if (containerRef.current.querySelector('math-field')) return;

    const mf = document.createElement('math-field');
    mf.className = 'checkpoint-mathfield stepper-mathfield';
    mf.mathVirtualKeyboardPolicy = 'auto';
    mf.addEventListener('input', () => {
      setUserAnswer(mf.value || '');
    });
    mf.addEventListener('keydown', (e) => {
      if (e.key === 'Enter') checkRef.current?.();
    });
    containerRef.current.prepend(mf);
    mfRef.current = mf;

    if (previousSubmission) {
      mf.value = previousSubmission.userAnswer || '';
      mf.readOnly = true;
      setSubmitted(true);
      setCorrect(previousSubmission.correct);
      setUserAnswer(previousSubmission.userAnswer || '');
    } else {
      setTimeout(() => mf.focus(), 100);
    }

    return () => { mfRef.current = null; };
  }, [isMathMode, mathLiveReady, stepIndex]);

  // Text mode: restore previous submission
  useEffect(() => {
    if (isMathMode) return;
    if (previousSubmission) {
      setSubmitted(true);
      setCorrect(previousSubmission.correct);
      setUserAnswer(previousSubmission.userAnswer || '');
    } else {
      setTimeout(() => inputRef.current?.focus(), 100);
    }
  }, [isMathMode, stepIndex]);

  function handleCheck() {
    if (submitted) return;
    const val = isMathMode
      ? (mfRef.current?.value?.trim() || userAnswer.trim())
      : userAnswer.trim();
    if (!val) return;

    const normUser = normaliseAnswer(val);
    const isCorrect = acceptedAnswers.some(a => normaliseAnswer(a) === normUser);

    setSubmitted(true);
    setCorrect(isCorrect);
    if (isMathMode && mfRef.current) mfRef.current.readOnly = true;

    if (lectureItemId && blockId) {
      apiFetch(`/api/checkpoint-submissions/${lectureItemId}`, {
        method: 'POST',
        body: JSON.stringify({ blockId, userAnswer: val, correctAnswer: answer, correct: isCorrect }),
      }).then((res) => {
        const data = res?.data;
        if (data?.currentHearts != null && !isCorrect && onWrongAnswer) {
          onWrongAnswer(data.currentHearts);
        }
      }).catch((e) => {
        if (e?.status === 422 && onWrongAnswer) {
          onWrongAnswer('empty');
        }
      });
    }

    if (isCorrect) {
      onCorrect?.();
    }
  }
  checkRef.current = handleCheck;

  function handleRetry() {
    setSubmitted(false);
    setCorrect(false);
    setUserAnswer('');
    if (isMathMode && mfRef.current) {
      mfRef.current.readOnly = false;
      mfRef.current.value = '';
      setTimeout(() => mfRef.current?.focus(), 50);
    } else {
      setTimeout(() => inputRef.current?.focus(), 50);
    }
  }

  return (
    <div className='space-y-4'>
      {/* Context text blocks */}
      {step.texts.map((tb) => {
        const raw = typeof tb.content === 'string' ? tb.content : tiptapToHtml(tb.content);
        const html = applyMathToHtml(raw);
        if (!html) return null;
        return (
          <div
            key={tb.id}
            className='text-sm text-[#b0b0c8] leading-relaxed tiptap-content'
            dangerouslySetInnerHTML={{ __html: html }}
          />
        );
      })}

      {/* Question card */}
      <div className='rounded-xl border border-[#2a2a3e] bg-[#0e0e1a] overflow-hidden'>
        {/* Title */}
        {title && (
          <div className='px-4 pt-3 pb-1'>
            <p className='text-xs font-semibold text-violet-400'>{title}</p>
          </div>
        )}

        {/* Question */}
        <div
          className='px-4 py-3 text-white text-sm leading-relaxed'
          dangerouslySetInnerHTML={{ __html: questionHtml }}
        />

        {/* Input area */}
        <div className='px-4 pb-4'>
          <div ref={containerRef} className='flex items-center gap-2'>
            {/* math mode: math-field gets prepended here by useEffect */}
            {/* text mode: render a regular input */}
            {!isMathMode && (
              <input
                ref={inputRef}
                type='text'
                value={userAnswer}
                onChange={(e) => setUserAnswer(e.target.value)}
                onKeyDown={(e) => { if (e.key === 'Enter') checkRef.current?.(); }}
                disabled={submitted}
                placeholder='Type your answer...'
                className='flex-1 min-h-10 px-3 py-2 rounded-lg border border-[#2a2a3e]
                           bg-[#12121f] text-white text-sm font-mono
                           placeholder:text-[#3a3a52]
                           focus:border-violet-600 focus:outline-none focus:ring-2 focus:ring-violet-600/20
                           disabled:opacity-60 transition-colors'
              />
            )}
            {!submitted ? (
              <button
                onClick={handleCheck}
                disabled={!userAnswer.trim()}
                className='shrink-0 px-4 py-2 rounded-lg bg-violet-600 hover:bg-violet-500
                           disabled:opacity-30 disabled:cursor-not-allowed
                           text-white text-sm font-bold transition-colors'
              >
                Check
              </button>
            ) : correct ? (
              <div className='shrink-0 w-9 h-9 rounded-lg bg-emerald-600/20 border border-emerald-500/40
                              flex items-center justify-center'>
                <Check size={16} className='text-emerald-400' />
              </div>
            ) : (
              <div className='shrink-0 w-9 h-9 rounded-lg bg-rose-600/20 border border-rose-500/40
                              flex items-center justify-center text-rose-400 text-sm font-bold'>
                ✗
              </div>
            )}
          </div>

          {/* Hint */}
          {hint && !submitted && !hintShown && (
            <button
              onClick={() => setHintShown(true)}
              className='mt-2 text-xs text-[#5a5a72] hover:text-amber-400 transition-colors
                         border border-[#2a2a3e] rounded-full px-3 py-1'
            >
              💡 힌트 보기
            </button>
          )}
          {hintShown && (
            <div
              className='mt-2 text-xs text-amber-300/80 bg-amber-500/5 border border-amber-500/10
                            rounded-lg px-3 py-2'
              dangerouslySetInnerHTML={{ __html: '💡 ' + applyMathToHtml(hint) }}
            />
          )}

          {/* Feedback */}
          {submitted && (
            <div className={`mt-3 rounded-lg px-4 py-2.5 text-sm font-semibold border-l-2
              ${correct
                ? 'bg-emerald-500/10 border-emerald-500 text-emerald-400'
                : 'bg-rose-500/10 border-rose-500 text-rose-400'
              }`}
            >
              {correct ? (
                '✓ 정답입니다!'
              ) : (
                <div className='flex items-center justify-between'>
                  <span>✗ 오답입니다. 정답: <strong>{answer}</strong></span>
                  <button
                    onClick={handleRetry}
                    className='flex items-center gap-1 text-xs text-[#9090a8] hover:text-white
                               bg-[#1e1e30] rounded-lg px-2.5 py-1 transition-colors'
                  >
                    <RotateCcw size={12} />
                    다시 풀기
                  </button>
                </div>
              )}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}

// ─── Main stepper ───────────────────────────────────────────────────────────

export default function CheckpointStepper({ blocks, lectureItemId, onWrongAnswer, onComplete }) {
  const { steps, trailingTexts } = buildSteps(blocks);
  const totalSteps = steps.length;

  const [currentStep, setCurrentStep] = useState(0);
  const [solvedSteps, setSolvedSteps] = useState(new Set());
  const [previousSubs, setPreviousSubs] = useState({});
  const [loaded, setLoaded] = useState(false);
  const [resetKey, setResetKey] = useState(0);
  const [reviewMode, setReviewMode] = useState(false);
  const containerRef = useRef(null);
  const fpRef = useRef(null);

  // Hydrate .graph-block elements inside checkpoint text blocks
  useEffect(() => {
    const el = containerRef.current;
    if (!el) return;
    if (fpRef.current) {
      hydrateGraphBlocks(el, fpRef.current);
    } else {
      import('function-plot').then(({ default: fp }) => {
        fpRef.current = fp;
        hydrateGraphBlocks(el, fp);
      });
    }
  }, [currentStep, solvedSteps, reviewMode, loaded]);

  // Load previous submissions from DB
  useEffect(() => {
    if (!lectureItemId) { setLoaded(true); return; }
    apiFetch(`/api/checkpoint-submissions/item/${lectureItemId}`)
      .then((res) => {
        const subs = res?.data?.submissions;
        if (!Array.isArray(subs) || subs.length === 0) { setLoaded(true); return; }
        const latest = {};
        const solved = new Set();
        for (const s of subs) {
          if (!latest[s.blockId]) latest[s.blockId] = s;
        }
        // Find which steps are already solved
        steps.forEach((step, i) => {
          const prev = latest[step.checkpoint.id];
          if (prev?.correct) solved.add(i);
        });
        setPreviousSubs(latest);
        setSolvedSteps(solved);
        // Jump to first unsolved step
        const firstUnsolved = steps.findIndex((_, i) => !solved.has(i));
        if (firstUnsolved >= 0) {
          setCurrentStep(firstUnsolved);
        } else if (solved.size === totalSteps) {
          setCurrentStep(totalSteps - 1);
          setReviewMode(true);
        }
        setLoaded(true);
      })
      .catch(() => setLoaded(true));
  }, [lectureItemId]);

  const handleCorrect = useCallback((stepIdx) => {
    setSolvedSteps((prev) => {
      const next = new Set(prev);
      next.add(stepIdx);
      if (next.size === totalSteps) {
        onComplete?.();
        setTimeout(() => setReviewMode(true), 600);
      }
      return next;
    });
  }, [totalSteps, onComplete]);

  function goNext() {
    if (currentStep < totalSteps - 1) {
      setCurrentStep(currentStep + 1);
    }
  }

  function goToStep(idx) {
    // Allow going to solved steps or the next unsolved step
    if (solvedSteps.has(idx) || idx <= currentStep) {
      setCurrentStep(idx);
    }
  }

  function handleResetAll() {
    setSolvedSteps(new Set());
    setPreviousSubs({});
    setCurrentStep(0);
    setResetKey(k => k + 1);
    setReviewMode(false);
  }

  const allSolved = solvedSteps.size === totalSteps;

  if (!loaded) {
    return (
      <div className='text-sm text-[#5a5a72] py-8 text-center'>불러오는 중...</div>
    );
  }

  if (totalSteps === 0) {
    return <p className='text-sm text-[#5a5a72]'>체크포인트 문제가 없습니다.</p>;
  }

  // ── Review mode: all solved → show all questions + trailing texts ──
  if (reviewMode) {
    return (
      <div ref={containerRef} className='mt-6 space-y-5'>
        {/* Header */}
        <div className='flex items-center justify-between gap-4'>
          <div className='flex items-center gap-2'>
            <Check size={16} className='text-emerald-400' />
            <span className='text-sm font-bold text-white'>All Complete!</span>
            <span className='text-xs text-[#6a6a82]'>— {totalSteps} questions</span>
          </div>
          <button
            onClick={handleResetAll}
            className='flex items-center gap-1.5 text-xs text-[#6a6a82] hover:text-violet-400
                       transition-colors'
          >
            <RotateCcw size={12} />
            다시 풀기
          </button>
        </div>

        {/* All questions expanded */}
        {steps.map((step, i) => (
          <div key={`review-${lectureItemId}-${i}`} className='space-y-4'>
            <CheckpointQuestion
              step={step}
              stepIndex={i}
              lectureItemId={lectureItemId}
              onCorrect={() => {}}
              onWrongAnswer={onWrongAnswer}
              previousSubmission={previousSubs[step.checkpoint.id] || null}
            />
            {/* Solution walkthrough after each question */}
            {step.afterTexts.map((tb) => {
              const raw = typeof tb.content === 'string' ? tb.content : tiptapToHtml(tb.content);
              const html = applyMathToHtml(raw);
              if (!html) return null;
              return (
                <div
                  key={tb.id}
                  className='text-sm text-[#b0b0c8] leading-relaxed tiptap-content'
                  dangerouslySetInnerHTML={{ __html: html }}
                />
              );
            })}
          </div>
        ))}

        {/* Trailing texts: Common Mistakes, Key Takeaway, etc. */}
        {trailingTexts.map((tb) => {
          const raw = typeof tb.content === 'string' ? tb.content : tiptapToHtml(tb.content);
          const html = applyMathToHtml(raw);
          if (!html) return null;
          return (
            <div
              key={tb.id}
              className='tiptap-content text-sm text-[#b0b0c8] leading-relaxed'
              dangerouslySetInnerHTML={{ __html: html }}
            />
          );
        })}
      </div>
    );
  }

  // ── Active mode: step-by-step ──
  return (
    <div ref={containerRef} className='mt-6 space-y-5'>
      {/* Header + Progress */}
      <div className='flex items-center justify-between gap-4'>
        <div className='flex items-center gap-2'>
          <span className='text-base'>✏️</span>
          <span className='text-sm font-bold text-white'>Your Turn</span>
        </div>
        <span className='text-xs text-[#6a6a82] font-medium'>
          Question <span className='text-white font-bold'>{currentStep + 1}</span> / {totalSteps}
        </span>
      </div>

      {/* Progress dots */}
      <div className='flex items-center gap-1.5'>
        {steps.map((_, i) => {
          const isSolved = solvedSteps.has(i);
          const isCurrent = i === currentStep;
          return (
            <button
              key={i}
              onClick={() => goToStep(i)}
              className={`h-2 rounded-full transition-all duration-300 ${
                isCurrent
                  ? 'w-6 bg-violet-500'
                  : isSolved
                    ? 'w-2 bg-emerald-500 hover:bg-emerald-400 cursor-pointer'
                    : 'w-2 bg-[#2a2a3e]'
              }`}
              title={isSolved ? `Question ${i + 1} ✓` : `Question ${i + 1}`}
            />
          );
        })}
      </div>

      {/* Current question */}
      <CheckpointQuestion
        key={`${lectureItemId}-${currentStep}-${resetKey}`}
        step={steps[currentStep]}
        stepIndex={currentStep}
        lectureItemId={lectureItemId}
        onCorrect={() => handleCorrect(currentStep)}
        onWrongAnswer={onWrongAnswer}
        previousSubmission={previousSubs[steps[currentStep].checkpoint.id] || null}
      />

      {/* Solution walkthrough (shown after solving) */}
      {solvedSteps.has(currentStep) && steps[currentStep].afterTexts.map((tb) => {
        const raw = typeof tb.content === 'string' ? tb.content : tiptapToHtml(tb.content);
        const html = applyMathToHtml(raw);
        if (!html) return null;
        return (
          <div
            key={tb.id}
            className='text-sm text-[#b0b0c8] leading-relaxed tiptap-content'
            dangerouslySetInnerHTML={{ __html: html }}
          />
        );
      })}

      {/* Next button */}
      {solvedSteps.has(currentStep) && currentStep < totalSteps - 1 && (
        <div className='flex justify-center'>
          <button
            onClick={goNext}
            className='flex items-center gap-2 px-6 py-2.5 rounded-xl
                       bg-violet-600 hover:bg-violet-500
                       text-white text-sm font-semibold transition-colors'
          >
            Next Question
            <ChevronRight size={15} />
          </button>
        </div>
      )}
    </div>
  );
}
