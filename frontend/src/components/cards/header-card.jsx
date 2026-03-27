'use client';

import { Button } from '@/components/ui/button';
import { ThumbsUp, Plus, CheckCircle2 } from 'lucide-react';

function HeaderCard({ data, onEnroll, isEnrolled }) {
  const roundLearner = (count) => {
    if (count >= 1000) return (count / 1000).toFixed(1) + 'K';
    return count;
  };

  return (
    <div className='flex flex-col space-y-8 p-4'>
      {/* Title and Description */}
      <div className='space-y-6'>
        <h1 className='text-4xl md:text-6xl font-black tracking-tight leading-[1.2] text-slate-900 dark:text-slate-50'>
          {data?.title}
        </h1>
        <p className='text-lg md:text-xl max-w-4xl font-medium leading-relaxed text-slate-500 dark:text-slate-400'>
          {data?.description}
        </p>
      </div>

      {/* Rating and Learners Info */}
      <div className='flex items-center gap-4'>
        <div className='flex items-center gap-2 px-3 py-1.5 bg-violet-50 dark:bg-violet-950/50 text-violet-600 dark:text-violet-400 rounded-lg border border-violet-100 dark:border-violet-800/50 shadow-sm'>
          <ThumbsUp size={16} className='fill-current' />
          <span className='text-sm font-bold'>
            {data?.rating ? (data.rating * 20).toFixed(0) : 95}% Student Satisfaction
          </span>
        </div>
        {data?.learnersCount != null && (
          <span className='text-sm font-medium text-slate-400 dark:text-slate-500'>
            ({roundLearner(data?.learnersCount)} learners)
          </span>
        )}
      </div>

      {/* Action Button Group */}
      <div className='flex flex-wrap items-center gap-4 pt-4'>
        {isEnrolled ? (
          <Button
            onClick={onEnroll}
            className='h-14 px-10 rounded-2xl text-lg shadow-lg shadow-emerald-100 dark:shadow-emerald-950 bg-emerald-600 hover:bg-emerald-700'
          >
            <CheckCircle2 size={20} className='mr-2' />
            Continue Learning
          </Button>
        ) : (
          <Button
            onClick={onEnroll}
            className='h-14 px-10 rounded-2xl text-lg shadow-lg shadow-indigo-200 dark:shadow-indigo-950'
          >
            Enroll Now
          </Button>
        )}
        <Button
          variant='ghost'
          className='h-14 px-6 text-indigo-600 dark:text-indigo-400 font-bold text-lg gap-2 hover:bg-indigo-50 dark:hover:bg-indigo-950/50'
        >
          <div className='bg-indigo-600 dark:bg-indigo-500 text-white rounded-md p-0.5'>
            <Plus size={18} strokeWidth={3} />
          </div>
          Add to Playlist
        </Button>
      </div>
    </div>
  );
}

export default HeaderCard;
