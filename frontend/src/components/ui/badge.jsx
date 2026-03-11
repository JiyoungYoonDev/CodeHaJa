import * as React from 'react';
import { cva } from 'class-variance-authority';
import { Slot } from '@radix-ui/react-slot';
import { cn } from '@/lib/utils';

const badgeVariants = cva(
  'inline-flex w-fit shrink-0 items-center justify-center gap-1.5 overflow-hidden rounded-md border transition-all focus:outline-none focus:ring-2 focus:ring-ring focus:ring-offset-2 disabled:opacity-50',
  {
    variants: {
      variant: {
        default:
          'border-transparent bg-primary text-primary-foreground hover:bg-primary/80',
        secondary:
          'border-transparent bg-secondary text-secondary-foreground hover:bg-secondary/80',
        outline:
          'text-foreground border-border hover:bg-accent hover:text-accent-foreground',
        success:
          'border-transparent bg-green-100 text-green-800 font-bold dark:bg-green-900/40 dark:text-green-300',
        warning:
          'border-transparent bg-yellow-100 text-yellow-800 font-bold dark:bg-yellow-900/40 dark:text-yellow-300',
        info: 'border-transparent bg-blue-100 text-blue-800 font-bold dark:bg-blue-900/40 dark:text-blue-300',
        destructive:
          'border-transparent bg-destructive text-destructive-foreground hover:bg-destructive/80',
      },
      size: {
        sm: 'px-2.5 py-0.5 text-xs font-medium',
        md: 'px-3.5 py-1 text-sm font-semibold', 
        lg: 'px-5 py-1.5 text-base font-bold',
      },
    },
    defaultVariants: {
      variant: 'default',
      size: 'md',
    },
  },
);

function Badge({ className, variant, size, asChild = false, ...props }) {
  const Comp = asChild ? Slot : 'span';

  return (
    <Comp
      className={cn(badgeVariants({ variant, size }), className)}
      {...props}
    />
  );
}

export { Badge, badgeVariants };
