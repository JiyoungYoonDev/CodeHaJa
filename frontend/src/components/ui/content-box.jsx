import { cn } from '@/lib/utils';
import PageContainer from './page-container';

export default function ContentBox({ className, direction = 'col', children, ...props }) {
  return (
    <PageContainer
      className={cn(
        'rounded-xl border border-slate-200 bg-card p-8 md:p-10 flex',
        direction === 'row' ? 'flex-row' : 'flex-col',
        className,
      )}
      {...props}
    >
      {children}
    </PageContainer>
  );
}
