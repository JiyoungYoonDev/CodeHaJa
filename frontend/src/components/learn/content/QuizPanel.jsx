'use client';

import { useEffect, useReducer } from 'react';

const LETTERS = ['①', '②', '③', '④', '⑤', '⑥'];

// Normalize contentJson to always have a quizzes array
function parseQuizzes(contentJson) {
  if (!contentJson) return [];
  if (Array.isArray(contentJson.quizzes)) return contentJson.quizzes;
  // Old single-quiz format
  if (contentJson.question !== undefined || contentJson.quizType !== undefined) {
    return [{ id: 'q0', ...contentJson }];
  }
  return [];
}

function initState(quizzes) {
  return quizzes.reduce((acc, q) => {
    acc[q.id] = { selected: null, input: '', submitted: false, correct: false, hintIndex: 0, showExplanation: false };
    return acc;
  }, {});
}

function reducer(state, action) {
  const { id } = action;
  const q = state[id];
  switch (action.type) {
    case 'SELECT': return { ...state, [id]: { ...q, selected: action.value } };
    case 'INPUT': return { ...state, [id]: { ...q, input: action.value } };
    case 'SUBMIT': return { ...state, [id]: { ...q, submitted: true, correct: action.correct } };
    case 'RETRY': return { ...state, [id]: { ...q, selected: null, input: '', submitted: false, correct: false } };
    case 'SHOW_HINT': return { ...state, [id]: { ...q, hintIndex: Math.min(q.hintIndex + 1, action.total) } };
    case 'SHOW_EXPLANATION': return { ...state, [id]: { ...q, showExplanation: true } };
    default: return state;
  }
}

