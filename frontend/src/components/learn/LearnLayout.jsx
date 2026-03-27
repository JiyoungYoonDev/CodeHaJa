'use client';

import { useState, useRef, useCallback, useEffect } from 'react';

const MIN_LEFT = 280;
const MIN_RIGHT = 280;
const DEFAULT_WIDTH = 460;

export default function LearnLayout({
  topBar,
  sidebar,
  leftPanel,
  rightPanel,
}) {
  const [leftWidth, setLeftWidth] = useState(DEFAULT_WIDTH);
  const [isDragging, setIsDragging] = useState(false);
  const containerRef = useRef(null);
  const startX = useRef(0);
  const startWidth = useRef(0);

  const onMouseDown = useCallback(
    (e) => {
      startX.current = e.clientX;
      startWidth.current = leftWidth;
      setIsDragging(true);
      e.preventDefault();
    },
    [leftWidth],
  );

  useEffect(() => {
    if (!isDragging) return;

    const onMouseMove = (e) => {
      const containerWidth =
        containerRef.current?.offsetWidth ?? window.innerWidth;
      const sidebarWidth = 256; // w-64
      const splitterWidth = 8; // w-2
      const maxLeft = containerWidth - sidebarWidth - splitterWidth - MIN_RIGHT;
      const delta = e.clientX - startX.current;
      const next = Math.max(
        MIN_LEFT,
        Math.min(maxLeft, startWidth.current + delta),
      );
      setLeftWidth(next);
    };

    const onMouseUp = () => setIsDragging(false);

    document.addEventListener('mousemove', onMouseMove);
    document.addEventListener('mouseup', onMouseUp);
    return () => {
      document.removeEventListener('mousemove', onMouseMove);
      document.removeEventListener('mouseup', onMouseUp);
    };
  }, [isDragging]);

  return (
    <div
      className='flex flex-col h-full'
      style={{
        background: 'var(--background)',
        userSelect: isDragging ? 'none' : undefined,
        cursor: isDragging ? 'col-resize' : undefined,
      }}
    >
      {topBar}

      <div ref={containerRef} className='flex flex-1 min-h-0'>
        {/* Sidebar */}
        {sidebar && (
          <div className='w-64 shrink-0 h-full overflow-hidden'>{sidebar}</div>
        )}

        {/* Left panel */}
        <div
          className='shrink-0 h-full overflow-hidden relative'
          style={Object.assign(
            {},
            rightPanel ? { width: leftWidth } : { flex: 1 },
            { background: 'var(--card)', color: 'var(--card-foreground)' },
          )}
        >
          <div className='h-full overflow-y-auto learn-scroll'>{leftPanel}</div>
          {/* Bottom fade hint */}
          {rightPanel && (
            <div
              className='pointer-events-none absolute bottom-0 left-0 right-0 h-12'
              style={{
                background: 'linear-gradient(to top, var(--card), transparent)',
              }}
            />
          )}
        </div>

        {/* Splitter */}
        {rightPanel && (
          <div
            onMouseDown={onMouseDown}
            className={`w-2 shrink-0 h-full flex items-center justify-center cursor-col-resize group transition-colors ${
              isDragging
                ? 'bg-violet-500/30'
                : 'bg-[#1a1a2e] hover:bg-violet-500/20'
            }`}
          >
            <div
              className={`w-0.5 h-10 rounded-full transition-all ${
                isDragging
                  ? 'bg-violet-400 h-16'
                  : 'bg-[#3a3a5e] group-hover:bg-violet-400/70 group-hover:h-14'
              }`}
            />
          </div>
        )}

        {/* Right panel */}
        {rightPanel && (
          <div
            className='flex-1 min-w-0 h-full flex flex-col'
            style={{
              background: 'var(--background)',
              color: 'var(--foreground)',
              pointerEvents: isDragging ? 'none' : undefined,
            }}
          >
            {rightPanel}
          </div>
        )}
      </div>
    </div>
  );
}
