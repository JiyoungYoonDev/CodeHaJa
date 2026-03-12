import { apiFetch } from '@/lib/api-client';

const COURSE_CATEGORIES_PATH = process.env.NEXT_PUBLIC_API_COURSE_CATEGORIES;

const COURSE_BOOKS = process.env.NEXT_PUBLIC_API_BOOKS;

export const getProblemBooks = async () => {
    const data = await apiFetch(COURSE_BOOKS);
    console.log("CORRECT DATA ? ", data)
    return data;
};

export const getProblemBook = async (bookId) => {
  return apiFetch(`${COURSE_BOOKS}/${bookId}`);
};

export const getCourseCategories = async () => {
  return apiFetch(COURSE_CATEGORIES_PATH);
};
