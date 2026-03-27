'use client';

import { CheckCircle2 } from 'lucide-react';

export default function CurriculumRenderer({ content }) {
  const extractItems = (nodes) => {
    if (!nodes) return [];
    const items = [];
    
    const search = (nodeList) => {
      nodeList.forEach(node => {
        if (node.type === 'listItem') {
          const text = node.content
            ?.flatMap(c => c.content || [])
            .filter(t => t.type === 'text')
            .map(t => t.text)
            .join('') || "";
          if (text) items.push(text);
        }
        if (node.content) search(node.content);
      });
    };

    search(nodes);
    return items;
  };

  const learningPoints = extractItems(content?.content);

  if (learningPoints.length === 0) return null;

  return (
    <div className="grid grid-cols-1 md:grid-cols-2 gap-x-12 gap-y-5">
      {learningPoints.map((point, index) => (
        <div 
          key={index} 
          className="flex items-start gap-3 group animate-in fade-in slide-in-from-left duration-500"
          style={{ animationDelay: `${index * 100}ms` }}
        >
          <div className="mt-1 shrink-0">
            <CheckCircle2 
              size={20} 
              className="text-violet-500 fill-violet-50 dark:fill-violet-950 transition-transform group-hover:scale-110" 
            />
          </div>
          
          <p className="text-slate-700 dark:text-slate-300 font-bold leading-snug tracking-tight">
            {point}
          </p>
        </div>
      ))}
    </div>
  );
}