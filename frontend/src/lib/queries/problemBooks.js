import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { apiFetch } from '@/lib/api-client';

const problemBookKeys = {
  all: ['problemBooks'],
};

export function useProblemBooks() {
  return useQuery({
    queryKey: problemBookKeys.all,
    queryFn: () => apiFetch('/api/problem-books'),
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
      queryClient.invalidateQueries({ queryKey: problemBookKeys.all });
    },
  });
}
