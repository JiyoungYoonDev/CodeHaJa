'use client';

import { useEffect } from 'react';
import { CheckCircle2, ChevronRight, Trophy } from 'lucide-react';

export default function CorrectModal({ open, onNext, onClose, hasNext }) {
  // Auto-advance after 3 s if there's a next item, otherwise auto-close after 4 s
  useEffect(() => {
    if (!open) return;
    const t = setTimeout(hasNext ? onNext : onClose, hasNext ? 3000 : 4000);
    return () => clearTimeout(t);
  }, [open, hasNext, onNext, onClose]);

  if (!open) return null;

  return (
    <div className='fixed inset-0 z-50 flex items-end justify-center pb-16 pointer-events-none'>
      <div
        className='pointer-events-auto bg-[#0d1117] border border-emerald-500/30 rounded-2xl px-8 py-6
                   flex flex-col items-center gap-3 shadow-[0_0_60px_rgba(52,211,153,0.15)]
                   animate-in slide-in-from-bottom-4 fade-in duration-300 w-full max-w-sm mx-4'
      >
        {/* Icon */}
        <div className='w-14 h-14 rounded-full bg-emerald-500/10 flex items-center justify-center'>
          <Trophy size={28} className='text-emerald-400' />
        </div>

        {/* Text */}
        <div className='text-center'>
          <p className='text-lg font-bold text-white'>Correct! 🎉</p>
          <p className='text-sm text-[#6a6a82] mt-1'>
            {hasNext ? 'Moving to the next question in 3 seconds.' : 'You have completed all the questions in this section!'}
          </p>
        </div>

        {/* Button */}
        {hasNext && (
          <button
            onClick={onNext}
            className='mt-1 flex items-center gap-1.5 px-5 py-2 rounded-xl bg-emerald-600 hover:bg-emerald-500
                       text-white text-sm font-semibold transition-colors'
          >
            Next
            <ChevronRight size={15} />
          </button>
        )}
        {!hasNext && (
          <button
            onClick={onClose}
            className='mt-1 flex items-center gap-1.5 px-5 py-2 rounded-xl bg-emerald-600 hover:bg-emerald-500
                       text-white text-sm font-semibold transition-colors'
          >
            <CheckCircle2 size={15} />
            Complete
          </button>
        )}
      </div>
    </div>
  );
}
