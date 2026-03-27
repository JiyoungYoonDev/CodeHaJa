'use client';

import { useEffect, useRef } from 'react';
import Link from 'next/link';
import { X } from 'lucide-react';
import { Button } from '@/components/ui/button';
import GoogleLoginButton from './GoogleLoginButton';

export default function AuthRequiredModal({ open, onClose }) {
  const overlayRef = useRef(null);

  useEffect(() => {
    if (!open) return;
    const handleKey = (e) => { if (e.key === 'Escape') onClose(); };
    document.addEventListener('keydown', handleKey);
    return () => document.removeEventListener('keydown', handleKey);
  }, [open, onClose]);

  useEffect(() => {
    if (open) document.body.style.overflow = 'hidden';
    else document.body.style.overflow = '';
    return () => { document.body.style.overflow = ''; };
  }, [open]);

  if (!open) return null;

  return (
    <div
      ref={overlayRef}
      className='fixed inset-0 z-[200] flex items-center justify-center p-4'
      onClick={(e) => { if (e.target === overlayRef.current) onClose(); }}
    >
      {/* Backdrop */}
      <div className='absolute inset-0 bg-black/50 backdrop-blur-sm' />

      {/* Card */}
      <div
        role='dialog'
        aria-modal='true'
        className='relative z-10 w-full max-w-md bg-white dark:bg-slate-900 rounded-3xl shadow-2xl overflow-hidden'
      >
        {/* Close button */}
        <button
          onClick={onClose}
          className='absolute top-5 right-5 p-1.5 rounded-full hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors'
          aria-label='Close'
        >
          <X size={18} className='text-slate-400' />
        </button>

        {/* Visual accent top bar */}
        <div className='h-1.5 w-full bg-gradient-to-r from-indigo-500 via-violet-500 to-purple-500' />

        {/* Content */}
        <div className='px-8 pt-8 pb-10 space-y-6'>
          {/* Icon */}
          <div className='flex justify-center'>
            <div className='w-16 h-16 rounded-2xl bg-indigo-50 dark:bg-indigo-950/60 flex items-center justify-center'>
              <span className='text-3xl'>🎓</span>
            </div>
          </div>

          {/* Headline */}
          <div className='text-center space-y-2'>
            <h2 className='text-2xl font-black text-slate-900 dark:text-slate-50 leading-tight'>
              Sign up to access courses, quizzes,
              <br />and hands-on exercises
            </h2>
            <p className='text-sm text-slate-500 dark:text-slate-400 leading-relaxed'>
              Start for free and build your skills with a structured curriculum.
            </p>
          </div>

          {/* CTAs */}
          <div className='space-y-3'>
            <Button className='w-full h-12 text-base rounded-xl shadow-lg shadow-indigo-200 dark:shadow-indigo-950' asChild>
              <Link href='/signup' onClick={onClose}>Start for Free</Link>
            </Button>

            {/* Google */}
            <GoogleLoginButton label='Continue with Google' onSuccess={onClose} />

            <div className='text-center'>
              <span className='text-sm text-slate-400 dark:text-slate-500'>Already have an account? </span>
              <Link
                href='/login'
                onClick={onClose}
                className='text-sm font-bold text-indigo-600 dark:text-indigo-400 hover:underline'
              >
                Log In
              </Link>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
