'use client';

import { Badge } from '@/components/ui/badge';
import { Sparkles } from 'lucide-react';
import { useState } from 'react';
import { useProblemBooks } from '@/lib/queries/problemBooks';
import { useCourseCategoriesQuery } from '../../../../hooks/queries/use-course-categories';
import { formatCategoryTitle } from '../../../../constants/font-align';
import { useQueryClient } from '@tanstack/react-query';
import CourseCard from '@/components/course-card';

const accentPalette = [
  'from-orange-400 to-amber-500 shadow-orange-200/50',
  'from-emerald-400 to-teal-500 shadow-emerald-200/50',
  'from-blue-400 to-indigo-500 shadow-blue-200/50',
  'from-rose-400 to-fuchsia-500 shadow-rose-200/50',
  'from-violet-400 to-purple-500 shadow-violet-200/50',
];

export default function Page() {
  const { data: booksData = [], isLoading, isError } = useProblemBooks();
  // const queryClient = useQueryClient();
  // queryClient.clear(); // clears all cached queries + mutations
  // // or
  // queryClient.invalidateQueries({ queryKey: ['problem-books'] });
  const { data: categoriesData = [] } = useCourseCategoriesQuery();
  const [selectedCategory, setSelectedCategory] = useState('all');

  const categories = booksData.map((item, index) => {
    const rawCategory = item.course_category.category_name || 'Untitled';

    return {
      id: item.id,
      slug: String(item.problemCategory || '')
        .toLowerCase()
        .replaceAll(' ', '-'),
      summary: item.problemDescription || 'No description available',
      categoryName: formatCategoryTitle(rawCategory),
      isJoined: item.problemUserJoined,
      problemCount: item.problemCount || 0,
      projectCount: item.problemProjectsCount || 0,
      submissionCount: item.problemSubmissionCount || 0,
      accent: accentPalette[index % accentPalette.length],
    };
  });

  const filteredBooks = booksData.filter(
    (book) =>
      selectedCategory === 'all' || book.course_category.category_name === selectedCategory,
  );

  return (
    <div className='min-h-screen bg-[#F8FAFC] px-6 py-20 selection:bg-indigo-100'>
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

          <h1 className='text-5xl md:text-6xl font-black text-slate-900 tracking-tight leading-[1.1]'>
            What do you plan to <br />
            <span className='text-transparent bg-clip-text bg-gradient-to-r from-indigo-600 to-violet-600'>
              learn today?
            </span>
          </h1>

          <p className='mt-6 text-xl text-slate-500 max-w-2xl mx-auto leading-relaxed font-medium'>
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
                onClick={() => setSelectedCategory(cat.category_name)}
                className={`px-6 py-2.5 rounded-full text-sm font-bold transition-all ${
                  selectedCategory === cat.category_name
                    ? 'bg-indigo-600 text-white shadow-lg shadow-indigo-200 scale-105'
                    : 'bg-white text-slate-500 hover:bg-slate-50 border border-slate-200'
                }`}
              >
                {formatCategoryTitle(cat.category_name)}
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
          ) : categories.length === 0 ? (
            <div className='col-span-full py-32 text-center'>
              <p className='text-slate-400 text-xl font-semibold'>
                No courses found in this category.
              </p>
            </div>
          ) : (
            filteredBooks.map((book, index) => (
              <CourseCard
                key={book.id}
                course={{
                  ...book,
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
