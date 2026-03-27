import { useMutation, useQuery } from '@tanstack/react-query';
import { enrollCourse, getEnrollmentStatus } from '../../services/enrollment-service';

export function useEnrollmentStatusQuery(courseId, options = {}) {
  return useQuery({
    queryKey: ['enrollment-status', courseId],
    queryFn: () => getEnrollmentStatus(courseId),
    enabled: !!courseId,
    retry: false,
    ...options,
  });
}

export function useEnrollMutation() {
  return useMutation({
    mutationFn: ({ courseId }) => enrollCourse(courseId),
  });
}
