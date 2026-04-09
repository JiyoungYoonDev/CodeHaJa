'use client';

import { useEffect, useRef, useState } from 'react';
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
  Send,
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
  // PROJECT mode
  itemType,
  submissionType,
  onSubmitProject,
  isSubmittingProject,
  // Gamification
  hearts = 5,
}) {
  const busy = isRunning || isGrading || isSubmittingProject;
  const isProject = itemType === 'PROJECT';
  const isProjectEditor = isProject && submissionType === 'EDITOR';

  // Track heart loss for animation
  const prevHeartsRef = useRef(hearts);
  const [lostIndex, setLostIndex] = useState(null);

  useEffect(() => {
    if (hearts < prevHeartsRef.current) {
      // The heart at index `hearts` just got lost (0-indexed)
      setLostIndex(hearts);
      const timer = setTimeout(() => setLostIndex(null), 600);
      prevHeartsRef.current = hearts;
      return () => clearTimeout(timer);
    }
    prevHeartsRef.current = hearts;
  }, [hearts]);

  return (
    <div className='h-12 flex items-center justify-between px-4 bg-[#0a0a14] border-b border-[#1e1e2e] shrink-0 z-10'>
      {/* Left: back + icons + hearts */}
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
        {/* Hearts */}
        <div className='flex items-center gap-0.5 ml-2 px-2 py-1 rounded-lg bg-[#1e1e2e]' title={`하트 ${hearts}/5`}>
          {[...Array(5)].map((_, i) => (
            <span
              key={i}
              className={cn(
                'text-sm leading-none transition-colors duration-300',
                i < hearts ? 'text-red-400' : 'text-[#3a3a52]',
                i === lostIndex && 'animate-heart-break'
              )}
            >
              ♥
            </span>
          ))}
          <style jsx>{`
            @keyframes heart-break {
              0% { transform: scale(1); opacity: 1; color: #f87171; }
              30% { transform: scale(1.4); opacity: 1; color: #f87171; }
              60% { transform: scale(0.6); opacity: 0.5; }
              100% { transform: scale(1); opacity: 1; color: #3a3a52; }
            }
            .animate-heart-break {
              animation: heart-break 0.6s ease-out forwards;
            }
          `}</style>
        </div>
      </div>

      {/* Center: context-aware buttons */}
      <div className='flex items-center gap-2'>
        {/* CODING_SET: Run + Grade */}
        {!isProject && onRun && (
          <button
            onClick={onRun}
            disabled={busy}
            className={cn(
              'flex items-center gap-1.5 px-4 py-1.5 rounded-md text-sm font-semibold transition-all',
              'bg-[#1e1e2e] border border-[#3a3a52] text-white hover:bg-[#2a2a3e]',
              busy && 'opacity-50 cursor-not-allowed'
            )}
          >
            {isRunning ? <Loader2 size={13} className='animate-spin' /> : <Play size={13} fill='currentColor' className='text-violet-400' />}
            Run
          </button>
        )}
        {!isProject && onGrade && (
          <button
            onClick={onGrade}
            disabled={busy}
            className={cn(
              'flex items-center gap-1.5 px-4 py-1.5 rounded-md text-sm font-semibold transition-all',
              'bg-violet-600 hover:bg-violet-700 text-white',
              busy && 'opacity-50 cursor-not-allowed'
            )}
          >
            {isGrading ? <Loader2 size={13} className='animate-spin' /> : <CheckSquare size={13} />}
            Submit
          </button>
        )}

        {/* PROJECT EDITOR: Run + Submit Project */}
        {isProjectEditor && (
          <>
            <button
              onClick={onRun}
              disabled={busy}
              className={cn(
                'flex items-center gap-1.5 px-4 py-1.5 rounded-md text-sm font-semibold transition-all',
                'bg-[#1e1e2e] border border-[#3a3a52] text-white hover:bg-[#2a2a3e]',
                busy && 'opacity-50 cursor-not-allowed'
              )}
            >
              {isRunning ? <Loader2 size={13} className='animate-spin' /> : <Play size={13} fill='currentColor' className='text-violet-400' />}
              Run
            </button>
            <button
              onClick={onSubmitProject}
              disabled={busy || isCompleted}
              className={cn(
                'flex items-center gap-1.5 px-4 py-1.5 rounded-md text-sm font-semibold transition-all',
                isCompleted
                  ? 'bg-emerald-600/20 border border-emerald-500/40 text-emerald-400 cursor-default'
                  : 'bg-violet-600 hover:bg-violet-500 text-white',
                busy && !isCompleted && 'opacity-50 cursor-not-allowed'
              )}
            >
              {isSubmittingProject ? <Loader2 size={13} className='animate-spin' /> : <Send size={13} />}
              {isCompleted ? 'Submitted ✓' : 'Submit Project'}
            </button>
          </>
        )}

        {/* PROJECT REPO: submit handled inside right panel — show nothing here */}
      </div>

      {/* Right: Prev / Progress / Next + Complete */}
      <div className='flex items-center gap-2 flex-1 max-w-xl ml-4'>
        {prevLecture ? (
          <Link
            href={prevLecture.href}
            className='flex items-center gap-1 px-2 py-1.5 rounded-md text-xs text-[#9090a8] hover:text-white hover:bg-[#1e1e2e] transition-colors shrink-0'
          >
            <ChevronLeft size={14} />
            Prev
          </Link>
        ) : (
          <span className='flex items-center gap-1 px-2 py-1.5 text-xs text-[#3a3a52] cursor-not-allowed shrink-0'>
            <ChevronLeft size={14} />
            Prev
          </span>
        )}

        <div className='flex-1 h-1.5 bg-[#1e1e2e] rounded-full overflow-hidden'>
          <div
            className='h-full bg-violet-600 rounded-full transition-all duration-500'
            style={{ width: `${Math.round(progress * 100)}%` }}
          />
        </div>

        {nextLecture ? (
          <Link
            href={nextLecture.href}
            className='flex items-center gap-1 px-2 py-1.5 rounded-md text-xs text-[#9090a8] hover:text-white hover:bg-[#1e1e2e] transition-colors shrink-0'
          >
            Next
            <ChevronRight size={14} />
          </Link>
        ) : (
          <span className='flex items-center gap-1 px-2 py-1.5 text-xs text-[#3a3a52] cursor-not-allowed shrink-0'>
            Next
            <ChevronRight size={14} />
          </span>
        )}

        <button
          onClick={onComplete}
          className={cn(
            'ml-2 px-3 py-1.5 rounded-md text-xs font-semibold border transition-all shrink-0',
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
