'use client';

import { useEffect, useReducer, useRef, useState } from 'react';
import katex from 'katex';
import MathKeyboard from './MathKeyboard';
import GraphDisplay from './GraphDisplay';
import { tiptapToHtml, applyMathToHtml } from '@/lib/tiptap-renderer';

// ─── KaTeX ─────────────────────────────────────────────────────────────────────

function renderMath(text) {
  if (!text) return '';
  let result = text.replace(/\$\$([^$]+)\$\$/g, (_, expr) => {
    try { return katex.renderToString(expr.trim(), { displayMode: true, throwOnError: false }); }
    catch { return `<span class="text-rose-400">${expr}</span>`; }
  });
  result = result.replace(/\$([^$\n]+)\$/g, (_, expr) => {
    try { return katex.renderToString(expr.trim(), { displayMode: false, throwOnError: false }); }
    catch { return `<span class="text-rose-400">${expr}</span>`; }
  });
  return result;
}

function MathText({ text, className = '' }) {
  if (!text) return null;
  return <span className={className} dangerouslySetInnerHTML={{ __html: renderMath(text) }} />;
}

// ─── Answer normalisation ──────────────────────────────────────────────────────

function normaliseMath(str) {
  return str.replace(/\u2212/g, '-').replace(/\u00b2/g, '^2').replace(/\s+/g, '').toLowerCase();
}

// ─── Migrate old format → blocks ──────────────────────────────────────────────

function toBlocks(contentJson) {
  if (!contentJson) return [];

  // New blocks format
  if (Array.isArray(contentJson.blocks)) return contentJson.blocks;

  // Old format: { introduction?, quizzes[] }
  if (Array.isArray(contentJson.quizzes)) {
    const blocks = [];
    if (contentJson.introduction) {
      blocks.push({ id: 'intro', type: 'text', content: contentJson.introduction });
    }
    for (const quiz of contentJson.quizzes) {
      blocks.push({ ...quiz, id: quiz.id ?? `q-${Math.random()}`, type: 'quiz' });
    }
    return blocks;
  }

  // Old single-quiz format
  if (contentJson.question !== undefined || contentJson.quizType !== undefined) {
    return [{ id: 'q0', type: 'quiz', ...contentJson }];
  }

  return [];
}

// ─── Per-quiz state management ─────────────────────────────────────────────────

function initState(quizBlocks) {
  return quizBlocks.reduce((acc, q) => {
    acc[q.id] = { selected: null, input: '', submitted: false, correct: false, hintIndex: 0, showExplanation: false, showKeyboard: false };
    return acc;
  }, {});
}

function reducer(state, action) {
  const { id } = action;
  const q = state[id] ?? {};
  switch (action.type) {
    case 'SELECT':       return { ...state, [id]: { ...q, selected: action.value } };
    case 'INPUT':        return { ...state, [id]: { ...q, input: action.value } };
    case 'SUBMIT':       return { ...state, [id]: { ...q, submitted: true, correct: action.correct } };
    case 'RETRY':        return { ...state, [id]: { ...q, selected: null, input: '', submitted: false, correct: false } };
    case 'SHOW_HINT':    return { ...state, [id]: { ...q, hintIndex: Math.min((q.hintIndex ?? 0) + 1, action.total) } };
    case 'SHOW_EXPL':    return { ...state, [id]: { ...q, showExplanation: true } };
    case 'TOGGLE_KB':    return { ...state, [id]: { ...q, showKeyboard: !(q.showKeyboard) } };
    default: return state;
  }
}

const LETTERS = ['①', '②', '③', '④', '⑤', '⑥'];

// ─── Text block renderer ───────────────────────────────────────────────────────

