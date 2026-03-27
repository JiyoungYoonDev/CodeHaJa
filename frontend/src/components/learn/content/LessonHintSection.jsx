'use client';

import { useState } from 'react';
import { Lightbulb } from 'lucide-react';
import { cn } from '@/lib/utils';

export default function LessonHintSection({ hints = [], totalHints = 2, usedHints = 0, xpPenalty = 1 }) {
  const [revealed, setRevealed] = useState([]);

  function revealHint() {
    if (revealed.length >= hints.length) return;
    setRevealed((prev) => [...prev, prev.length]);
  }

  const canReveal = revealed.length < totalHints;

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
          disabled={!canReveal}
          className={cn(
            'text-xs px-3 py-1.5 rounded-full border font-medium transition-all',
            canReveal
              ? 'border-violet-500/40 text-violet-400 hover:bg-violet-600/10 cursor-pointer'
              : 'border-[#2e2e42] text-[#5a5a72] cursor-not-allowed'
          )}
        >
          Show Hint (-{xpPenalty}XP)
        </button>
      </div>

      {revealed.length > 0 && (
        <div className='mt-3 space-y-2'>
          {revealed.map((idx) => (
            <div
              key={idx}
              className='text-sm text-[#c0c0d8] bg-[#1e1e32] border border-[#2e2e48] rounded-lg px-4 py-3'
            >
              {hints[idx] ?? `Hint ${idx + 1}: (No hint available)`}
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
