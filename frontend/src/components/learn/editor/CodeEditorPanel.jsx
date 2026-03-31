'use client';

import CodeEditorFileTree from './CodeEditorFileTree';
import MonacoEditorWrapper from './MonacoEditorWrapper';
import CodeEditorToolbar from './CodeEditorToolbar';
import CodeRunOutput from './CodeRunOutput';

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
}) {
  const source = currentProblem ?? item?.contentJson ?? null;
  const language = source?.language ?? 'python';
  const folderName = item?.title ?? '실습';

  // Current file content shown in Monaco
  const code = activeFile !== null ? (filesContent[activeFile] ?? '') : '';

  async function handleSave() {
    // Draft save requires entry — placeholder for future implementation
  }

  return (
    <div className='flex-1 flex flex-col min-w-0 h-full'>
      {/* File tree + Editor */}
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

      {/* Toolbar: Save / History / AI */}
      <CodeEditorToolbar onSave={handleSave} />

      {/* Output */}
      <div className='min-h-14 max-h-44 overflow-auto bg-[#0e0e1a] border-t border-[#1e1e2e]'>
        <CodeRunOutput result={runResult} mode={outputMode} />
      </div>
    </div>
  );
}
