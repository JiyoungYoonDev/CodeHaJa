'use client';

import { useState } from 'react';
import { useRouter } from 'next/navigation';
import { CheckCircle2, ChevronDown, ChevronRight, X } from 'lucide-react';
import { useCourseDetailQuery } from '../../../hooks/queries/use-course';
import { useLectureItemsQuery } from '../../../hooks/queries/use-learn';
import { cn } from '@/lib/utils';

const ITEM_TYPE_LABEL = {
  RICH_TEXT: 'Concept',
  CODE_BLOCK: 'Concept',
  CODING_SET: 'Coding',
  QUIZ_SET: 'MCQ',
  IMAGE: 'Example',
  VIDEO: 'Video',
  TEST_BLOCK: 'Test',
  CHECKPOINT: 'Checkpoint',
  PROJECT_TASK: 'Project',
};

const ITEM_TYPE_COLOR = {
  RICH_TEXT: 'bg-blue-500/20 text-blue-300',
  CODE_BLOCK: 'bg-blue-500/20 text-blue-300',
  CODING_SET: 'bg-violet-500/20 text-violet-300',
  QUIZ_SET: 'bg-amber-500/20 text-amber-300',
  IMAGE: 'bg-emerald-500/20 text-emerald-300',
  VIDEO: 'bg-rose-500/20 text-rose-300',
  TEST_BLOCK: 'bg-orange-500/20 text-orange-300',
  CHECKPOINT: 'bg-slate-500/20 text-slate-300',
  PROJECT_TASK: 'bg-cyan-500/20 text-cyan-300',
};

function ItemTypeBadge({ itemType }) {
  const label = ITEM_TYPE_LABEL[itemType] ?? itemType;
  const color = ITEM_TYPE_COLOR[itemType] ?? 'bg-slate-500/20 text-slate-300';
  return (
    <span
      className={cn(
        'text-[10px] font-semibold px-1.5 py-0.5 rounded-full',
        color,
      )}
    >
      {label}
    </span>
  );
}

function LectureItemRow({ item, courseId, sectionId, activeItemId, visitedItemIds, passedItemIds, lectureCompleted }) {
  const router = useRouter();
  const isActive = item.id === activeItemId;
  const isVisited = item.itemType === 'CODING_SET'
    ? (passedItemIds?.has(item.id) ?? false)
    : (lectureCompleted || (visitedItemIds?.has(item.id) ?? false));

  function handleClick() {
    router.push(`/learn/${courseId}/${sectionId}?itemId=${item.id}`);
  }

  return (
    <button
      onClick={handleClick}
      className={cn(
        'w-full flex items-center gap-2 px-3 py-2 text-left text-xs rounded-lg transition-colors',
        isActive
          ? 'bg-violet-600/20' // keep accent for active
          : '',
        isActive
          ? { color: 'var(--primary-foreground)' }
          : { color: 'var(--muted-foreground)' },
      )}
    >
      <span className='flex-1 truncate'>{item.title}</span>
      {isVisited && <CheckCircle2 size={11} className='shrink-0 text-emerald-400' />}
      <ItemTypeBadge itemType={item.itemType} />
    </button>
  );
}

function SectionBlock({ section, courseId, activeLectureId, activeItemId, visitedLectureIds, visitedItemIds, passedItemIds }) {
  const isCurrentSection = section.lectures?.some(
    (l) => l.id === activeLectureId,
  );
  const [open, setOpen] = useState(isCurrentSection ?? false);

  return (
    <div>
      <button
        onClick={() => setOpen((v) => !v)}
        className='w-full flex items-center gap-2 px-3 py-2 text-left text-xs font-semibold transition-colors'
        style={{ color: 'var(--muted-foreground)' }}
      >
        {open ? <ChevronDown size={12} /> : <ChevronRight size={12} />}
        <span className='flex-1 truncate uppercase tracking-wider'>
          {section.title}
        </span>
      </button>

      {open &&
        section.lectures?.map((lecture) => (
          <LectureBlock
            key={lecture.id}
            lecture={lecture}
            courseId={courseId}
            sectionId={section.id}
            activeLectureId={activeLectureId}
            activeItemId={activeItemId}
            visitedLectureIds={visitedLectureIds}
            visitedItemIds={visitedItemIds}
            passedItemIds={passedItemIds}
          />
        ))}
    </div>
  );
}

function LectureBlock({
  lecture,
  courseId,
  sectionId,
  activeLectureId,
  activeItemId,
  visitedLectureIds,
  visitedItemIds,
  passedItemIds,
}) {
  const isActive = lecture.id === activeLectureId;
  const isVisited = visitedLectureIds?.has(lecture.id) ?? false;
  const [open, setOpen] = useState(isActive);

  const { data: itemsResponse } = useLectureItemsQuery(
    open ? lecture.id : null,
  );
  const items = itemsResponse?.data?.content ?? [];

  return (
    <div className='ml-4'>
      <button
        onClick={() => setOpen((v) => !v)}
        className={cn(
          'w-full flex items-center gap-2 px-3 py-2 text-left text-xs font-medium transition-colors rounded-lg',
          isActive ? 'text-white' : 'text-[#7a7a92] hover:text-white',
        )}
      >
        {open ? <ChevronDown size={11} /> : <ChevronRight size={11} />}
        <span className='flex-1 truncate'>{lecture.title}</span>
        {isVisited && <CheckCircle2 size={12} className='shrink-0 text-emerald-400' />}
      </button>

      {open && (
        <div className='ml-2 flex flex-col gap-0.5'>
          {items.length === 0 && (
            <p className='px-3 py-1 text-[10px] text-[#3a3a52]'>항목 없음</p>
          )}
          {items.map((item) => (
            <LectureItemRow
              key={item.id}
              item={item}
              courseId={courseId}
              sectionId={sectionId}
              activeItemId={activeItemId}
              visitedItemIds={visitedItemIds}
              passedItemIds={passedItemIds}
              lectureCompleted={isVisited}
            />
          ))}
        </div>
      )}
    </div>
  );
}

export default function LearnSidebar({
  courseId,
  activeLectureId,
  activeItemId,
  visitedLectureIds,
  visitedItemIds,
  passedItemIds,
  onClose,
}) {
  const { data: courseResponse } = useCourseDetailQuery(courseId);
  const course = courseResponse?.data;
  const sections = course?.sections ?? [];

  return (
    <div className='flex flex-col h-full bg-[#0d0d1a] border-r border-[#1e1e2e]'>
      {/* Header */}
      <div className='flex items-center justify-between px-4 py-3 border-b border-[#1e1e2e]'>
        <div className='flex-1 min-w-0'>
          <p className='text-xs font-bold text-white truncate'>
            {course?.title ?? '...'}
          </p>
        </div>
        {onClose && (
          <button
            onClick={onClose}
            className='ml-2 p-1 text-[#6a6a82] hover:text-white transition-colors'
          >
            <X size={14} />
          </button>
        )}
      </div>

      {/* Sections list */}
      <div className='flex-1 overflow-y-auto py-2 flex flex-col gap-1'>
        {sections.map((section) => (
          <SectionBlock
            key={section.id}
            section={section}
            courseId={courseId}
            activeLectureId={activeLectureId}
            activeItemId={activeItemId}
            visitedLectureIds={visitedLectureIds}
            visitedItemIds={visitedItemIds}
            passedItemIds={passedItemIds}
          />
        ))}
      </div>
    </div>
  );
}
