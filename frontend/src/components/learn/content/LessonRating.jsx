'use client';

import { useState } from 'react';
import { ThumbsUp, ThumbsDown } from 'lucide-react';
import { cn } from '@/lib/utils';

export default function LessonRating() {
  const [rating, setRating] = useState(null);

  return (
    <div className='mt-8 border border-[#2a2a3e] rounded-xl p-5'>
      <p className='text-sm text-[#9090a8] text-center mb-4'>이번 레슨은 어땠나요?</p>
      <div className='flex items-center justify-center gap-4'>
        <button
          onClick={() => setRating('up')}
          className={cn(
            'flex items-center gap-2 px-4 py-2 rounded-lg border text-sm font-medium transition-all',
            rating === 'up'
              ? 'border-violet-500 bg-violet-600/15 text-violet-300'
              : 'border-[#2a2a3e] text-[#7070a0] hover:border-violet-500/50 hover:text-violet-400'
          )}
        >
          <ThumbsUp size={15} />
        </button>
        <div className='w-px h-5 bg-[#2a2a3e]' />
        <button
          onClick={() => setRating('down')}
          className={cn(
            'flex items-center gap-2 px-4 py-2 rounded-lg border text-sm font-medium transition-all',
            rating === 'down'
              ? 'border-rose-500 bg-rose-600/15 text-rose-300'
              : 'border-[#2a2a3e] text-[#7070a0] hover:border-rose-500/50 hover:text-rose-400'
          )}
        >
          <ThumbsDown size={15} />
        </button>
      </div>
    </div>
  );
}
