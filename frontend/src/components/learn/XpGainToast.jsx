'use client';

import { useEffect, useState } from 'react';

/**
 * Floating "+N XP" animation that appears briefly then fades away.
 * Usage: <XpGainToast xp={50} key={toastKey} />
 * Remount with a new key to re-trigger.
 */
export default function XpGainToast({ xp }) {
  const [visible, setVisible] = useState(true);

  useEffect(() => {
    const t = setTimeout(() => setVisible(false), 1800);
    return () => clearTimeout(t);
  }, []);

  if (!xp || !visible) return null;

  return (
    <div
      className='fixed bottom-24 left-1/2 -translate-x-1/2 z-50 pointer-events-none'
      style={{ animation: 'xpFloat 1.8s ease-out forwards' }}
    >
      <div className='flex items-center gap-1.5 px-4 py-2 rounded-full bg-violet-600/90 text-white font-bold text-sm shadow-lg shadow-violet-900/50'>
        <span className='text-yellow-300'>⚡</span>
        +{xp} XP
      </div>
      <style>{`
        @keyframes xpFloat {
          0%   { opacity: 0; transform: translateX(-50%) translateY(0px); }
          15%  { opacity: 1; transform: translateX(-50%) translateY(-8px); }
          70%  { opacity: 1; transform: translateX(-50%) translateY(-28px); }
          100% { opacity: 0; transform: translateX(-50%) translateY(-44px); }
        }
      `}</style>
    </div>
  );
}
