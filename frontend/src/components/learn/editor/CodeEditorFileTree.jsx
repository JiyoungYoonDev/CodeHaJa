import { ChevronDown, FileCode2, FolderOpen } from 'lucide-react';

export default function CodeEditorFileTree({
  folderName = '실습',
  files = [],
  activeFile = null,
  onFileSelect,
}) {
  return (
    <div className='w-48 min-w-[160px] h-full bg-[#0e0e1a] border-r border-[#1e1e2e] flex flex-col text-sm'>
      {/* Header */}
      <div className='px-3 py-2 text-[10px] uppercase tracking-widest text-[#4a4a62] font-semibold border-b border-[#1e1e2e]'>
        Explorer
      </div>

      {/* Folder */}
      <div className='px-2 py-1'>
        <div className='flex items-center gap-1.5 px-1 py-1 text-[#9090a8] cursor-pointer select-none rounded hover:bg-[#1a1a2e] transition-colors'>
          <ChevronDown size={13} className='flex-shrink-0' />
          <FolderOpen size={14} className='flex-shrink-0 text-[#e8c46a]' />
          <span className='truncate text-xs'>{folderName}</span>
        </div>

        {/* Files */}
        {files.map((file) => {
          const isActive = file.name === activeFile;
          return (
            <button
              key={file.id ?? file.name}
              type='button'
              onClick={() => onFileSelect?.(file.name)}
              className={`w-full flex items-center gap-1.5 pl-6 pr-1 py-1 rounded cursor-pointer select-none ml-1 transition-colors text-left ${
                isActive
                  ? 'text-white bg-[#1e1e36]'
                  : 'text-[#9090a8] hover:text-white hover:bg-[#1a1a2e]'
              }`}
            >
              <FileCode2 size={13} className={`shrink-0 ${isActive ? 'text-violet-400' : 'text-[#5a5a72]'}`} />
              <span className='truncate text-xs'>{file.name}</span>
            </button>
          );
        })}
      </div>
    </div>
  );
}
