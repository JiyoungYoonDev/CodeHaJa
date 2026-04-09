import Link from 'next/link';
import { Badge } from '@/components/ui/badge';
import { ArrowRight, BookOpen, Sparkles, Users, Clock } from 'lucide-react';

export default function CourseCard({ course }) {
  const {
    id,
    title,
    summary,
    problemCount,
    submissionCount,
    projectsCount,
    hours,
    categoryName,
    categorySlug,
    isJoined,
    badgeType,
    accent,
  } = course;

  return (
    <Link
      href={`/learn/${id}`}
      className='group relative flex flex-col h-full rounded-[32px] bg-card border border-border text-card-foreground shadow-[0_8px_30px_rgb(0,0,0,0.04)] transition-all duration-500 hover:-translate-y-3 hover:shadow-[0_20px_50px_rgba(79,70,229,0.15)]'
    >
      <div
        className={`absolute top-0 inset-x-0 h-2 bg-gradient-to-r ${accent || 'from-primary to-primary/70'} rounded-t-[32px]`}
      />

      <div className='p-8 pt-10 flex flex-col h-full'>
        <div className='flex justify-between items-center mb-8'>
          <div
            className={`p-4 rounded-2xl bg-gradient-to-br ${accent || 'from-primary to-primary/70'} text-white shadow-xl`}
          >
            <BookOpen size={28} strokeWidth={2.5} />
          </div>

          <div className='flex gap-2'>
            {badgeType && (
              <Badge variant='secondary' className='px-3 py-1 rounded-full'>
                {badgeType}
              </Badge>
            )}

            {isJoined ? (
              <Badge
                variant='success'
                className='px-4 py-1.5 rounded-full text-xs uppercase tracking-wider'
              >
                Learning
              </Badge>
            ) : (
              <div className='text-primary opacity-0 group-hover:opacity-100 transition-opacity translate-x-4 group-hover:translate-x-0 duration-300'>
                <ArrowRight size={24} />
              </div>
            )}
          </div>
        </div>

        <div className='mb-2'>
          <span className='text-xs font-bold uppercase tracking-tighter text-primary'>
            {categoryName}
          </span>
        </div>

        <h3 className='text-2xl font-black leading-tight text-card-foreground group-hover:text-primary transition-colors mb-3'>
          {title}
        </h3>

        <p className='text-sm leading-relaxed line-clamp-2 mb-8 font-medium text-muted-foreground'>
          {summary}
        </p>

        <div className='mt-auto pt-6 flex items-center justify-between border-t border-border'>
          <div className='space-y-1'>
            <p className='text-[10px] font-black uppercase tracking-widest text-muted-foreground'>
              Curriculum
            </p>
            <div className='flex items-center gap-1.5'>
              <Sparkles size={14} className='text-amber-500' />
              <span className='text-sm font-bold text-foreground'>
                {problemCount} Problems
              </span>
            </div>
          </div>

          <div className='h-8 w-px bg-border' />

          <div className='space-y-1 text-center px-2'>
            <p className='text-[10px] font-black uppercase tracking-widest text-muted-foreground'>
              Duration
            </p>
            <div className='flex items-center gap-1.5'>
              <Clock size={14} className='text-muted-foreground' />
              <span className='text-sm font-bold text-foreground'>
                {hours}h
              </span>
            </div>
          </div>

          <div className='h-8 w-px bg-border' />

          <div className='space-y-1 text-right'>
            <p className='text-[10px] font-black uppercase tracking-widest text-muted-foreground'>
              Projects
            </p>
            <div className='flex items-center gap-1.5 justify-end'>
              <Users size={14} className='text-primary' />
              <span className='text-sm font-bold text-foreground'>
                {projectsCount} Projects
              </span>
            </div>
          </div>
        </div>
      </div>
    </Link>
  );
}
