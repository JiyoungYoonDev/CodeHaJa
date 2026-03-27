'use client';

import { Save, History, Sparkles } from 'lucide-react';

export default function CodeEditorToolbar({ onSave }) {
  return (
    <div className='flex items-center justify-end px-4 py-2 bg-[#0e0e1a] border-t border-[#1e1e2e] gap-2'>
      <button
        onClick={onSave}
        title='Save'
        className='p-2 rounded-lg text-[#5a5a72] hover:text-[#9090a8] hover:bg-[#1a1a2e] transition-all'
      >
        <Save size={15} />
      </button>
      <button
        title='Submission History'
        className='p-2 rounded-lg text-[#5a5a72] hover:text-[#9090a8] hover:bg-[#1a1a2e] transition-all'
      >
        <History size={15} />
      </button>
      <button className='flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-semibold border border-violet-500/40 text-violet-400 hover:bg-violet-600/10 transition-all'>
        <Sparkles size={13} />
        AI Code Review
      </button>
    </div>
  );
}
