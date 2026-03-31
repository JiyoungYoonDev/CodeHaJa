'use client';

import { useRef } from 'react';

const ROWS = [
  [
    { label: 'x²',  insert: '²' },
    { label: 'xⁿ',  insert: '^' },
    { label: '√',   insert: '√' },
    { label: '+',   insert: '+' },
    { label: '−',   insert: '−' },
  ],
  [
    { label: '½',   insert: '/' },
    { label: '·',   insert: '·' },
    { label: '(',   insert: '(' },
    { label: ')',   insert: ')' },
    { label: '⌫',   action: 'backspace' },
  ],
  [
    { label: 'π',   insert: 'π' },
    { label: '∞',   insert: '∞' },
    { label: '≠',   insert: '≠' },
    { label: '≤',   insert: '≤' },
    { label: '≥',   insert: '≥' },
  ],
];

/**
 * Math keyboard — inserts symbols into the target input element.
 * @param {object} props
 * @param {React.RefObject<HTMLInputElement>} props.inputRef — ref to the input to type into
 * @param {(val: string) => void} props.onChange — called with the new full value
 * @param {string} props.value — current input value
 */
export default function MathKeyboard({ inputRef, value, onChange }) {
  function insert(symbol) {
    const el = inputRef?.current;
    if (!el) {
      onChange(value + symbol);
      return;
    }
    const start = el.selectionStart ?? value.length;
    const end = el.selectionEnd ?? value.length;
    const next = value.slice(0, start) + symbol + value.slice(end);
    onChange(next);
    // restore cursor after React re-render
    requestAnimationFrame(() => {
      el.focus();
      el.setSelectionRange(start + symbol.length, start + symbol.length);
    });
  }

  function backspace() {
    const el = inputRef?.current;
    if (!el) {
      onChange(value.slice(0, -1));
      return;
    }
    const start = el.selectionStart ?? value.length;
    const end = el.selectionEnd ?? value.length;
    let next;
    if (start !== end) {
      next = value.slice(0, start) + value.slice(end);
    } else if (start > 0) {
      next = value.slice(0, start - 1) + value.slice(start);
    } else {
      next = value;
    }
    onChange(next);
    requestAnimationFrame(() => {
      el.focus();
      const pos = Math.max(0, start === end ? start - 1 : start);
      el.setSelectionRange(pos, pos);
    });
  }

  return (
    <div className='mt-1 p-2 bg-[#1a1a2e] border border-[#2a2a3e] rounded-lg shadow-lg w-fit'>
      {ROWS.map((row, ri) => (
        <div key={ri} className='flex gap-1 mb-1 last:mb-0'>
          {row.map((key) => (
            <button
              key={key.label}
              type='button'
              onMouseDown={(e) => {
                e.preventDefault(); // prevent input blur
                key.action === 'backspace' ? backspace() : insert(key.insert);
              }}
              className='w-10 h-9 flex items-center justify-center rounded bg-[#2a2a3e] hover:bg-[#3a3a5e] text-white text-sm font-mono font-bold transition-colors select-none'
            >
              {key.label}
            </button>
          ))}
        </div>
      ))}
    </div>
  );
}
