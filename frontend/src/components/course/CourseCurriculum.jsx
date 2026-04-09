'use client';

import { useState } from 'react';
import { CheckCircle2, BookOpen, ChevronDown, ChevronUp } from 'lucide-react';
import { cn } from '@/lib/utils';

const DIFFICULTY_LABEL = {
  BEGINNER: 'Beginner',
  EASY: 'Easy',
  INTERMEDIATE: 'Intermediate',
  ADVANCED: 'Advanced',
  EXPERT: 'Expert',
};

const DIFFICULTY_COLOR = {
  BEGINNER: 'bg-emerald-900/60 text-emerald-400',
  EASY: 'bg-emerald-900/60 text-emerald-400',
  INTERMEDIATE: 'bg-blue-900/60 text-blue-400',
  ADVANCED: 'bg-orange-900/60 text-orange-400',
  EXPERT: 'bg-red-900/60 text-red-400',
};

function DifficultyBadge({ difficulty }) {
  const key = difficulty?.toUpperCase() ?? 'EASY';
  const label = DIFFICULTY_LABEL[key] ?? key;
  const color = DIFFICULTY_COLOR[key] ?? DIFFICULTY_COLOR.EASY;
  return (
    <span className={cn('text-xs font-semibold px-3 py-1 rounded-full', color)}>
      {label}
    </span>
  );
}

function SectionTable({ section, sectionIndex, courseId, visitedItemIds, difficulty, onStartLecture }) {
  const [open, setOpen] = useState(sectionIndex === 0);
  const lectures = section.lectures ?? [];

  const visitedCount = lectures.filter(
    (l) => l.firstItemId && visitedItemIds?.has(l.firstItemId),
  ).length;
  const progressPct = lectures.length > 0 ? Math.round((visitedCount / lectures.length) * 100) : 0;

  return (
    <div className='border border-border rounded-2xl overflow-hidden'>
      {/* Section header */}
      <button
        onClick={() => setOpen((v) => !v)}
        className='w-full flex items-center gap-4 px-6 py-5 bg-card hover:bg-accent transition-colors text-left'
      >
        <div className='w-10 h-10 rounded-full bg-muted flex items-center justify-center shrink-0'>
          <span className='text-sm font-black text-muted-foreground'>
            {sectionIndex + 1}
          </span>
        </div>

        <div className='flex-1 min-w-0'>
          <p className='text-sm font-bold text-foreground'>{section.title}</p>
          {section.description && (
            <p className='text-xs text-muted-foreground mt-0.5 truncate'>
              {section.description}
            </p>
          )}
          {visitedCount > 0 && (
            <p className='text-xs font-semibold text-emerald-500 mt-1'>
              {progressPct}% in progress
            </p>
          )}
        </div>

        <span className='text-xs text-muted-foreground shrink-0'>
          {lectures.length} {lectures.length === 1 ? 'lecture' : 'lectures'}
        </span>
        {open ? (
          <ChevronUp size={16} className='text-muted-foreground shrink-0' />
        ) : (
          <ChevronDown size={16} className='text-muted-foreground shrink-0' />
        )}
      </button>

      {/* Table */}
      {open && lectures.length > 0 && (
        <div className='bg-card'>
          {/* Table header */}
          <div className='grid grid-cols-[1fr_80px_100px_70px] gap-4 px-6 py-2 border-t border-border'>
            <span className='text-[10px] font-bold uppercase tracking-widest text-muted-foreground'>
              Problem Name
            </span>
            <span className='text-[10px] font-bold uppercase tracking-widest text-muted-foreground text-center'>
              Status
            </span>
            <span className='text-[10px] font-bold uppercase tracking-widest text-muted-foreground text-center'>
              Difficulty
            </span>
            <span className='text-[10px] font-bold uppercase tracking-widest text-muted-foreground text-right'>
              Time
            </span>
          </div>

          {/* Rows */}
          <div className='divide-y divide-border'>
            {lectures.map((lecture) => {
              const isVisited = lecture.firstItemId && visitedItemIds?.has(lecture.firstItemId);
              return (
                <button
                  key={lecture.id}
                  onClick={() => onStartLecture?.(courseId, section.id, lecture.firstItemId)}
                  className='w-full grid grid-cols-[1fr_80px_100px_70px] gap-4 items-center px-6 py-3 hover:bg-accent transition-colors text-left'
                >
                  <span
                    className={cn(
                      'text-sm font-medium truncate',
                      isVisited
                        ? 'text-muted-foreground'
                        : 'text-primary',
                    )}
                  >
                    {lecture.title}
                  </span>

                  <span className='flex justify-center'>
                    {isVisited ? (
                      <CheckCircle2 size={18} className='text-emerald-500' />
                    ) : (
                      <BookOpen size={16} className='text-muted-foreground' />
                    )}
                  </span>

                  <span className='flex justify-center'>
                    <DifficultyBadge difficulty={difficulty} />
                  </span>

                  <span className='text-xs text-muted-foreground text-right'>
                    {lecture.durationMinutes != null ? `${lecture.durationMinutes}m` : '-'}
                  </span>
                </button>
              );
            })}
          </div>
        </div>
      )}

      {open && lectures.length === 0 && (
        <div className='px-6 py-4 text-xs text-muted-foreground bg-card border-t border-border'>
          No lectures available.
        </div>
      )}
    </div>
  );
}

export default function CourseCurriculum({
  sections = [],
  courseId,
  difficulty,
  visitedItemIds,
  onStartLecture,
}) {
  if (!sections.length) {
    return <p className='text-muted-foreground text-sm'>No curriculum information available.</p>;
  }

  return (
    <div className='space-y-3'>
      {sections.map((section, i) => (
        <SectionTable
          key={section.id}
          section={section}
          sectionIndex={i}
          courseId={courseId}
          visitedItemIds={visitedItemIds}
          difficulty={difficulty}
          onStartLecture={onStartLecture}
        />
      ))}
    </div>
  );
}
