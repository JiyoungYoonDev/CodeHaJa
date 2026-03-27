import { apiFetch } from '@/lib/api-client';

export const enrollCourse = async (courseId) => {
  return apiFetch('/api/enrollments', {
    method: 'POST',
    body: JSON.stringify({ courseId }),
  });
};

export const getEnrollmentStatus = async (courseId) => {
  return apiFetch(`/api/enrollments/status?courseId=${courseId}`);
};
