import { cn } from '@/lib/utils';

export default function LessonXpBadge({ points = 0, className }) {
  return (
    <span
      className={cn(
        'inline-flex items-center px-3 py-1 rounded-full text-xs font-bold',
        'bg-violet-600/20 text-violet-400 border border-violet-600/30',
        className
      )}
    >
      {points} XP
    </span>
  );
}
