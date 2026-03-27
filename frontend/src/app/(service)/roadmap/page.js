'use client';

import { Badge } from '@/components/ui/badge';
import { Sparkles } from 'lucide-react';
import { useState } from 'react';
import { useCourseCategoriesQuery } from '../../../../hooks/queries/use-course-categories';
import { formatCategoryTitle } from '../../../../constants/font-align';
import CourseCard from '@/components/cards/course-card';
import { useCourseQuery } from '../../../../hooks/queries/use-course';

const accentPalette = [
  'from-orange-400 to-amber-500 shadow-orange-200/50',
  'from-emerald-400 to-teal-500 shadow-emerald-200/50',
  'from-blue-400 to-indigo-500 shadow-blue-200/50',
  'from-rose-400 to-fuchsia-500 shadow-rose-200/50',
  'from-violet-400 to-purple-500 shadow-violet-200/50',
];

export default function Page() {
  const { data: courseData = [], isLoading, isError } = useCourseQuery();
  const { data: categoriesData = [] } = useCourseCategoriesQuery();
  const [selectedCategory, setSelectedCategory] = useState('all');

  const filteredCourses = courseData.filter(
    (course) =>
      selectedCategory === 'all' || course.categoryName === selectedCategory,
  );

  return (
    <div className='min-h-screen bg-white dark:bg-neutral-950 px-6 py-20 selection:bg-indigo-100'>
      <div className='mx-auto max-w-7xl'>
        {/* HEADER SECTION */}
        <header className='mb-20 text-center relative'>
          <div className='absolute top-0 left-1/2 -translate-x-1/2 w-64 h-64 bg-indigo-200/20 blur-3xl rounded-full -z-10' />

          <Badge
            variant='info'
            className='px-5 py-1.5 rounded-full bg-white shadow-sm border-slate-200 text-indigo-600 mb-6 animate-bounce-slow'
          >
            <Sparkles size={14} className='mr-1.5' />
            LEVEL UP YOUR SKILLS
          </Badge>

          <h1
            className='text-5xl md:text-6xl font-black tracking-tight leading-[1.1]'
            style={{ color: 'var(--card-foreground)' }}
          >
            What do you plan to <br />
            <span
              className='text-transparent bg-clip-text bg-gradient-to-r from-indigo-600 to-violet-600'
              style={{ color: 'var(--primary)' }}
            >
              learn today?
            </span>
          </h1>

          <p
            className='mt-6 text-xl max-w-2xl mx-auto leading-relaxed font-medium'
            style={{ color: 'var(--muted-foreground)' }}
          >
            Explore our curated roadmaps and master coding step-by-step.
          </p>

          {/* CATEGORY FILTER TAB */}
          <div className='mt-12 flex flex-wrap justify-center gap-3'>
            <button
              onClick={() => setSelectedCategory('all')}
              className={`px-6 py-2.5 rounded-full text-sm font-bold transition-all ${
                selectedCategory === 'all'
                  ? 'bg-slate-900 text-white shadow-lg shadow-slate-200 scale-105'
                  : 'bg-white text-slate-500 hover:bg-slate-50 border border-slate-200'
              }`}
            >
              All Courses
            </button>
            {categoriesData.map((cat) => (
              <button
                key={cat.id}
                onClick={() => setSelectedCategory(cat.categoryName)}
                className={`px-6 py-2.5 rounded-full text-sm font-bold transition-all ${
                  selectedCategory === cat.categoryName
                    ? 'bg-indigo-600 text-white shadow-lg shadow-indigo-200 scale-105'
                    : 'bg-white text-slate-500 hover:bg-slate-50 border border-slate-200'
                }`}
              >
                {formatCategoryTitle(cat.categoryName)}
              </button>
            ))}
          </div>
        </header>

        {/* ROADMAP GRID */}
        <div className='grid gap-10 md:grid-cols-2 lg:grid-cols-3'>
          {isLoading ? (
            Array(6)
              .fill(0)
              .map((_, i) => (
                <div
                  key={i}
                  className='h-80 rounded-[32px] bg-slate-100 animate-pulse'
                />
              ))
          ) : isError ? (
            <div className='col-span-full py-20 text-center rounded-[32px] bg-rose-50 border-2 border-dashed border-rose-200'>
              <p className='text-rose-500 font-bold text-lg'>
                Failed to load roadmaps. Please try again.
              </p>
            </div>
          ) : filteredCourses.length === 0 ? (
            <div className='col-span-full py-32 text-center'>
              <p className='text-slate-400 text-xl font-semibold'>
                No courses found in this category.
              </p>
            </div>
          ) : (
            filteredCourses.map((course, index) => (
              <CourseCard
                key={course.id}
                course={{
                  ...course,
                  accent: accentPalette[index % accentPalette.length],
                }}
              />
            ))
          )}
        </div>
      </div>
    </div>
  );
}
