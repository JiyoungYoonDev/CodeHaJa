import Link from 'next/link';
import { Badge } from '@/components/ui/badge';
import { ArrowRight, BookOpen, Sparkles, Users, Clock } from 'lucide-react';

export default function CourseCard({ course }) {
  const title = course.problem_title || 'Untitled';
  const summary = course.book_description || 'No description available.';
  const problemCount = course.book_count || 0;
  const submissionCount = course.book_submission_count || 0;
  const projectsCount = course.book_projects_count || 0;
  const hours = course.hours || 0;
  const category = course.course_category?.category_name || 'General';
  const isJoined = course.book_user_joined;
  const badgeType = course.badge_type;
console.log(course.id)
  return (
    <Link
      href={`/roadmap/${category}/${course.id}`}
      className='group relative flex flex-col h-full bg-white rounded-[32px] border border-slate-100 shadow-[0_8px_30px_rgb(0,0,0,0.04)] transition-all duration-500 hover:-translate-y-3 hover:shadow-[0_20px_50px_rgba(79,70,229,0.15)] hover:border-indigo-100'
    >
      <div className={`absolute top-0 inset-x-0 h-2 bg-gradient-to-r ${course.accent || 'from-slate-400 to-slate-500'} rounded-t-[32px]`} />

      <div className='p-8 pt-10 flex flex-col h-full'>
        <div className='flex justify-between items-center mb-8'>
          <div className={`p-4 rounded-2xl bg-gradient-to-br ${course.accent || 'from-indigo-500 to-purple-600'} text-white shadow-xl`}>
            <BookOpen size={28} strokeWidth={2.5} />
          </div>
          
          <div className="flex gap-2">
             {badgeType && <Badge variant="secondary" className="px-3 py-1 rounded-full">{badgeType}</Badge>}
             
             {isJoined ? (
                <Badge variant='success' className='px-4 py-1.5 rounded-full text-xs uppercase tracking-wider'>
                  Learning
                </Badge>
              ) : (
                <div className='text-indigo-600 opacity-0 group-hover:opacity-100 transition-opacity translate-x-4 group-hover:translate-x-0 duration-300'>
                  <ArrowRight size={24} />
                </div>
              )}
          </div>
        </div>

        <div className="mb-2">
            <span className="text-xs font-bold text-indigo-600 uppercase tracking-tighter">{category}</span>
        </div>

        <h3 className='text-2xl font-black text-slate-800 leading-tight group-hover:text-indigo-600 transition-colors mb-3'>
          {title}
        </h3>

        <p className='text-slate-500 text-sm leading-relaxed line-clamp-2 mb-8 font-medium'>
          {summary}
        </p>

        {/* 하단 스탯 영역 */}
        <div className='mt-auto pt-6 border-t border-slate-50 flex items-center justify-between'>
          <div className='space-y-1'>
            <p className='text-[10px] font-black text-slate-400 uppercase tracking-widest'>Curriculum</p>
            <div className='flex items-center gap-1.5'>
              <Sparkles size={14} className='text-amber-500' />
              <span className='text-sm font-bold text-slate-700'>{problemCount} Problems</span>
            </div>
          </div>
          
          <div className='h-8 w-px bg-slate-100' />

          <div className='space-y-1 text-center px-2'>
            <p className='text-[10px] font-black text-slate-400 uppercase tracking-widest'>Duration</p>
            <div className='flex items-center gap-1.5'>
              <Clock size={14} className='text-slate-400' />
              <span className='text-sm font-bold text-slate-700'>{hours}h</span>
            </div>
          </div>

          <div className='h-8 w-px bg-slate-100' />
          
          <div className='space-y-1 text-right'>
            <p className='text-[10px] font-black text-slate-400 uppercase tracking-widest'>Projects</p>
            <div className='flex items-center gap-1.5 justify-end'>
              <Users size={14} className='text-indigo-500' />
              <span className='text-sm font-bold text-slate-700'>{projectsCount}개</span>
            </div>
          </div>
        </div>
      </div>
    </Link>
  );
}