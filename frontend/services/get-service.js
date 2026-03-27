import { apiFetch } from '@/lib/api-client';

const COURSE_CATEGORIES_PATH = process.env.NEXT_PUBLIC_API_COURSE_CATEGORIES;

const COURSE = process.env.NEXT_PUBLIC_API_COURSES;

export const getCoursesByCourseId = async (courseId) => {
  return apiFetch(`${COURSE}/${courseId}`);
};

export const getCourseCategories = async () => {
  const response = await apiFetch(COURSE_CATEGORIES_PATH);
  return Array.isArray(response) ? response : (response.data ?? []);
};
