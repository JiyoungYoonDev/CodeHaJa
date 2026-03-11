import { apiFetch } from '@/lib/api-client';

const COURSE_CATEGORIES_PATH =
  process.env.NEXT_PUBLIC_API_COURSE_CATEGORIES;

export const getProblemBooks = async () => {
  return apiFetch('/api/problem-books');
};

export const getProblemBook = async (bookId) => {
  return apiFetch(`/api/problem-books/${bookId}`);
};

export const getCourseCategories = async () => {
  return apiFetch(COURSE_CATEGORIES_PATH);
};
