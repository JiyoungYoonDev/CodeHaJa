import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { apiFetch } from '@/lib/api-client';
import { queryKeys } from '../query-keys';
import { getCourseCategories, getProblemBooks } from '../../../services/get-service';

export function useProblemBooks(options = {}) {
  return useQuery({
    queryKey: queryKeys.problemBooks,
    queryFn: getProblemBooks,
    ...options,
  });
}

export function useCreateProblemBook() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (payload) =>
      apiFetch('/api/problem-books', {
        method: 'POST',
        body: JSON.stringify(payload),
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.problemBooks });
    },
  });
}
