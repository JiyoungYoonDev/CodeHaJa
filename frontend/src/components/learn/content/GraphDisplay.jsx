'use client';

import { useEffect, useRef } from 'react';

/**
 * Renders a 2D function graph using function-plot.
 * @param {{ fn1: string, fn2?: string, label1?: string, label2?: string, xDomain?: [number,number], yDomain?: [number,number] }} props
 */
export default function GraphDisplay({ fn1, fn2, label1, label2, xDomain, yDomain }) {
  const containerRef = useRef(null);

  useEffect(() => {
    if (!containerRef.current || !fn1) return;

    let cancelled = false;

    import('function-plot').then(({ default: functionPlot }) => {
      if (cancelled || !containerRef.current) return;

      const width = containerRef.current.offsetWidth || 420;
      const height = Math.round(width * 0.65);

      const data = [
        {
          fn: fn1,
          color: '#2dd4bf',
          graphType: 'polyline',
          ...(label1 ? { title: label1 } : {}),
        },
      ];

      if (fn2) {
        data.push({
          fn: fn2,
          color: '#818cf8',
          graphType: 'polyline',
          ...(label2 ? { title: label2 } : {}),
        });
      }

      try {
        // clear previous render
        containerRef.current.innerHTML = '';

        functionPlot({
          target: containerRef.current,
          width,
          height,
          xAxis: { domain: xDomain ?? [-10, 10], label: 'x' },
          yAxis: { domain: yDomain ?? [-10, 10], label: 'y' },
          grid: true,
          data,
          annotations: [],
        });
      } catch (err) {
        console.error('[GraphDisplay]', err);
      }
    });

    return () => { cancelled = true; };
  }, [fn1, fn2, xDomain, yDomain, label1, label2]);

  if (!fn1) return null;

  return (
    <div className='my-4 rounded-xl overflow-hidden border border-[#2a2a3e] bg-[#0d1117]'>
      <div className='px-3 pt-2 pb-0 flex gap-4 text-xs font-mono'>
        {label1 && <span className='text-teal-400'>— {label1}</span>}
        {label2 && fn2 && <span className='text-violet-400'>— {label2}</span>}
      </div>
      <div ref={containerRef} className='w-full [&_svg]:w-full [&_.fps-graph]:fill-transparent' />
    </div>
  );
}
