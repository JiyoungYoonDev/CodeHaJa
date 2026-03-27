'use client';

import { useEffect, useRef, useState } from 'react';
import { useRouter } from 'next/navigation';
import { useAuth } from '@/lib/auth-context';
import { googleLoginApi } from '../../../services/auth-service';

const GOOGLE_CLIENT_ID = process.env.NEXT_PUBLIC_GOOGLE_CLIENT_ID;

export default function GoogleLoginButton({ onSuccess, onError, label = 'Google로 계속하기' }) {
  const router = useRouter();
  const { login } = useAuth();
  const btnRef = useRef(null);
  const [loading, setLoading] = useState(false);
  const [scriptReady, setScriptReady] = useState(false);

  // Load GIS script once
  useEffect(() => {
    if (!GOOGLE_CLIENT_ID || GOOGLE_CLIENT_ID === 'YOUR_GOOGLE_CLIENT_ID_HERE') return;

    if (window.google?.accounts?.id) {
      setScriptReady(true);
      return;
    }

    const script = document.createElement('script');
    script.src = 'https://accounts.google.com/gsi/client';
    script.async = true;
    script.defer = true;
    script.onload = () => setScriptReady(true);
    document.head.appendChild(script);
  }, []);

  // Initialize + render Google button when script is ready
  useEffect(() => {
    if (!scriptReady || !btnRef.current || !window.google?.accounts?.id) return;

    window.google.accounts.id.initialize({
      client_id: GOOGLE_CLIENT_ID,
      callback: handleCredentialResponse,
      auto_select: false,
      cancel_on_tap_outside: true,
    });

    // Render Google's button inside our container
    window.google.accounts.id.renderButton(btnRef.current, {
      theme: 'outline',
      size: 'large',
      width: btnRef.current.offsetWidth || 400,
      text: 'continue_with',
      logo_alignment: 'left',
    });
  }, [scriptReady]);

  async function handleCredentialResponse(response) {
    const idToken = response.credential;
    setLoading(true);
    try {
      const res = await googleLoginApi(idToken);
      login(res);
      onSuccess?.();
      router.push('/');
    } catch (err) {
      onError?.(err);
    } finally {
      setLoading(false);
    }
  }

  // No client ID configured — show disabled button
  if (!GOOGLE_CLIENT_ID || GOOGLE_CLIENT_ID === 'YOUR_GOOGLE_CLIENT_ID_HERE') {
    return (
      <button
        disabled
        className='w-full h-12 flex items-center justify-center gap-3 rounded-xl border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-800 text-sm font-medium text-slate-400 dark:text-slate-500 cursor-not-allowed opacity-60'
        title='Google Client ID가 설정되지 않았습니다'
      >
        <GoogleIcon />
        {label}
      </button>
    );
  }

  return (
    <div className='relative w-full'>
      {/* Google's rendered button — fills the container */}
      <div
        ref={btnRef}
        className='w-full overflow-hidden rounded-xl'
        style={{ minHeight: 48 }}
      />
      {/* Loading overlay */}
      {loading && (
        <div className='absolute inset-0 rounded-xl bg-white/70 dark:bg-slate-800/70 flex items-center justify-center'>
          <div className='w-5 h-5 border-2 border-indigo-500 border-t-transparent rounded-full animate-spin' />
        </div>
      )}
    </div>
  );
}

function GoogleIcon() {
  return (
    <svg width='18' height='18' viewBox='0 0 24 24'>
      <path fill='#4285F4' d='M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z' />
      <path fill='#34A853' d='M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z' />
      <path fill='#FBBC05' d='M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z' />
      <path fill='#EA4335' d='M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z' />
    </svg>
  );
}
