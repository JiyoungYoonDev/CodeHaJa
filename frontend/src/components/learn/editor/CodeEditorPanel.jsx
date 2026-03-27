'use client';

import CodeEditorFileTree from './CodeEditorFileTree';
import MonacoEditorWrapper from './MonacoEditorWrapper';
import CodeEditorToolbar from './CodeEditorToolbar';
import CodeRunOutput from './CodeRunOutput';

export default function CodeEditorPanel({ item, code, onCodeChange, runResult, outputMode }) {
  const language = item?.contentJson?.language ?? 'python';
  const fileName = item?.contentJson?.fileName ?? 'main.py';
  const folderName = item?.title ?? '실습';

  async function handleSave() {
    // Draft save requires entry — placeholder for future implementation
  }

  return (
    <div className='flex-1 flex flex-col min-w-0 h-full'>
      {/* File tree + Editor */}
      <div className='flex flex-1 min-h-0'>
        <CodeEditorFileTree folderName={folderName} fileName={fileName} />
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
