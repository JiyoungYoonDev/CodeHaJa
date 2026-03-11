'use client';

import Link from 'next/link';

const roadmapData = [
  {
    id: 1,
    category: 'java',
    title: 'Java 시작하기',
    status: 'completed',
    level: 1,
    summary: 'JDK 설정과 첫 Hello World',
  },
  {
    id: 2,
    category: 'java',
    title: '변수와 자료형',
    status: 'current',
    level: 1,
    summary: '기본형과 참조형의 차이 이해',
  },
  {
    id: 3,
    category: 'java',
    title: '제어문 마스터',
    status: 'locked',
    level: 2,
    summary: 'for, while, if 자유자재로 쓰기',
  },
  {
    id: 4,
    category: 'java',
    title: '클래스와 객체',
    status: 'locked',
    level: 3,
    summary: '붕어빵틀(Class) 만들기',
  },
];

export default function Page({ params }) {
  const { problemCategory } = params;

  console.log('로드맵 데이터:', problemCategory);
  return (
    <div className='min-h-screen bg-slate-50 py-12 px-4'>
      <div className='max-w-2xl mx-auto'>
        <header className='text-center mb-16'>
          <h1 className='text-4xl font-extrabold text-slate-900 mb-4'>
            Java Zero to Hero
          </h1>
          <p className='text-slate-600'>
            단계별 퀴즈를 풀며 자바 마스터로 거듭나세요.
          </p>
        </header>

        <div className='relative'>
          <div className='absolute left-8 top-0 bottom-0 w-1 bg-slate-200 -z-10' />

          <div className='space-y-12'>
            {roadmapData.map((step) => (
              <div key={step.id} className='flex items-start gap-6 group'>
                <div
                  className={`w-16 h-16 rounded-full flex items-center justify-center shrink-0 border-4 ${
                    step.status === 'completed'
                      ? 'bg-green-500 border-green-100 text-white'
                      : step.status === 'current'
                        ? 'bg-blue-600 border-blue-100 text-white shadow-lg shadow-blue-200'
                        : 'bg-white border-slate-100 text-slate-300'
                  }`}
                >
                  {step.status === 'completed' ? '✓' : step.id}
                </div>

                <Link
                  href={
                    step.status === 'locked'
                      ? '#'
                      : `/roadmap/${step.category}/${step.id}`
                  }
                  className={`flex-1 p-6 rounded-2xl border transition-all ${
                    step.status === 'locked'
                      ? 'bg-slate-50 border-slate-200 opacity-60 cursor-not-allowed'
                      : 'bg-white border-white shadow-sm hover:shadow-md hover:-translate-y-1 cursor-pointer'
                  }`}
                >
                  <div className='flex justify-between items-start mb-2'>
                    <span className='text-xs font-bold uppercase tracking-wider text-blue-500'>
                      Level {step.level}
                    </span>
                    {step.status === 'locked' && <span>🔒</span>}
                  </div>
                  <h3 className='text-xl font-bold text-slate-800 mb-2'>
                    {step.title}
                  </h3>
                  <p className='text-slate-500 text-sm'>{step.summary}</p>
                </Link>
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
}
