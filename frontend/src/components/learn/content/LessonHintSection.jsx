'use client';

import { useState, useEffect, useCallback } from 'react';
import { Lightbulb } from 'lucide-react';
import { cn } from '@/lib/utils';
import { apiFetch } from '@/lib/api-client';

/** Parse backtick-wrapped inline code and newlines in hint text */
function HintText({ text }) {
  if (!text) return null;
  // Split by `code` patterns
  const parts = text.split(/(`[^`]+`)/g);
  return (
    <>
      {parts.map((part, i) => {
        if (part.startsWith('`') && part.endsWith('`')) {
          return (
            <code key={i} className='px-1.5 py-0.5 rounded bg-[#2a2a44] text-violet-300 text-xs font-mono'>
              {part.slice(1, -1)}
            </code>
          );
        }
        // Handle newlines
        return part.split('\n').map((line, j, arr) => (
          <span key={`${i}-${j}`}>
            {line}
            {j < arr.length - 1 && <br />}
          </span>
        ));
      })}
    </>
  );
}

export default function LessonHintSection({ hints = [], totalHints = 2, lectureItemId, xpPenalty = 5 }) {
  const [revealed, setRevealed] = useState([]);
  const [loading, setLoading] = useState(false);

  // Load existing hint usage from server
  useEffect(() => {
    if (!lectureItemId) return;
    apiFetch(`/api/interactions/items/${lectureItemId}/hints`)
      .then((res) => {
        const indexes = res?.data?.revealedIndexes ?? [];
        if (indexes.length > 0) setRevealed(indexes);
      })
      .catch(() => {});
  }, [lectureItemId]);

  const revealHint = useCallback(async () => {
    if (revealed.length >= hints.length) return;
    const nextIndex = revealed.length;

    if (lectureItemId) {
      setLoading(true);
      try {
        const res = await apiFetch(`/api/interactions/items/${lectureItemId}/hints`, {
          method: 'POST',
          body: JSON.stringify({ hintIndex: nextIndex }),
        });
        setRevealed(res?.data?.revealedIndexes ?? [...revealed, nextIndex]);
      } catch {
        // Fallback: still reveal locally
        setRevealed((prev) => [...prev, nextIndex]);
      } finally {
        setLoading(false);
      }
    } else {
      setRevealed((prev) => [...prev, nextIndex]);
    }
  }, [revealed, hints.length, lectureItemId]);

  const canReveal = revealed.length < totalHints && revealed.length < hints.length;

  return (
    <div className='mt-6'>
      <div className='flex items-center justify-between'>
        <div className='flex items-center gap-2 text-sm text-[#9090a8]'>
          <Lightbulb size={15} className='text-amber-400' />
          <span>
            {revealed.length}/{totalHints} used hints
          </span>
        </div>
        <button
          onClick={revealHint}
          disabled={!canReveal || loading}
          className={cn(
            'text-xs px-3 py-1.5 rounded-full border font-medium transition-all',
            canReveal && !loading
              ? 'border-violet-500/40 text-violet-400 hover:bg-violet-600/10 cursor-pointer'
              : 'border-[#2e2e42] text-[#5a5a72] cursor-not-allowed'
          )}
        >
          {loading ? 'Loading...' : `Show Hint (-${xpPenalty}XP)`}
        </button>
      </div>

      {revealed.length > 0 && (
        <div className='mt-3 space-y-2'>
          {revealed.map((idx) => (
            <div
              key={idx}
              className='text-sm text-[#c0c0d8] bg-[#1e1e32] border border-[#2e2e48] rounded-lg px-4 py-3 leading-relaxed'
            >
              <HintText text={hints[idx] ?? `Hint ${idx + 1}: (No hint available)`} />
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
