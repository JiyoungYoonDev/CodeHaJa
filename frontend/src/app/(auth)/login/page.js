'use client';

import { Suspense, useState } from 'react';
import { useRouter, useSearchParams } from 'next/navigation';
import Link from 'next/link';
import { Code2, Eye, EyeOff } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { loginApi } from '../../../../services/auth-service';
import { useAuth } from '@/lib/auth-context';
import GoogleLoginButton from '@/components/common/GoogleLoginButton';

function LoginPageInner() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const from = searchParams.get('from');
  const { login } = useAuth();
  const [form, setForm] = useState({ email: '', password: '' });
  const [showPw, setShowPw] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  async function handleSubmit(e) {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      const res = await loginApi(form);
      login(res);
      const dest = from && from.startsWith('/') ? from : '/';
      router.push(dest);
    } catch (err) {
      setError('이메일 또는 비밀번호가 올바르지 않습니다.');
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className='w-full max-w-md'>
      <div className='bg-white dark:bg-slate-900 rounded-3xl shadow-xl border border-slate-100 dark:border-slate-800 overflow-hidden'>
        <div className='h-1.5 w-full bg-gradient-to-r from-indigo-500 via-violet-500 to-purple-500' />
        <div className='p-8 space-y-6'>
          {/* Logo */}
          <div className='flex flex-col items-center gap-2'>
            <Code2 size={32} className='text-indigo-600' />
            <h1 className='text-2xl font-black text-slate-900 dark:text-slate-50'>다시 만나서 반가워요!</h1>
            <p className='text-sm text-slate-400 dark:text-slate-500'>계속 학습을 이어가세요</p>
          </div>

          {/* Form */}
          <form onSubmit={handleSubmit} className='space-y-4'>
            <div className='space-y-1'>
              <label className='text-xs font-bold text-slate-500 dark:text-slate-400 uppercase tracking-wider'>이메일</label>
              <input
                type='email'
                required
                value={form.email}
                onChange={(e) => setForm((f) => ({ ...f, email: e.target.value }))}
                placeholder='hello@codehaja.com'
                className='w-full px-4 py-3 rounded-xl border border-slate-200 dark:border-slate-700 bg-slate-50 dark:bg-slate-800 text-slate-900 dark:text-slate-100 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500 placeholder:text-slate-300 dark:placeholder:text-slate-600'
              />
            </div>

            <div className='space-y-1'>
              <label className='text-xs font-bold text-slate-500 dark:text-slate-400 uppercase tracking-wider'>비밀번호</label>
              <div className='relative'>
                <input
                  type={showPw ? 'text' : 'password'}
                  required
                  value={form.password}
                  onChange={(e) => setForm((f) => ({ ...f, password: e.target.value }))}
                  placeholder='••••••••'
                  className='w-full px-4 py-3 pr-11 rounded-xl border border-slate-200 dark:border-slate-700 bg-slate-50 dark:bg-slate-800 text-slate-900 dark:text-slate-100 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500 placeholder:text-slate-300 dark:placeholder:text-slate-600'
                />
                <button
                  type='button'
                  onClick={() => setShowPw((v) => !v)}
                  className='absolute right-3 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-600'
                >
                  {showPw ? <EyeOff size={16} /> : <Eye size={16} />}
                </button>
              </div>
              <div className='flex justify-end'>
                <Link href='/forgot-password' className='text-xs text-indigo-600 dark:text-indigo-400 hover:underline'>
                  비밀번호를 잊으셨나요?
                </Link>
              </div>
            </div>

            {error && (
              <p className='text-sm text-rose-500 bg-rose-50 dark:bg-rose-950/50 px-4 py-3 rounded-xl'>{error}</p>
            )}

            <Button type='submit' className='w-full h-12 text-base rounded-xl shadow-lg shadow-indigo-200 dark:shadow-indigo-950' disabled={loading}>
              {loading ? '로그인 중...' : '로그인'}
            </Button>
          </form>

          {/* Divider */}
          <div className='flex items-center gap-3'>
            <div className='flex-1 h-px bg-slate-100 dark:bg-slate-800' />
            <span className='text-xs text-slate-400'>또는</span>
            <div className='flex-1 h-px bg-slate-100 dark:bg-slate-800' />
          </div>

          {/* Google */}
          <GoogleLoginButton label='Google로 로그인' />

          <p className='text-center text-sm text-slate-400 dark:text-slate-500'>
            계정이 없으신가요?{' '}
            <Link href='/signup' className='font-bold text-indigo-600 dark:text-indigo-400 hover:underline'>
              무료로 가입하기
            </Link>
          </p>
        </div>
      </div>
    </div>
  );
}

export default function LoginPage() {
  return (
    <Suspense>
      <LoginPageInner />
    </Suspense>
  );
}