function SingleQuiz({ quiz, index, qState, dispatch, onCorrect }) {
  const { selected, input, submitted, correct, hintIndex, showExplanation } = qState;
  const hints = quiz.hints ?? [];
  const visibleHints = hints.slice(0, hintIndex);
  const canSubmit = quiz.quizType === 'MULTIPLE_CHOICE' ? selected !== null : input.trim() !== '';

  function handleSubmit() {
    if (submitted) return;
    let isCorrect = false;
    if (quiz.quizType === 'MULTIPLE_CHOICE') {
      const chosen = (quiz.options ?? []).find((o) => o.id === selected);
      isCorrect = chosen?.isCorrect === true;
    } else {
      isCorrect = input.trim().toLowerCase() === (quiz.correctAnswer ?? '').trim().toLowerCase();
    }
    dispatch({ type: 'SUBMIT', id: quiz.id, correct: isCorrect });
    if (isCorrect) onCorrect();
  }

  return (
    <div>
      {/* Header */}
      <div className='flex items-center gap-3 mb-3'>
        <span className='text-violet-400 text-sm font-black'>질문 {index + 1}</span>
        {quiz.points > 0 && (
          <span className='bg-violet-500/20 text-violet-300 text-[11px] font-bold px-2 py-0.5 rounded-full'>
            {quiz.points} XP
          </span>
        )}
      </div>

      {/* Question */}
      <p className='text-white font-bold text-base leading-relaxed mb-4'>{quiz.question}</p>

      {/* Options */}
      {quiz.quizType === 'MULTIPLE_CHOICE' && (
        <div className='space-y-2 mb-5'>
          {(quiz.options ?? []).map((option, i) => {
            const isSelected = selected === option.id;
            let cls = 'border-[#2a2a3e] text-[#c9d1d9] hover:border-[#4a4a5e]';
            if (submitted) {
              if (option.isCorrect) cls = 'border-emerald-500 bg-emerald-500/10 text-emerald-300';
              else if (isSelected && !option.isCorrect) cls = 'border-rose-500 bg-rose-500/10 text-rose-300';
              else cls = 'border-[#2a2a3e] text-[#5a5a72]';
            } else if (isSelected) {
              cls = 'border-violet-500 bg-violet-500/10 text-white';
            }
            return (
              <button
                key={option.id}
                type='button'
                disabled={submitted}
                onClick={() => dispatch({ type: 'SELECT', id: quiz.id, value: option.id })}
                className={`w-full flex items-center gap-3 px-4 py-2.5 rounded-lg border transition-colors text-left text-sm ${cls} ${submitted ? 'cursor-default' : 'cursor-pointer'}`}
              >
                <span className='shrink-0 text-base'>{LETTERS[i] ?? i + 1}</span>
                <span>{option.text}</span>
              </button>
            );
          })}
        </div>
      )}

      {/* Short Answer Input */}
      {quiz.quizType === 'SHORT_ANSWER' && (
        <input
          type='text'
          value={input}
          onChange={(e) => dispatch({ type: 'INPUT', id: quiz.id, value: e.target.value })}
          onKeyDown={(e) => e.key === 'Enter' && canSubmit && !submitted && handleSubmit()}
          disabled={submitted}
          placeholder='정답을 입력하세요...'
          className='w-full bg-[#1a1a2e] border border-[#2a2a3e] rounded-lg px-4 py-2.5 text-sm text-white placeholder-[#5a5a72] focus:outline-none focus:border-violet-500 transition-colors disabled:opacity-60 mb-5'
        />
      )}

      {/* Visible hints */}
      {visibleHints.length > 0 && (
        <div className='space-y-2 mb-4'>
          {visibleHints.map((hint, i) => (
            <div key={i} className='bg-[#1e1e32] border-l-2 border-amber-400/60 rounded-lg px-4 py-2.5 text-sm text-[#b0b0c8]'>
              💡 {hint}
            </div>
          ))}
        </div>
      )}

      {/* Explanation (after submit) */}
      {submitted && showExplanation && quiz.explanation && (
        <div className='mb-4 bg-[#1e1e32] border-l-2 border-violet-400/60 rounded-lg px-4 py-3 text-sm text-[#b0b0c8] leading-relaxed'>
          {quiz.explanation}
        </div>
      )}

      {/* Result feedback */}
      {submitted && (
        <div className={`mb-4 rounded-lg px-4 py-2.5 border-l-2 text-sm font-semibold ${correct ? 'bg-emerald-500/10 border-emerald-500 text-emerald-400' : 'bg-rose-500/10 border-rose-500 text-rose-400'}`}>
          {correct ? '✓ 정답입니다!' : '✗ 오답입니다.'}
        </div>
      )}

      {/* Action row */}
      <div className='flex items-center justify-between gap-3 flex-wrap'>
        <div className='flex gap-2'>
          {!submitted ? (
            <button
              type='button'
              disabled={!canSubmit}
              onClick={handleSubmit}
              className='px-5 py-2 rounded-lg bg-violet-600 hover:bg-violet-500 disabled:opacity-40 disabled:cursor-not-allowed text-white text-sm font-bold transition-colors'
            >
              정답 확인
            </button>
          ) : !correct ? (
            <button
              type='button'
              onClick={() => dispatch({ type: 'RETRY', id: quiz.id })}
              className='px-5 py-2 rounded-lg bg-[#2a2a3e] hover:bg-[#3a3a4e] text-[#c9d1d9] text-sm font-bold transition-colors'
            >
              다시 풀기
            </button>
          ) : null}
        </div>

        <div className='flex items-center gap-3'>
          {hints.length > 0 && hintIndex < hints.length && (
            <button
              type='button'
              onClick={() => dispatch({ type: 'SHOW_HINT', id: quiz.id, total: hints.length })}
              className='flex items-center gap-1.5 text-xs text-[#9090a8] hover:text-amber-400 transition-colors'
            >
              <span className='text-[#5a5a72]'>{hintIndex}/{hints.length} 힌트 사용</span>
              <span className='border border-[#3a3a4e] rounded-full px-2.5 py-1 hover:border-amber-400/60'>
                힌트 보기
              </span>
            </button>
          )}
          {submitted && quiz.explanation && !showExplanation && (
            <button
              type='button'
              onClick={() => dispatch({ type: 'SHOW_EXPLANATION', id: quiz.id })}
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

export default function QuizPanel({ contentJson, onComplete, isCompleted }) {
  const quizzes = parseQuizzes(contentJson);
  const [state, dispatch] = useReducer(reducer, quizzes, initState);

  const correctIds = new Set(Object.entries(state).filter(([, q]) => q.correct).map(([id]) => id));
  const allCorrect = quizzes.length > 0 && quizzes.every((q) => correctIds.has(q.id));

  useEffect(() => {
    if (allCorrect && !isCompleted && onComplete) {
      onComplete();
    }
  }, [allCorrect]);

  if (quizzes.length === 0) {
    return <p className='text-sm text-[#5a5a72]'>퀴즈 데이터가 없습니다.</p>;
  }

  return (
    <div className='space-y-8'>
      {quizzes.map((quiz, index) => (
        <div key={quiz.id}>
          <SingleQuiz
            quiz={quiz}
            index={index}
            qState={state[quiz.id] ?? { selected: null, input: '', submitted: false, correct: false, hintIndex: 0, showExplanation: false }}
            dispatch={dispatch}
            onCorrect={() => {}}
          />
          {index < quizzes.length - 1 && (
            <div className='mt-8 border-t border-[#2a2a3e]' />
          )}
        </div>
      ))}
    </div>
  );
}
