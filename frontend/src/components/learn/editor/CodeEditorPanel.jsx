'use client';

import { useState, useEffect } from 'react';
import CodeEditorFileTree from './CodeEditorFileTree';
import MonacoEditorWrapper from './MonacoEditorWrapper';
import CodeEditorToolbar from './CodeEditorToolbar';
import CodeRunOutput from './CodeRunOutput';
import InteractiveTerminal from './InteractiveTerminal';

export default function CodeEditorPanel({
  item,
  currentProblem,
  problemFiles = [],
  filesContent = {},
  activeFile = null,
  onActiveFileChange,
  onCodeChange,
  runResult,
  outputMode,
  isRunning = false,
  onRunExit,
}) {
  const source = currentProblem ?? item?.contentJson ?? null;
  const language = source?.language ?? 'python';
  const folderName = item?.title ?? '실습';

  const code = activeFile !== null ? (filesContent[activeFile] ?? '') : '';

  // Show terminal while running + after exit until Grade is clicked
  const [showTerminal, setShowTerminal] = useState(false);

  useEffect(() => {
    if (isRunning) setShowTerminal(true);
  }, [isRunning]);

  // Switch back to static output when Grade result arrives
  useEffect(() => {
    if (outputMode === 'grade' && runResult) setShowTerminal(false);
  }, [outputMode, runResult]);

  function handleTerminalExit() {
    onRunExit?.();
  }

  async function handleSave() {}

  return (
    <div className='flex-1 flex flex-col min-w-0 h-full'>
      <div className='flex flex-1 min-h-0'>
        <CodeEditorFileTree
          folderName={folderName}
          files={problemFiles}
          activeFile={activeFile}
          onFileSelect={onActiveFileChange}
        />
        <div className='flex-1 min-w-0'>
          <MonacoEditorWrapper value={code} onChange={onCodeChange} language={language} />
        </div>
      </div>

      <CodeEditorToolbar onSave={handleSave} />

      {/* Output area */}
      <div className='min-h-30 max-h-56 overflow-auto bg-[#0e0e1a] border-t border-[#1e1e2e]'>
        {showTerminal ? (
          <InteractiveTerminal
            code={filesContent[activeFile] ?? ''}
            language={language}
            running={isRunning}
            onExit={handleTerminalExit}
          />
        ) : (
          <CodeRunOutput result={runResult} mode={outputMode} />
        )}
      </div>
    </div>
  );
}
