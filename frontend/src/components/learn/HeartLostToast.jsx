'use client';

import { useEffect } from 'react';

export default function HeartLostToast({ hearts, onClose }) {
  useEffect(() => {
    const t = setTimeout(onClose, 2000);
    return () => clearTimeout(t);
  }, [onClose]);

  return (
    <div className='fixed inset-0 z-50 flex items-end justify-center pb-16 pointer-events-none'>
      <div
        className='pointer-events-auto bg-[#0d1117] border border-rose-500/30 rounded-2xl px-8 py-5
                   flex flex-col items-center gap-2.5 shadow-[0_0_60px_rgba(244,63,94,0.15)]
                   animate-in slide-in-from-bottom-4 fade-in duration-300 w-full max-w-xs mx-4'
      >
        {/* Animated heart */}
        <div className='text-4xl animate-bounce'>💔</div>

        {/* Text */}
        <div className='text-center'>
          <p className='text-base font-bold text-white'>하트 -1</p>
          <p className='text-sm text-[#6a6a82] mt-0.5'>
            남은 하트: {' '}
            {[...Array(5)].map((_, i) => (
              <span key={i} className={i < hearts ? 'text-red-400' : 'text-[#3a3a52]'}>♥</span>
            ))}
          </p>
        </div>
      </div>
    </div>
  );
}
