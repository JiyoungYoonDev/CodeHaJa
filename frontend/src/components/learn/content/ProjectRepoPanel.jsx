'use client';

import { useState } from 'react';
import { Github, Globe, FileCode, Type, StickyNote, CheckCircle2, Loader2 } from 'lucide-react';

// ─── helpers ──────────────────────────────────────────────────────────────────

function isValidUrl(str) {
  try { new URL(str); return true; } catch { return false; }
}

const TYPE_META = {
  github_url:   { icon: Github,     placeholder: 'https://github.com/username/repo',      isUrl: true,  multiline: false },
  demo_url:     { icon: Globe,      placeholder: 'https://your-project.vercel.app',        isUrl: true,  multiline: false },
  code_snippet: { icon: FileCode,   placeholder: 'Paste your key implementation here...', isUrl: false, multiline: true  },
  text:         { icon: Type,       placeholder: 'Your answer...',                          isUrl: false, multiline: false },
  note:         { icon: StickyNote, placeholder: 'Write your response here...',            isUrl: false, multiline: true  },
};

// Migrate old boolean-flag format → array
function normaliseFields(contentJson) {
  const raw = contentJson?.fields;
  if (!raw) return [];
  if (Array.isArray(raw)) return raw;
  // Old format: { repoRequired, demoRequired, snippetRequired, noteLabel }
  const result = [];
  if (raw.repoRequired !== false) result.push({ id: 'repo',    type: 'github_url',   label: 'GitHub Repository URL', required: !!raw.repoRequired   });
  if (raw.demoRequired || raw.demoOptional) result.push({ id: 'demo', type: 'demo_url', label: 'Live Demo URL',       required: !!raw.demoRequired   });
  if (raw.snippetRequired) result.push({ id: 'snippet', type: 'code_snippet', label: 'Key Code Snippet',             required: true                  });
  if (raw.noteLabel)       result.push({ id: 'note',    type: 'note',         label: raw.noteLabel,                  required: false                 });
  return result;
}

// ─── component ────────────────────────────────────────────────────────────────

export default function ProjectRepoPanel({ contentJson, onSubmit, isCompleted, isSubmitting }) {
  const fields = normaliseFields(contentJson);
  const [values, setValues] = useState(() => Object.fromEntries(fields.map((f) => [f.id, ''])));
  const [errors, setErrors] = useState({});

  function validate() {
    const e = {};
    fields.forEach((f) => {
      const val = (values[f.id] ?? '').trim();
      if (f.required && !val) {
        e[f.id] = `${f.label || 'This field'} is required.`;
      } else if (val && (f.type === 'github_url' || f.type === 'demo_url') && !isValidUrl(val)) {
        e[f.id] = 'Enter a valid URL.';
      }
    });
    return e;
  }

  function handleSubmit() {
    const e = validate();
    if (Object.keys(e).length > 0) { setErrors(e); return; }
    setErrors({});
    const payload = Object.fromEntries(fields.map((f) => [f.type, values[f.id]]));
    onSubmit?.(payload);
  }

  if (isCompleted) {
    return (
      <div className='flex flex-col items-center justify-center h-full gap-4 text-center px-8'>
        <CheckCircle2 size={48} className='text-emerald-400' />
        <p className='text-lg font-bold text-white'>Project Submitted!</p>
        <p className='text-sm text-[#9090a8]'>Your submission has been recorded.</p>
      </div>
    );
  }

  if (fields.length === 0) {
    return (
      <div className='flex flex-col items-center justify-center h-full gap-2 text-center px-8'>
        <p className='text-sm text-[#5a5a72]'>No submission fields configured.</p>
      </div>
    );
  }

  return (
    <div className='h-full overflow-y-auto px-6 py-6 space-y-5'>
      <p className='text-xs font-black uppercase tracking-widest text-[#5a5a72]'>Project Submission</p>

      {fields.map((field) => {
        const meta = TYPE_META[field.type] ?? TYPE_META.text;
        const Icon = meta.icon;
        const val = values[field.id] ?? '';
        const err = errors[field.id];

        return (
          <div key={field.id} className='space-y-1.5'>
            <label className='flex items-center gap-1.5 text-xs font-semibold text-[#9090a8]'>
              <Icon size={12} />
              {field.label || field.type}
              {field.required && <span className='text-rose-400 ml-0.5'>*</span>}
            </label>
            {meta.multiline ? (
              <textarea
                placeholder={meta.placeholder}
                value={val}
                onChange={(e) => setValues((prev) => ({ ...prev, [field.id]: e.target.value }))}
                rows={field.type === 'code_snippet' ? 6 : 4}
                className='w-full bg-[#1a1a2e] border border-[#2a2a3e] rounded-lg px-4 py-2.5 text-sm text-white placeholder-[#5a5a72] focus:outline-none focus:border-violet-500 transition-colors resize-none font-mono'
              />
            ) : (
              <input
                type={meta.isUrl ? 'url' : 'text'}
                placeholder={meta.placeholder}
                value={val}
                onChange={(e) => setValues((prev) => ({ ...prev, [field.id]: e.target.value }))}
                className='w-full bg-[#1a1a2e] border border-[#2a2a3e] rounded-lg px-4 py-2.5 text-sm text-white placeholder-[#5a5a72] focus:outline-none focus:border-violet-500 transition-colors font-mono'
              />
            )}
            {err && <p className='text-xs text-rose-400'>{err}</p>}
          </div>
        );
      })}

      <button
        onClick={handleSubmit}
        disabled={isSubmitting}
        className='w-full flex items-center justify-center gap-2 py-3 rounded-xl bg-violet-600 hover:bg-violet-500 disabled:opacity-50 disabled:cursor-not-allowed text-white font-bold text-sm transition-colors'
      >
        {isSubmitting ? <Loader2 size={15} className='animate-spin' /> : null}
        {isSubmitting ? 'Submitting...' : 'Submit Project'}
      </button>
    </div>
  );
}
