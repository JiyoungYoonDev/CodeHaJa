import { cn } from '@/lib/utils';
import { CheckCircle2, XCircle, Terminal } from 'lucide-react';

export default function CodeRunOutput({ result, mode }) {
  if (!result) {
    return (
      <div className='px-4 py-3 text-[#4a4a62] text-sm flex items-center gap-2'>
        <Terminal size={14} />
        <span>&lt;/&gt; Output</span>
      </div>
    );
  }

  if (result.submissionStatus === 'PENDING') {
    return (
      <div className='px-4 py-3 text-sm text-[#9090a8] flex items-center gap-2'>
        <span className='inline-block w-2 h-2 rounded-full bg-amber-400 animate-pulse' />
        Submission complete — processing on server.
      </div>
    );
  }

  if (mode === 'grade') {
    const passed = result.submissionStatus === 'PASSED';
    const passedCount = result.passedCount ?? 0;
    const totalCount = result.totalCount ?? 0;
    const testCaseResults = result.testCaseResults ?? [];

    return (
      <div className='px-4 py-3 space-y-2'>
        <div className='flex items-center gap-2'>
          {passed ? (
            <CheckCircle2 size={15} className='text-emerald-400' />
          ) : (
            <XCircle size={15} className='text-rose-400' />
          )}
          <span
            className={cn(
              'text-sm font-semibold',
              passed ? 'text-emerald-400' : 'text-rose-400'
            )}
          >
            {passed ? 'Correct! 🎉' : 'Incorrect'}
          </span>
          <span className='text-xs text-[#5a5a72] ml-auto'>
            {passedCount}/{totalCount} tests passed
          </span>
        </div>
        {testCaseResults.length > 0 && (
          <div className='space-y-1.5'>
            {testCaseResults.map((tc) => (
              <div
                key={tc.index}
                className={cn(
                  'flex items-start gap-2 text-xs font-mono rounded px-3 py-1.5',
                  tc.passed ? 'bg-emerald-950/30' : 'bg-rose-950/30'
                )}
              >
                <span className='shrink-0 mt-0.5'>
                  {tc.passed ? (
                    <CheckCircle2 size={12} className='text-emerald-400' />
                  ) : (
                    <XCircle size={12} className='text-rose-400' />
                  )}
                </span>
                <div className='min-w-0'>
                  <span className={cn('font-semibold', tc.passed ? 'text-emerald-300' : 'text-rose-300')}>
                    Test {tc.index}
                  </span>
                  {tc.input && (
                    <span className='text-[#6a6a82] ml-2'>Input: {tc.input}</span>
                  )}
                  {!tc.passed && (
                    <div className='mt-0.5 text-[#9090a8]'>
                      <div>Expected: <span className='text-[#c0c0d8]'>{tc.expectedOutput}</span></div>
                      <div>Got: <span className='text-rose-300'>{tc.actualOutput || '(no output)'}</span></div>
                    </div>
                  )}
                </div>
              </div>
            ))}
          </div>
        )}
        {testCaseResults.length === 0 && result.stdout && (
          <pre className='text-xs font-mono text-[#c0c0d8] bg-[#0d1117] rounded px-3 py-2 whitespace-pre-wrap max-h-32 overflow-auto'>
            {result.stdout}
          </pre>
        )}
        {result.stderr && (
          <pre className='text-xs font-mono text-rose-300 bg-[#0d1117] rounded px-3 py-2 whitespace-pre-wrap max-h-24 overflow-auto'>
            {result.stderr}
          </pre>
        )}
      </div>
    );
  }

  // Run mode
  return (
    <div className='px-4 py-3 space-y-2'>
      <div className='flex items-center gap-2 text-[#9090a8] text-xs'>
        <Terminal size={13} />
        <span>Output</span>
        {result.executionTimeMs != null && (
          <span className='ml-auto text-[#5a5a72]'>{result.executionTimeMs}ms</span>
        )}
      </div>
      {result.stdout ? (
        <pre className='text-sm font-mono text-[#c0c0d8] whitespace-pre-wrap'>
          {result.stdout}
        </pre>
      ) : (
        <p className='text-xs text-[#5a5a72]'>(No output)</p>
      )}
      {result.stderr && (
        <pre className='text-xs font-mono text-rose-300 whitespace-pre-wrap mt-1'>
          {result.stderr}
        </pre>
      )}
    </div>
  );
}
