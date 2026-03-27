'use client';

import Link from 'next/link';
import { Home, ChevronLeft, ChevronRight, Play } from 'lucide-react';
import { cn } from '@/lib/utils';

export default function LearnBottomBar({
  courseId,
  currentLectureTitle,
  prevLecture,
  nextLecture,
  onComplete,
  isCompleted,
}) {
  return (
    <div className='h-12 flex items-center justify-between px-6 bg-[#0a0a14] border-t border-[#1e1e2e] flex-shrink-0'>
      {/* Left: Home */}
      <Link
        href={`/learn/${courseId}`}
        className='flex items-center gap-2 text-sm text-[#6a6a82] hover:text-white transition-colors'
      >
        <Home size={15} />
        <span>Back to Home</span>
      </Link>

      {/* Center: Prev / current / next */}
      <div className='flex items-center gap-4'>
        <button
          disabled={!prevLecture}
          className={cn(
            'p-1.5 rounded-lg transition-colors',
            prevLecture
              ? 'text-[#9090a8] hover:text-white hover:bg-[#1e1e2e]'
              : 'text-[#3a3a52] cursor-not-allowed'
          )}
        >
          <ChevronLeft size={16} />
        </button>

        <div className='flex items-center gap-2 text-sm text-[#c0c0d8]'>
          <Play size={13} fill='currentColor' className='text-violet-400' />
          <span className='font-medium'>{currentLectureTitle ?? 'Lesson'}</span>
        </div>

        {nextLecture ? (
          <Link
            href={nextLecture.href}
            className='p-1.5 rounded-lg text-[#9090a8] hover:text-white hover:bg-[#1e1e2e] transition-colors'
          >
            <ChevronRight size={16} />
          </Link>
        ) : (
          <button className='p-1.5 rounded-lg text-[#3a3a52] cursor-not-allowed'>
            <ChevronRight size={16} />
          </button>
        )}
      </div>

      {/* Right: Next lecture label + Complete button */}
      <div className='flex items-center gap-4'>
        {nextLecture && (
          <Link
            href={nextLecture.href}
            className='text-sm text-[#6a6a82] hover:text-white transition-colors flex items-center gap-1'
          >
            {nextLecture.title}
            <ChevronRight size={14} />
          </Link>
        )}
        <button
          onClick={onComplete}
          className={cn(
            'px-4 py-1.5 rounded-lg text-sm font-semibold transition-all',
            isCompleted
              ? 'bg-emerald-600/20 text-emerald-400 border border-emerald-500/40'
              : 'bg-[#1e1e2e] text-[#9090a8] border border-[#2a2a3e] hover:bg-violet-600/10 hover:text-violet-300 hover:border-violet-500/40'
          )}
        >
          {isCompleted ? 'Completed ✓' : 'Complete Lesson'}
        </button>
      </div>
    </div>
  );
}
