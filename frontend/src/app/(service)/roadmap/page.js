'use client';

import { Badge } from '@/components/ui/badge';
import { Sparkles } from 'lucide-react';
import { useState } from 'react';
import { useCourseCategoriesQuery } from '../../../../hooks/queries/use-course-categories';
import { formatCategoryTitle } from '../../../../constants/font-align';
import CourseCard from '@/components/cards/course-card';
import { useCourseQuery } from '../../../../hooks/queries/use-course';

const accentPalette = [
  'from-orange-400 to-amber-500',
  'from-emerald-400 to-teal-500',
  'from-blue-400 to-indigo-500',
  'from-rose-400 to-fuchsia-500',
  'from-violet-400 to-purple-500',
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
    <div className='min-h-screen bg-background px-6 py-20'>
      <div className='mx-auto max-w-7xl'>
        {/* HEADER SECTION */}
        <header className='mb-20 text-center relative'>
          <div className='absolute top-0 left-1/2 -translate-x-1/2 w-64 h-64 bg-primary/10 blur-3xl rounded-full -z-10' />

          <Badge
            variant='info'
            className='px-5 py-1.5 rounded-full bg-card border-border text-primary mb-6 animate-bounce-slow'
          >
            <Sparkles size={14} className='mr-1.5' />
            LEVEL UP YOUR SKILLS
          </Badge>

          <h1 className='text-5xl md:text-6xl font-black tracking-tight leading-[1.1] text-foreground'>
            What do you plan to <br />
            <span className='text-primary'>learn today?</span>
          </h1>

          <p className='mt-6 text-xl max-w-2xl mx-auto leading-relaxed font-medium text-muted-foreground'>
            Explore our curated roadmaps and master coding step-by-step.
          </p>

          {/* CATEGORY FILTER TAB */}
          <div className='mt-12 flex flex-wrap justify-center gap-3'>
            <button
              onClick={() => setSelectedCategory('all')}
              className={`px-6 py-2.5 rounded-full text-sm font-bold transition-all ${
                selectedCategory === 'all'
                  ? 'bg-foreground text-background shadow-lg scale-105'
                  : 'bg-card text-muted-foreground hover:bg-accent border border-border'
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
                    ? 'bg-primary text-primary-foreground shadow-lg scale-105'
                    : 'bg-card text-muted-foreground hover:bg-accent border border-border'
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
                  className='h-80 rounded-[32px] bg-muted animate-pulse'
                />
              ))
          ) : isError ? (
            <div className='col-span-full py-20 text-center rounded-[32px] bg-destructive/10 border-2 border-dashed border-destructive/30'>
              <p className='text-destructive font-bold text-lg'>
                Failed to load roadmaps. Please try again.
              </p>
            </div>
          ) : filteredCourses.length === 0 ? (
            <div className='col-span-full py-32 text-center'>
              <p className='text-muted-foreground text-xl font-semibold'>
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
