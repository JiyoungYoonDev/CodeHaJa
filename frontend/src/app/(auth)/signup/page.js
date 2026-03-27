'use client';

import { useState } from 'react';
import { useRouter } from 'next/navigation';
import Link from 'next/link';
import { Code2, Eye, EyeOff } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { signupApi } from '../../../../services/auth-service';
import { useAuth } from '@/lib/auth-context';
import GoogleLoginButton from '@/components/common/GoogleLoginButton';

export default function SignupPage() {
  const router = useRouter();
  const { login } = useAuth();
  const [form, setForm] = useState({ email: '', password: '', name: '' });
  const [showPw, setShowPw] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  async function handleSubmit(e) {
    e.preventDefault();
    setError('');
    if (form.password.length < 8) {
      setError('비밀번호는 8자 이상이어야 합니다.');
      return;
    }
    setLoading(true);
    try {
      const res = await signupApi(form);
      login(res);
      router.push('/');
    } catch (err) {
      setError('이미 사용 중인 이메일이거나 가입에 실패했습니다.');
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className='w-full max-w-md'>
      <div className='bg-white dark:bg-slate-900 rounded-3xl shadow-xl border border-slate-100 dark:border-slate-800 overflow-hidden'>
        <div className='h-1.5 w-full bg-gradient-to-r from-indigo-500 via-violet-500 to-purple-500' />
        <div className='p-8 space-y-6'>
          <div className='flex flex-col items-center gap-2'>
            <Code2 size={32} className='text-indigo-600' />
            <h1 className='text-2xl font-black text-slate-900 dark:text-slate-50'>지금 시작해보세요!</h1>
            <p className='text-sm text-slate-400 dark:text-slate-500'>무료로 가입하고 바로 학습 시작</p>
          </div>

          <form onSubmit={handleSubmit} className='space-y-4'>
            <div className='space-y-1'>
              <label className='text-xs font-bold text-slate-500 dark:text-slate-400 uppercase tracking-wider'>이름</label>
              <input
                type='text'
                required
                value={form.name}
                onChange={(e) => setForm((f) => ({ ...f, name: e.target.value }))}
                placeholder='홍길동'
                className='w-full px-4 py-3 rounded-xl border border-slate-200 dark:border-slate-700 bg-slate-50 dark:bg-slate-800 text-slate-900 dark:text-slate-100 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500 placeholder:text-slate-300 dark:placeholder:text-slate-600'
              />
            </div>

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
                  placeholder='8자 이상'
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
            </div>

            {error && (
              <p className='text-sm text-rose-500 bg-rose-50 dark:bg-rose-950/50 px-4 py-3 rounded-xl'>{error}</p>
            )}

            <Button type='submit' className='w-full h-12 text-base rounded-xl shadow-lg shadow-indigo-200 dark:shadow-indigo-950' disabled={loading}>
              {loading ? '가입 중...' : '무료로 시작하기'}
            </Button>
          </form>

          <div className='flex items-center gap-3'>
            <div className='flex-1 h-px bg-slate-100 dark:bg-slate-800' />
            <span className='text-xs text-slate-400'>또는</span>
            <div className='flex-1 h-px bg-slate-100 dark:bg-slate-800' />
          </div>

          <GoogleLoginButton label='Google로 가입하기' />

          <p className='text-center text-sm text-slate-400 dark:text-slate-500'>
            이미 계정이 있으신가요?{' '}
            <Link href='/login' className='font-bold text-indigo-600 dark:text-indigo-400 hover:underline'>
              로그인
            </Link>
          </p>
        </div>
      </div>
    </div>
  );
}
