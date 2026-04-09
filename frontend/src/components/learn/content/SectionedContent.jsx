'use client';

import { useState, useEffect, useRef, useCallback, memo } from 'react';
import { ChevronDown, List } from 'lucide-react';

/**
 * Split HTML string by h2/h3 headings into sections.
 * Returns [{ id, title, level, html }]
 * Content before the first heading goes into a preamble section.
 */
function splitByHeadings(html) {
  if (!html) return [];

  // Split on <h2> or <h3> tags
  const parts = html.split(/(?=<h[23][^>]*>)/);
  const sections = [];
  let idx = 0;

  for (const part of parts) {
    const headingMatch = part.match(/^<h([23])[^>]*>([\s\S]*?)<\/h[23]>/);
    if (headingMatch) {
      const level = parseInt(headingMatch[1]);
      // Strip any HTML tags from heading text for the TOC label
      const title = headingMatch[2].replace(/<[^>]+>/g, '').trim();
      const body = part.slice(headingMatch[0].length);
      sections.push({ id: `sec-${idx}`, title, level, html: part, bodyHtml: body });
      idx++;
    } else if (part.trim()) {
      // Preamble content before first heading
      sections.push({ id: `sec-preamble`, title: '', level: 0, html: part, bodyHtml: part });
      idx++;
    }
  }

  return sections;
}

export default function SectionedContent({ html, contentRef, itemId, onReveal }) {
  const sections = splitByHeadings(html);
  const hasHeadings = sections.filter(s => s.level > 0).length >= 2;

  // If fewer than 2 headings, just render normally
  if (!hasHeadings) {
    return (
      <div
        ref={contentRef}
        className='mt-4 tiptap-content'
        dangerouslySetInnerHTML={{ __html: html }}
      />
    );
  }

  return <SectionedView sections={sections} contentRef={contentRef} itemId={itemId} onReveal={onReveal} />;
}

/**
 * Memoised section wrapper — only re-renders when `hidden` changes.
 * Prevents React from walking dangerouslySetInnerHTML diffs on every visibleCount bump.
 */
const SectionSlot = memo(function SectionSlot({ sec, hidden, onRef }) {
  const refCb = useCallback((el) => onRef(sec.id, el), [sec.id, onRef]);
  return (
    <div
      ref={refCb}
      data-sec-id={sec.id}
      className={hidden ? 'hidden' : undefined}
      dangerouslySetInnerHTML={{ __html: sec.html }}
    />
  );
}, (prev, next) => prev.hidden === next.hidden && prev.sec === next.sec);

