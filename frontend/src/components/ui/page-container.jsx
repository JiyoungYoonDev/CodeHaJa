import { cn } from '@/lib/utils';

export default function PageContainer({ className, children, ...props }) {
  return (
    <div
      className={cn('max-w-[100rem] mx-auto px-6', className)}
      {...props}
    >
      {children}
    </div>
  );
}
