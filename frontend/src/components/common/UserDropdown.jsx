'use client';

import { useState } from 'react';
import { useRouter } from 'next/navigation';
import { useAuth } from '@/lib/auth-context';
import { LogOut, User } from 'lucide-react';

export default function UserDropdown() {
  const { user, logout } = useAuth();
  const router = useRouter();
  const [open, setOpen] = useState(false);

  async function handleLogout() {
    await logout();
    router.push('/');
  }

  return (
    <div className='relative'>
      <button
        onClick={() => setOpen((v) => !v)}
        className='flex items-center gap-2 px-3 py-1.5 rounded-xl hover:bg-accent transition-colors'
      >
        {user?.profileImage ? (
          <img
            src={user.profileImage}
            alt={user.name}
            referrerPolicy='no-referrer'
            width={32}
            height={32}
            className='w-8 h-8 rounded-full object-cover'
          />
        ) : (
          <div className='w-8 h-8 rounded-full bg-indigo-100 dark:bg-indigo-900 flex items-center justify-center'>
            <User size={16} className='text-indigo-600 dark:text-indigo-400' />
          </div>
        )}
        <span className='text-sm font-medium text-foreground hidden md:block max-w-[120px] truncate'>
          {user?.name ?? user?.email}
        </span>
      </button>

      {open && (
        <>
          <div className='fixed inset-0 z-10' onClick={() => setOpen(false)} />
          <div className='absolute right-0 mt-2 w-52 bg-white dark:bg-slate-900 border border-border rounded-2xl shadow-lg z-20 overflow-hidden'>
            <div className='px-4 py-3 border-b border-border'>
              <p className='text-sm font-bold text-foreground truncate'>{user?.name}</p>
              <p className='text-xs text-muted-foreground truncate'>{user?.email}</p>
            </div>
            <button
              onClick={handleLogout}
              className='w-full flex items-center gap-3 px-4 py-3 text-sm text-slate-600 dark:text-slate-400 hover:bg-slate-50 dark:hover:bg-slate-800 transition-colors'
            >
              <LogOut size={15} />
              Logout
            </button>
          </div>
        </>
      )}
    </div>
  );
}
