import { Loader2 } from 'lucide-react';

export function PageLoading({ message = 'Loading...' }) {
  return (
    <div className='p-20 flex flex-col items-center justify-center gap-3'>
      <Loader2 size={24} className='animate-spin text-primary' />
      <p className='text-sm font-bold text-muted-foreground'>{message}</p>
    </div>
  );
}

export function PageError({ message = 'Something went wrong. Please try again.' }) {
  return (
    <div className='p-20 text-center'>
      <p className='text-sm font-bold text-destructive'>{message}</p>
    </div>
  );
}
