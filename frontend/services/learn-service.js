import { apiFetch } from '@/lib/api-client';

// LectureItemEntry
export const getLectureItemEntry = async (entryId) => {
  return apiFetch(`/api/lecture-item-entries/${entryId}`);
};

// Coding submission (grade + run)
export const submitCode = async (lectureItemEntryId, { sourceCode, language }) => {
  return apiFetch(`/api/coding-submissions/${lectureItemEntryId}`, {
    method: 'POST',
    body: JSON.stringify({ sourceCode, language }),
  });
};

export const getSubmissions = async (lectureItemEntryId) => {
  return apiFetch(`/api/coding-submissions/entry/${lectureItemEntryId}`);
};

export const getLatestSubmission = async (itemId) => {
  return apiFetch(`/api/coding-submissions/item/${itemId}/latest`);
};

export const getPassedCodingItems = async (courseId) => {
  return apiFetch(`/api/coding-submissions/course/${courseId}/passed-items`);
};

// Single lecture item (content)
export const getLectureItem = async (itemId) => {
  return apiFetch(`/api/lecture-items/${itemId}`);
};

// Lecture items for sidebar
export const getLectureItems = async (lectureId) => {
  return apiFetch(`/api/lectures/${lectureId}/items?size=100`);
};

// Section lectures for navigation
export const getSectionLectures = async (sectionId) => {
  return apiFetch(`/api/course-sections/${sectionId}/lectures?size=100`);
};

// Course lecture progress (completed lecture IDs)
export const getCourseLectureProgress = async (courseId) => {
  return apiFetch(`/api/progress/courses/${courseId}/lectures`);
};

// Progress - lecture
export const saveLectureProgress = async (lectureId, payload) => {
  return apiFetch(`/api/progress/lectures/${lectureId}`, {
    method: 'PUT',
    body: JSON.stringify(payload),
  });
};

export const getLectureProgress = async (lectureId) => {
  return apiFetch(`/api/progress/lectures/${lectureId}`);
};

// Item-level completion
export const saveItemProgress = async (itemId) => {
  return apiFetch(`/api/progress/items/${itemId}`, { method: 'POST' });
};

export const getCompletedItemCount = async (courseId) => {
  return apiFetch(`/api/progress/courses/${courseId}/completed-item-count`);
};

// Progress - entry
export const saveEntryProgress = async (entryId, payload) => {
  return apiFetch(`/api/progress/entries/${entryId}`, {
    method: 'PUT',
    body: JSON.stringify(payload),
  });
};

export const getEntryProgress = async (entryId) => {
  return apiFetch(`/api/progress/entries/${entryId}`);
};