function TextBlock({ block }) {
  const html = applyMathToHtml(tiptapToHtml(block.content));
  if (!html) return null;
  return (
    <div
      className='text-[#c9d1d9] text-sm leading-relaxed
        [&_p]:my-1.5
        [&_h1]:text-white [&_h1]:text-xl [&_h1]:font-bold [&_h1]:mt-4 [&_h1]:mb-2
        [&_h2]:text-white [&_h2]:text-lg [&_h2]:font-bold [&_h2]:mt-3 [&_h2]:mb-1.5
        [&_h3]:text-white [&_h3]:font-semibold [&_h3]:mt-2 [&_h3]:mb-1
        [&_strong]:text-white [&_em]:italic
        [&_code]:bg-[#2a2a44] [&_code]:text-violet-300 [&_code]:px-1 [&_code]:rounded
        [&_pre]:bg-[#1a1a2e] [&_pre]:rounded-lg [&_pre]:p-3 [&_pre]:my-2 [&_pre]:overflow-x-auto
        [&_ul]:list-disc [&_ul]:pl-5 [&_ul]:space-y-0.5
        [&_ol]:list-decimal [&_ol]:pl-5 [&_ol]:space-y-0.5
        [&_blockquote]:border-l-2 [&_blockquote]:border-[#4a4a5e] [&_blockquote]:pl-3 [&_blockquote]:text-[#9090a8]'
      dangerouslySetInnerHTML={{ __html: html }}
    />
  );
}

// ─── Quiz block renderer ───────────────────────────────────────────────────────

function QuizBlock({ block, quizIndex, qState, dispatch, showSolution }) {
  const { selected, input, submitted, correct, hintIndex, showExplanation, showKeyboard } = qState;
  const hints = block.hints ?? [];
  const visibleHints = hints.slice(0, hintIndex);
  const graph = block.graph ?? null;

  const isMC    = block.quizType === 'MULTIPLE_CHOICE';
  const isMath  = block.quizType === 'MATH_INPUT';
  const canSubmit = isMC ? selected !== null : input.trim() !== '';

  const mathInputRef = useRef(null);

  function handleSubmit() {
    if (submitted) return;
    let isCorrect = false;
    if (isMC) {
      const chosen = (block.options ?? []).find((o) => o.id === selected);
      isCorrect = chosen?.isCorrect === true;
    } else if (isMath) {
      isCorrect = normaliseMath(input.trim()) === normaliseMath((block.correctAnswer ?? '').trim());
    } else {
      isCorrect = input.trim().toLowerCase() === (block.correctAnswer ?? '').trim().toLowerCase();
    }
    dispatch({ type: 'SUBMIT', id: block.id, correct: isCorrect });
  }

  return (
    <div>
      {/* Header */}
      <div className='flex items-center gap-3 mb-3'>
        <span className='text-violet-400 text-sm font-black'>Problem {quizIndex + 1}</span>
        {(block.points ?? 0) > 0 && (
          <span className='bg-violet-500/20 text-violet-300 text-[11px] font-bold px-2 py-0.5 rounded-full'>
            {block.points} XP
          </span>
        )}
      </div>

      {/* Question */}
      <p className='text-white font-bold text-base leading-relaxed mb-4'>
        <MathText text={block.question} />
      </p>

      {/* Graph */}
      {graph?.fn1 && (
        <GraphDisplay fn1={graph.fn1} fn2={graph.fn2} label1={graph.label1} label2={graph.label2} xDomain={graph.xDomain} yDomain={graph.yDomain} />
      )}

      {/* Multiple Choice */}
      {isMC && (
        <div className='space-y-2 mb-5'>
          {(block.options ?? []).map((opt, i) => {
            const isSelected = selected === opt.id;
            let cls = 'border-[#2a2a3e] text-[#c9d1d9] hover:border-[#4a4a5e]';
            if (submitted || showSolution) {
              if (opt.isCorrect)                 cls = 'border-emerald-500 bg-emerald-500/10 text-emerald-300';
              else if (isSelected && !opt.isCorrect) cls = 'border-rose-500 bg-rose-500/10 text-rose-300';
              else                               cls = 'border-[#2a2a3e] text-[#5a5a72]';
            } else if (isSelected) {
              cls = 'border-violet-500 bg-violet-500/10 text-white';
            }
            return (
              <button
                key={opt.id}
                type='button'
                disabled={submitted || showSolution}
                onClick={() => dispatch({ type: 'SELECT', id: block.id, value: opt.id })}
                className={`w-full flex items-center gap-3 px-4 py-2.5 rounded-lg border transition-colors text-left text-sm ${cls} ${(submitted || showSolution) ? 'cursor-default' : 'cursor-pointer'}`}
              >
                <span className='shrink-0 text-base'>{LETTERS[i] ?? i + 1}</span>
                <MathText text={opt.text} />
              </button>
            );
          })}
        </div>
      )}

      {/* Short Answer */}
      {!isMC && !isMath && (
        <input
          type='text'
          value={input}
          onChange={(e) => dispatch({ type: 'INPUT', id: block.id, value: e.target.value })}
          onKeyDown={(e) => e.key === 'Enter' && canSubmit && !submitted && handleSubmit()}
          disabled={submitted || showSolution}
          placeholder='답을 입력하세요...'
          className='w-full bg-[#1a1a2e] border border-[#2a2a3e] rounded-lg px-4 py-2.5 text-sm text-white placeholder-[#5a5a72] focus:outline-none focus:border-violet-500 transition-colors disabled:opacity-60 mb-5'
        />
      )}

      {/* Math Input */}
      {isMath && (
        <div className='mb-5'>
          <div className='flex items-center gap-2'>
            <input
              ref={mathInputRef}
              type='text'
              value={input}
              onChange={(e) => dispatch({ type: 'INPUT', id: block.id, value: e.target.value })}
              onKeyDown={(e) => e.key === 'Enter' && canSubmit && !submitted && handleSubmit()}
              disabled={submitted || showSolution}
              placeholder='답을 입력하세요...'
              className='flex-1 bg-[#1a1a2e] border border-[#2a2a3e] rounded-lg px-4 py-2.5 text-sm font-mono text-white placeholder-[#5a5a72] focus:outline-none focus:border-violet-500 transition-colors disabled:opacity-60'
            />
            {!submitted && !showSolution && (
              <button
                type='button'
                onClick={() => dispatch({ type: 'TOGGLE_KB', id: block.id })}
                title='Math keyboard'
                className='shrink-0 w-9 h-9 flex items-center justify-center rounded-lg bg-[#2a2a3e] hover:bg-[#3a3a5e] text-[#9090a8] hover:text-violet-400 transition-colors text-base'
              >
                ⌨
              </button>
            )}
          </div>
          {showKeyboard && !submitted && !showSolution && (
            <MathKeyboard
              inputRef={mathInputRef}
              value={input}
              onChange={(val) => dispatch({ type: 'INPUT', id: block.id, value: val })}
            />
          )}
          {input && (
            <div className='mt-1.5 px-3 py-1.5 bg-[#1a1a2e] border border-[#2a2a3e] rounded-lg text-sm text-white min-h-8 flex items-center'>
              <MathText text={input} />
            </div>
          )}
        </div>
      )}

      {/* Hints */}
      {visibleHints.length > 0 && (
        <div className='space-y-2 mb-4'>
          {visibleHints.map((hint, i) => (
            <div key={i} className='bg-[#1e1e32] border-l-2 border-amber-400/60 rounded-lg px-4 py-2.5 text-sm text-[#b0b0c8]'>
              💡 <MathText text={hint} />
            </div>
          ))}
        </div>
      )}

      {/* Solution (show solution toggled) */}
      {showSolution && block.explanation && (
        <div className='mb-4 bg-[#1e1e32] border-l-2 border-violet-400/60 rounded-lg px-4 py-3 text-sm text-[#b0b0c8] leading-relaxed'>
          <span className='text-violet-400 font-bold text-xs uppercase tracking-widest block mb-1'>Solution</span>
          <MathText text={block.explanation} />
        </div>
      )}

      {/* Post-submit explanation */}
      {submitted && showExplanation && block.explanation && (
        <div className='mb-4 bg-[#1e1e32] border-l-2 border-violet-400/60 rounded-lg px-4 py-3 text-sm text-[#b0b0c8] leading-relaxed'>
          <MathText text={block.explanation} />
        </div>
      )}

      {/* Result */}
      {submitted && (
        <div className={`mb-4 rounded-lg px-4 py-2.5 border-l-2 text-sm font-semibold ${correct ? 'bg-emerald-500/10 border-emerald-500 text-emerald-400' : 'bg-rose-500/10 border-rose-500 text-rose-400'}`}>
          {correct ? '✓ 정답입니다!' : '✗ 오답입니다.'}
        </div>
      )}

      {/* Actions */}
      <div className='flex items-center justify-between gap-3 flex-wrap'>
        <div className='flex gap-2'>
          {!submitted && !showSolution ? (
            <button
              type='button'
              disabled={!canSubmit}
              onClick={handleSubmit}
              className='px-5 py-2 rounded-lg bg-violet-600 hover:bg-violet-500 disabled:opacity-40 disabled:cursor-not-allowed text-white text-sm font-bold transition-colors'
            >
              Check
            </button>
          ) : submitted && !correct ? (
            <button
              type='button'
              onClick={() => dispatch({ type: 'RETRY', id: block.id })}
              className='px-5 py-2 rounded-lg bg-[#2a2a3e] hover:bg-[#3a3a4e] text-[#c9d1d9] text-sm font-bold transition-colors'
            >
              다시 풀기
            </button>
          ) : null}
        </div>

        <div className='flex items-center gap-3'>
          {hints.length > 0 && hintIndex < hints.length && !submitted && (
            <button
              type='button'
              onClick={() => dispatch({ type: 'SHOW_HINT', id: block.id, total: hints.length })}
              className='flex items-center gap-1.5 text-xs text-[#9090a8] hover:text-amber-400 transition-colors'
            >
              <span className='text-[#5a5a72]'>{hintIndex}/{hints.length}</span>
              <span className='border border-[#3a3a4e] rounded-full px-2.5 py-1 hover:border-amber-400/60'>힌트 보기</span>
            </button>
          )}
          {submitted && block.explanation && !showExplanation && (
            <button
              type='button'
              onClick={() => dispatch({ type: 'SHOW_EXPL', id: block.id })}
              className='text-xs border border-[#3a3a4e] rounded-full px-3 py-1 text-[#9090a8] hover:text-violet-400 hover:border-violet-400/60 transition-colors'
            >
              해설 보기
            </button>
          )}
        </div>
      </div>
    </div>
  );
}

// ─── QuizPanel root ────────────────────────────────────────────────────────────

export default function QuizPanel({ contentJson, onComplete, isCompleted }) {
  const blocks = toBlocks(contentJson);
  const quizBlocks = blocks.filter((b) => b.type === 'quiz');

  const [state, dispatch] = useReducer(reducer, quizBlocks, initState);
  const [showSolutions, setShowSolutions] = useState(false);

  const correctIds = new Set(Object.entries(state).filter(([, q]) => q.correct).map(([id]) => id));
  const allCorrect = quizBlocks.length > 0 && quizBlocks.every((q) => correctIds.has(q.id));

  useEffect(() => {
    if (allCorrect && !isCompleted && onComplete) {
      const answers = quizBlocks.map((q) => ({
        blockId: q.id,
        quizType: q.quizType,
        answer: state[q.id]?.input || state[q.id]?.selected || null,
        isCorrect: state[q.id]?.correct ?? false,
        points: q.points ?? 0,
      }));
      const totalPoints = quizBlocks.reduce((sum, q) => sum + (q.points ?? 0), 0);
      const earnedPoints = quizBlocks.reduce((sum, q) => sum + (state[q.id]?.correct ? (q.points ?? 0) : 0), 0);
      onComplete({ answers, totalPoints, earnedPoints });
    }
  }, [allCorrect]);

  if (blocks.length === 0) {
    return <p className='text-sm text-[#5a5a72]'>콘텐츠 데이터가 없습니다.</p>;
  }

  // Count quiz blocks seen so far (for "Problem N" numbering)
  let quizCounter = -1;

  return (
    <div className='space-y-6'>
      {blocks.map((block) => {
        if (block.type === 'text') {
          return <TextBlock key={block.id} block={block} />;
        }

        quizCounter++;
        const thisIndex = quizCounter;

        return (
          <div key={block.id}>
            <QuizBlock
              block={block}
              quizIndex={thisIndex}
              qState={state[block.id] ?? { selected: null, input: '', submitted: false, correct: false, hintIndex: 0, showExplanation: false, showKeyboard: false }}
              dispatch={dispatch}
              showSolution={showSolutions}
            />
          </div>
        );
      })}

      {/* Show solution toggle */}
      {quizBlocks.length > 0 && (
        <div className='pt-2 border-t border-[#2a2a3e]'>
          <button
            type='button'
            onClick={() => setShowSolutions((v) => !v)}
            className='text-sm text-[#4a8fff] hover:text-[#6aafff] transition-colors flex items-center gap-1'
          >
            I need help. Please show me the solution.
            <span className='text-xs ml-1'>{showSolutions ? '∧' : '∨'}</span>
          </button>
        </div>
      )}
    </div>
  );
}
