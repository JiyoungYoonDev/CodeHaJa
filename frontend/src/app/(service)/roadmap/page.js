'use client';

import Link from 'next/link';
import { Badge } from '@/components/ui/badge';
import { BookOpen, Trophy, Users, ArrowRight, Sparkles } from 'lucide-react';
import { useState } from 'react';
import { useProblemBooks } from '@/lib/queries/problemBooks';
import { useCourseCategoriesQuery } from '../../../../hooks/queries/use-course-categories';
import { formatCategoryTitle } from '../../../../constants/font-align';

const accentPalette = [
  'from-orange-400 to-amber-500 shadow-orange-200/50',
  'from-emerald-400 to-teal-500 shadow-emerald-200/50',
  'from-blue-400 to-indigo-500 shadow-blue-200/50',
  'from-rose-400 to-fuchsia-500 shadow-rose-200/50',
  'from-violet-400 to-purple-500 shadow-violet-200/50',
];

export default function Page() {
  const { data: booksData = [], isLoading, isError } = useProblemBooks();
  const { data: categoriesData = [] } = useCourseCategoriesQuery();
  const [selectedCategory, setSelectedCategory] = useState('all');

  const categories = booksData.map((item, index) => {
    const rawCategory = item.problemCategory || 'Untitled';

    return {
      id: item.id,
      slug: String(item.problemCategory || '')
        .toLowerCase()
        .replaceAll(' ', '-'),
      title: formatCategoryTitle(rawCategory) || 'Untitled',
      summary: item.problemDescription || '설명이 없습니다.',
      categoryName: item.problemCategory,
      isJoined: item.problemUserJoined,
      problemCount: item.problemCount || 0,
      projectCount: item.problemProjectsCount || 0,
      submissionCount: item.problemSubmissionCount || 0,
      accent: accentPalette[index % accentPalette.length],
    };
  });

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
            categories.map((category) => (
              <Link
                key={category.id}
                href={`/roadmap/${category.slug}`}
                className='group relative flex flex-col h-full bg-white rounded-[32px] border border-slate-100 shadow-[0_8px_30px_rgb(0,0,0,0.04)] transition-all duration-500 hover:-translate-y-3 hover:shadow-[0_20px_50px_rgba(79,70,229,0.15)] hover:border-indigo-100'
              >
                {/* Accent Top Glow */}
                <div
                  className={`absolute top-0 inset-x-0 h-2 bg-gradient-to-r ${category.accent} rounded-t-[32px]`}
                />

                <div className='p-8 pt-10 flex flex-col h-full'>
                  <div className='flex justify-between items-center mb-8'>
                    <div
                      className={`p-4 rounded-2xl bg-gradient-to-br ${category.accent} text-white shadow-xl`}
                    >
                      <BookOpen size={28} strokeWidth={2.5} />
                    </div>
                    {category.isJoined ? (
                      <Badge
                        variant='success'
                        className='px-4 py-1.5 rounded-full text-xs uppercase tracking-wider'
                      >
                        In Progress
                      </Badge>
                    ) : (
                      <div className='text-indigo-600 opacity-0 group-hover:opacity-100 transition-opacity translate-x-4 group-hover:translate-x-0 duration-300'>
                        <ArrowRight size={24} />
                      </div>
                    )}
                  </div>

                  <h3 className='text-2xl font-black text-slate-800 leading-tight group-hover:text-indigo-600 transition-colors mb-3'>
                    {category.title}
                  </h3>

                  <p className='text-slate-500 text-base leading-relaxed line-clamp-2 mb-8 font-medium'>
                    {category.summary}
                  </p>

                  <div className='mt-auto pt-6 border-t border-slate-50 flex items-center justify-between'>
                    <div className='space-y-1'>
                      <p className='text-[10px] font-black text-slate-400 uppercase tracking-widest'>
                        Modules
                      </p>
                      <div className='flex items-center gap-1.5'>
                        <Sparkles size={14} className='text-amber-500' />
                        <span className='text-sm font-bold text-slate-700'>
                          {category.problemCount} Problems
                        </span>
                      </div>
                    </div>
                    <div className='h-8 w-px bg-slate-100' />
                    <div className='space-y-1 text-right'>
                      <p className='text-[10px] font-black text-slate-400 uppercase tracking-widest'>
                        Community
                      </p>
                      <div className='flex items-center gap-1.5 justify-end'>
                        <Users size={14} className='text-indigo-500' />
                        <span className='text-sm font-bold text-slate-700'>
                          {category.submissionCount}+
                        </span>
                      </div>
                    </div>
                  </div>
                </div>
              </Link>
            ))
          )}
        </div>
      </div>
    </div>
  );
}
