'use client';

import dynamic from 'next/dynamic';

const MonacoEditor = dynamic(() => import('@monaco-editor/react'), { ssr: false });

const EDITOR_OPTIONS = {
  fontSize: 14,
  fontFamily: "'JetBrains Mono', 'Fira Code', 'Cascadia Code', monospace",
  minimap: { enabled: false },
  lineNumbers: 'on',
  renderLineHighlight: 'line',
  scrollBeyondLastLine: false,
  automaticLayout: true,
  tabSize: 4,
  wordWrap: 'on',
  padding: { top: 16, bottom: 16 },
  overviewRulerLanes: 0,
  scrollbar: {
    verticalScrollbarSize: 6,
    horizontalScrollbarSize: 6,
  },
};

export default function MonacoEditorWrapper({ value, onChange, language = 'python' }) {
  return (
    <MonacoEditor
      height='100%'
      language={language}
      theme='vs-dark'
      value={value}
      onChange={onChange}
      options={EDITOR_OPTIONS}
    />
  );
}
