'use client';

import Link from 'next/link';
import {
  ArrowLeft,
  LayoutList,
  Bookmark,
  Play,
  CheckSquare,
  ChevronLeft,
  ChevronRight,
  Loader2,
} from 'lucide-react';
import { cn } from '@/lib/utils';

export default function LearnTopBar({
  courseId,
  sectionId,
  prevLecture,
  nextLecture,
  progress = 0,
  isRunning,
  isGrading,
  onRun,
  onGrade,
  isCompleted,
  onComplete,
  sidebarOpen,
  onToggleSidebar,
}) {
  const busy = isRunning || isGrading;

  return (
    <div className='h-12 flex items-center justify-between px-4 bg-[#0a0a14] border-b border-[#1e1e2e] shrink-0 z-10'>
      {/* Left: back + icons */}
      <div className='flex items-center gap-1'>
        <Link
          href={`/learn/${courseId}`}
          className='p-2 rounded-lg text-[#6a6a82] hover:text-white hover:bg-[#1e1e2e] transition-colors'
          title='Back to Course'
        >
          <ArrowLeft size={16} />
        </Link>
        <button
          onClick={onToggleSidebar}
          className={cn(
            'p-2 rounded-lg transition-colors',
            sidebarOpen
              ? 'text-violet-400 bg-[#1e1e2e]'
              : 'text-[#6a6a82] hover:text-white hover:bg-[#1e1e2e]'
          )}
          title='Table of Contents'
        >
          <LayoutList size={16} />
        </button>
        <button className='p-2 rounded-lg text-[#6a6a82] hover:text-white hover:bg-[#1e1e2e] transition-colors' title='Bookmark'>
          <Bookmark size={16} />
        </button>
      </div>

      {/* Center: Run + Submit */}
      <div className='flex items-center gap-2'>
        <button
          onClick={onRun}
          disabled={busy}
          className={cn(
            'flex items-center gap-1.5 px-4 py-1.5 rounded-md text-sm font-semibold transition-all',
            'bg-[#1e1e2e] border border-[#3a3a52] text-white hover:bg-[#2a2a3e]',
            busy && 'opacity-50 cursor-not-allowed'
          )}
        >
          {isRunning ? (
            <Loader2 size={13} className='animate-spin' />
          ) : (
            <Play size={13} fill='currentColor' className='text-violet-400' />
          )}
          Run
        </button>

        <button
          onClick={onGrade}
          disabled={busy}
          className={cn(
            'flex items-center gap-1.5 px-4 py-1.5 rounded-md text-sm font-semibold transition-all',
            'bg-violet-600 hover:bg-violet-700 text-white',
            busy && 'opacity-50 cursor-not-allowed'
          )}
        >
          {isGrading ? (
            <Loader2 size={13} className='animate-spin' />
          ) : (
            <CheckSquare size={13} />
          )}
          Submit
        </button>
      </div>

      {/* Right: Prev / Next + Complete */}
      <div className='flex items-center gap-2'>
        {prevLecture ? (
          <Link
            href={prevLecture.href}
            className='flex items-center gap-1 px-2 py-1.5 rounded-md text-xs text-[#9090a8] hover:text-white hover:bg-[#1e1e2e] transition-colors'
          >
            <ChevronLeft size={14} />
            Previous
          </Link>
        ) : (
          <span className='flex items-center gap-1 px-2 py-1.5 text-xs text-[#3a3a52] cursor-not-allowed'>
            <ChevronLeft size={14} />
            Previous
          </span>
        )}

        <div className='w-28 h-1.5 bg-[#1e1e2e] rounded-full overflow-hidden'>
          <div
            className='h-full bg-violet-600 rounded-full transition-all duration-300'
            style={{ width: `${Math.round(progress * 100)}%` }}
          />
        </div>

        {nextLecture ? (
          <Link
            href={nextLecture.href}
            className='flex items-center gap-1 px-2 py-1.5 rounded-md text-xs text-[#9090a8] hover:text-white hover:bg-[#1e1e2e] transition-colors'
          >
            Next
            <ChevronRight size={14} />
          </Link>
        ) : (
          <span className='flex items-center gap-1 px-2 py-1.5 text-xs text-[#3a3a52] cursor-not-allowed'>
            Next
            <ChevronRight size={14} />
          </span>
        )}

        <button
          onClick={onComplete}
          className={cn(
            'ml-2 px-3 py-1.5 rounded-md text-xs font-semibold border transition-all',
            isCompleted
              ? 'bg-emerald-600/20 border-emerald-500/40 text-emerald-400'
              : 'border-[#2a2a3e] text-[#9090a8] hover:border-violet-500/40 hover:text-violet-300'
          )}
        >
          {isCompleted ? 'Completed ✓' : 'Complete Lesson'}
        </button>
      </div>
    </div>
  );
}
