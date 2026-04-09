'use client';

import { useState, useEffect, useCallback } from 'react';
import { ThumbsUp, ThumbsDown } from 'lucide-react';
import { cn } from '@/lib/utils';
import { apiFetch } from '@/lib/api-client';

export default function LessonRating({ lectureItemId }) {
  const [rating, setRating] = useState(null); // 'LIKE' | 'DISLIKE' | null
  const [counts, setCounts] = useState({ likes: 0, dislikes: 0 });
  const [loading, setLoading] = useState(false);

  // Load existing reaction from server
  useEffect(() => {
    if (!lectureItemId) return;
    apiFetch(`/api/interactions/items/${lectureItemId}/reactions`)
      .then((res) => {
        const data = res?.data;
        if (data) {
          setRating(data.userReaction);
          setCounts({ likes: data.likeCount ?? 0, dislikes: data.dislikeCount ?? 0 });
        }
      })
      .catch(() => {});
  }, [lectureItemId]);

  const handleReact = useCallback(async (type) => {
    if (!lectureItemId || loading) return;
    setLoading(true);
    try {
      const res = await apiFetch(`/api/interactions/items/${lectureItemId}/reactions`, {
        method: 'POST',
        body: JSON.stringify({ reactionType: type }),
      });
      const data = res?.data;
      if (data) {
        setRating(data.userReaction);
        setCounts({ likes: data.likeCount ?? 0, dislikes: data.dislikeCount ?? 0 });
      }
    } catch {
      // Optimistic fallback
      setRating((prev) => (prev === type ? null : type));
    } finally {
      setLoading(false);
    }
  }, [lectureItemId, loading]);

  return (
    <div className='mt-8 border border-[#2a2a3e] rounded-xl p-5'>
      <p className='text-sm text-[#9090a8] text-center mb-4'>How was this lesson?</p>
      <div className='flex items-center justify-center gap-4'>
        <button
          onClick={() => handleReact('LIKE')}
          disabled={loading}
          className={cn(
            'flex items-center gap-2 px-4 py-2 rounded-lg border text-sm font-medium transition-all',
            rating === 'LIKE'
              ? 'border-violet-500 bg-violet-600/15 text-violet-300'
              : 'border-[#2a2a3e] text-[#7070a0] hover:border-violet-500/50 hover:text-violet-400'
          )}
        >
          <ThumbsUp size={15} />
          <span>Like</span>
          {counts.likes > 0 && <span className='text-xs opacity-60'>{counts.likes}</span>}
        </button>
        <div className='w-px h-5 bg-[#2a2a3e]' />
        <button
          onClick={() => handleReact('DISLIKE')}
          disabled={loading}
          className={cn(
            'flex items-center gap-2 px-4 py-2 rounded-lg border text-sm font-medium transition-all',
            rating === 'DISLIKE'
              ? 'border-rose-500 bg-rose-600/15 text-rose-300'
              : 'border-[#2a2a3e] text-[#7070a0] hover:border-rose-500/50 hover:text-rose-400'
          )}
        >
          <ThumbsDown size={15} />
          <span>Dislike</span>
          {counts.dislikes > 0 && <span className='text-xs opacity-60'>{counts.dislikes}</span>}
        </button>
      </div>
    </div>
  );
}
