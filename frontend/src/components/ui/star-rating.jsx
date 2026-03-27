import { Star } from 'lucide-react';

export default function StarRating({ rating = 0, max = 5, size = 16 }) {
  const full = Math.floor(rating);
  const partial = rating - full;
  const empty = max - full - (partial > 0 ? 1 : 0);

  return (
    <div className='flex items-center gap-1.5'>
      <div className='flex items-center gap-0.5'>
        {Array.from({ length: full }, (_, i) => (
          <Star
            key={`full-${i}`}
            size={size}
            className='text-amber-400 fill-amber-400'
          />
        ))}

        {partial > 0 && (
          <div className='relative' style={{ width: size, height: size }}>
            <Star size={size} className='text-slate-200 fill-slate-200 absolute inset-0' />
            <div className='absolute inset-0 overflow-hidden' style={{ width: `${partial * 100}%` }}>
              <Star size={size} className='text-amber-400 fill-amber-400' />
            </div>
          </div>
        )}

        {Array.from({ length: empty }, (_, i) => (
          <Star
            key={`empty-${i}`}
            size={size}
            className='text-slate-200 fill-slate-200'
          />
        ))}
      </div>

      <span className='text-sm font-bold text-slate-700'>
        {rating > 0 ? rating.toFixed(1) : 'New'}
      </span>
    </div>
  );
}