function SectionedView({ sections, contentRef, itemId, onReveal }) {
  // Progressive reveal: start with preamble + first heading section
  const firstHeadingIdx = sections.findIndex(s => s.level > 0);
  const initialCount = firstHeadingIdx >= 0 ? firstHeadingIdx + 2 : 2;
  const [visibleCount, setVisibleCount] = useState(Math.min(initialCount, sections.length));
  const [activeId, setActiveId] = useState(sections[0]?.id ?? '');
  const [tocOpen, setTocOpen] = useState(false);
  const sectionRefs = useRef({});
  const observerRef = useRef(null);

  const allRevealed = visibleCount >= sections.length;

  // Reset when item changes
  useEffect(() => {
    const idx = sections.findIndex(s => s.level > 0);
    setVisibleCount(Math.min(idx >= 0 ? idx + 2 : 2, sections.length));
    setActiveId(sections[0]?.id ?? '');
    setTocOpen(false);
  }, [itemId]);

  // Stable intersection observer — created once, observes elements via ref callback
  useEffect(() => {
    const observer = new IntersectionObserver(
      (entries) => {
        for (const entry of entries) {
          if (entry.isIntersecting) {
            setActiveId(entry.target.dataset.secId);
          }
        }
      },
      { rootMargin: '-20% 0px -60% 0px', threshold: 0 }
    );
    observerRef.current = observer;
    // Observe any elements already registered
    Object.values(sectionRefs.current).forEach((el) => {
      if (el) observer.observe(el);
    });
    return () => { observer.disconnect(); observerRef.current = null; };
  }, [itemId]);

  // Stable ref callback — registers element with observer without recreating functions
  const setSectionRef = useCallback((id, el) => {
    const prev = sectionRefs.current[id];
    if (prev === el) return;
    if (prev && observerRef.current) observerRef.current.unobserve(prev);
    sectionRefs.current[id] = el;
    if (el && observerRef.current) observerRef.current.observe(el);
  }, []);

  const scrollTo = useCallback((id) => {
    sectionRefs.current[id]?.scrollIntoView({ behavior: 'smooth', block: 'start' });
    setActiveId(id);
    setTocOpen(false);
  }, []);

  function revealNext() {
    setVisibleCount((c) => Math.min(c + 1, sections.length));
    setTimeout(() => onReveal?.(), 50);
  }

  function revealAll() {
    setVisibleCount(sections.length);
    setTimeout(() => onReveal?.(), 50);
  }

  const headingSections = sections.filter(s => s.level > 0);

  return (
    <div className='mt-4 flex gap-6 relative'>
      {/* Main content */}
      <div ref={contentRef} className='tiptap-content flex-1 min-w-0'>
        {sections.map((sec, i) => (
          <div key={sec.id}>
            {sec.level > 0 && i > 0 && i < visibleCount && (
              <hr className='my-10 border-0 border-t border-[#2a2a3e]' />
            )}
            <SectionSlot
              sec={sec}
              hidden={i >= visibleCount}
              onRef={setSectionRef}
            />
          </div>
        ))}

        {/* Continue Reading button */}
        {!allRevealed && (
          <div className='mt-6 flex items-center gap-3'>
            <button
              onClick={revealNext}
              className='flex items-center gap-2 px-5 py-2.5 rounded-xl
                         bg-violet-600/15 border border-violet-500/25
                         text-violet-300 text-sm font-semibold
                         hover:bg-violet-600/25 hover:border-violet-500/40
                         transition-all duration-200'
            >
              <ChevronDown size={15} />
              Continue Reading
            </button>
            <button
              onClick={revealAll}
              className='text-xs text-[#5a5a72] hover:text-[#9090a8] transition-colors'
            >
              Show all
            </button>
          </div>
        )}
      </div>

      {/* Desktop TOC — sticky right sidebar */}
      <aside className='hidden xl:block w-52 shrink-0'>
        <nav className='sticky top-20'>
          <p className='text-[10px] font-black uppercase tracking-[0.15em] text-[#5a5a72] mb-3'>
            On this page
          </p>
          <ul className='space-y-0.5 border-l border-[#1e1e30]'>
            {headingSections.map((sec, i) => {
              const sectionIdx = sections.indexOf(sec);
              const isRevealed = sectionIdx < visibleCount;
              const isActive = activeId === sec.id;
              return (
                <li key={sec.id}>
                  <button
                    onClick={() => {
                      if (!isRevealed) {
                        setVisibleCount(sectionIdx + 1);
                        setTimeout(() => scrollTo(sec.id), 50);
                      } else {
                        scrollTo(sec.id);
                      }
                    }}
                    className={`block w-full text-left text-xs leading-relaxed py-1 transition-all duration-200
                      ${sec.level === 3 ? 'pl-5' : 'pl-3'}
                      ${isActive && isRevealed
                        ? 'text-violet-300 border-l-2 border-violet-500 -ml-px font-semibold'
                        : isRevealed
                          ? 'text-[#8080a0] hover:text-white'
                          : 'text-[#3a3a52] cursor-pointer'
                      }
                    `}
                  >
                    {sec.title}
                    {!isRevealed && <span className='ml-1 text-[#3a3a52]'>›</span>}
                  </button>
                </li>
              );
            })}
          </ul>
        </nav>
      </aside>

      {/* Mobile TOC — floating button + dropdown */}
      <div className='xl:hidden fixed bottom-20 right-4 z-40'>
        {tocOpen && (
          <div className='absolute bottom-12 right-0 w-64 max-h-72 overflow-y-auto
                          bg-[#12121e] border border-[#2a2a3e] rounded-xl shadow-2xl p-3
                          animate-in fade-in slide-in-from-bottom-2 duration-200'>
            <p className='text-[10px] font-black uppercase tracking-[0.15em] text-[#5a5a72] mb-2 px-1'>
              On this page
            </p>
            <ul className='space-y-0.5'>
              {headingSections.map((sec) => {
                const sectionIdx = sections.indexOf(sec);
                const isRevealed = sectionIdx < visibleCount;
                const isActive = activeId === sec.id;
                return (
                  <li key={sec.id}>
                    <button
                      onClick={() => {
                        if (!isRevealed) {
                          setVisibleCount(sectionIdx + 1);
                          setTimeout(() => scrollTo(sec.id), 50);
                        } else {
                          scrollTo(sec.id);
                        }
                      }}
                      className={`block w-full text-left text-xs py-1.5 px-2 rounded-lg transition-all
                        ${sec.level === 3 ? 'pl-5' : 'pl-2'}
                        ${isActive && isRevealed
                          ? 'text-violet-300 bg-violet-500/10 font-semibold'
                          : isRevealed
                            ? 'text-[#9090a8] hover:bg-[#1e1e30] hover:text-white'
                            : 'text-[#3a3a52]'
                        }
                      `}
                    >
                      {sec.title}
                    </button>
                  </li>
                );
              })}
            </ul>
          </div>
        )}
        <button
          onClick={() => setTocOpen(v => !v)}
          className={`w-10 h-10 rounded-full flex items-center justify-center shadow-lg transition-all
            ${tocOpen
              ? 'bg-violet-600 text-white'
              : 'bg-[#1a1a2e] border border-[#2a2a3e] text-[#9090a8] hover:text-white hover:border-violet-500/40'
            }`}
        >
          <List size={16} />
        </button>
      </div>
    </div>
  );
}
