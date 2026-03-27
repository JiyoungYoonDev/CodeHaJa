'use client';

import { useEffect } from 'react';
import { useRouter } from 'next/navigation';
import { Lock, X } from 'lucide-react';

export default function EnrollModal({ courseId, onClose }) {
  const router = useRouter();

  useEffect(() => {
    const handleKey = (e) => { if (e.key === 'Escape') onClose(); };
    document.addEventListener('keydown', handleKey);
    document.body.style.overflow = 'hidden';
    return () => {
      document.removeEventListener('keydown', handleKey);
      document.body.style.overflow = '';
    };
  }, [onClose]);

  return (
    <div
      className='fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm'
      onClick={onClose}
    >
      <div
        className='relative bg-white dark:bg-slate-900 rounded-2xl shadow-2xl w-full max-w-sm mx-4 p-8 flex flex-col items-center text-center'
        onClick={(e) => e.stopPropagation()}
      >
        <button
          onClick={onClose}
          className='absolute top-4 right-4 p-1.5 rounded-lg text-slate-400 hover:text-slate-700 dark:hover:text-slate-200 hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors'
        >
          <X size={16} />
        </button>

        <div className='w-14 h-14 rounded-full bg-violet-100 dark:bg-violet-900/30 flex items-center justify-center mb-4'>
          <Lock size={24} className='text-violet-600 dark:text-violet-400' />
        </div>

        <h2 className='text-lg font-bold text-slate-900 dark:text-white mb-2'>
          This lesson is locked
        </h2>
        <p className='text-sm text-slate-500 dark:text-slate-400 mb-6'>
          To access this lesson, you need to enroll in the course first.
        </p>

        <button
          onClick={() => {
            onClose();
            router.push(`/courses/${courseId}/enroll`);
          }}
          className='w-full py-3 rounded-xl bg-violet-600 hover:bg-violet-700 text-white text-sm font-semibold transition-colors'
        >
          Enroll Now
        </button>
        <button
          onClick={onClose}
          className='mt-3 text-sm text-slate-400 hover:text-slate-600 dark:hover:text-slate-300 transition-colors'
        >
          Later
        </button>
      </div>
    </div>
  );
}
