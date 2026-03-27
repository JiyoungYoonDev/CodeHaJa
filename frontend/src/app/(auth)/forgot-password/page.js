'use client';

import { useState } from 'react';
import Link from 'next/link';
import { Code2, ArrowLeft } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { forgotPasswordApi } from '../../../../services/auth-service';

export default function ForgotPasswordPage() {
  const [email, setEmail] = useState('');
  const [loading, setLoading] = useState(false);
  const [sent, setSent] = useState(false);
  const [error, setError] = useState('');

  async function handleSubmit(e) {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      await forgotPasswordApi(email);
      setSent(true);
    } catch {
      setError('요청 처리 중 오류가 발생했습니다. 다시 시도해주세요.');
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className='w-full max-w-md'>
      <div className='bg-white dark:bg-slate-900 rounded-3xl shadow-xl border border-slate-100 dark:border-slate-800 overflow-hidden'>
        <div className='h-1.5 w-full bg-gradient-to-r from-indigo-500 via-violet-500 to-purple-500' />
        <div className='p-8 space-y-6'>
          <Link href='/login' className='flex items-center gap-2 text-sm text-slate-400 hover:text-slate-600 dark:hover:text-slate-300 transition-colors'>
            <ArrowLeft size={16} />
            로그인으로 돌아가기
          </Link>

          <div className='flex flex-col items-center gap-2'>
            <Code2 size={32} className='text-indigo-600' />
            <h1 className='text-2xl font-black text-slate-900 dark:text-slate-50'>비밀번호 재설정</h1>
            <p className='text-sm text-slate-400 dark:text-slate-500 text-center'>
              가입한 이메일 주소를 입력하면<br />재설정 링크를 보내드립니다.
            </p>
          </div>

          {sent ? (
            <div className='text-center space-y-3 py-4'>
              <div className='text-4xl'>📬</div>
              <p className='font-bold text-slate-800 dark:text-slate-200'>이메일을 확인해주세요</p>
              <p className='text-sm text-slate-400 dark:text-slate-500'>
                {email}으로 재설정 링크를 발송했습니다.<br />스팸 메일함도 확인해보세요.
              </p>
            </div>
          ) : (
            <form onSubmit={handleSubmit} className='space-y-4'>
              <div className='space-y-1'>
                <label className='text-xs font-bold text-slate-500 dark:text-slate-400 uppercase tracking-wider'>이메일</label>
                <input
                  type='email'
                  required
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  placeholder='hello@codehaja.com'
                  className='w-full px-4 py-3 rounded-xl border border-slate-200 dark:border-slate-700 bg-slate-50 dark:bg-slate-800 text-slate-900 dark:text-slate-100 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500 placeholder:text-slate-300 dark:placeholder:text-slate-600'
                />
              </div>

              {error && (
                <p className='text-sm text-rose-500 bg-rose-50 dark:bg-rose-950/50 px-4 py-3 rounded-xl'>{error}</p>
              )}

              <Button type='submit' className='w-full h-12 text-base rounded-xl shadow-lg shadow-indigo-200 dark:shadow-indigo-950' disabled={loading}>
                {loading ? '전송 중...' : '재설정 링크 보내기'}
              </Button>
            </form>
          )}
        </div>
      </div>
    </div>
  );
}
