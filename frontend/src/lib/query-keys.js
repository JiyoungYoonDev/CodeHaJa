export const queryKeys = {
  courses: ['courses'],
  courseList: (categoryId) => ['courses', categoryId ?? 'all'],
  courseCategories: ['course-categories'],
  courseCategory: (categoryId) => ['course-categories', categoryId],
  sectionLectures: (sectionId) => ['section-lectures', sectionId],
  entryProgress: (entryId, userKey) => ['entry-progress', entryId, userKey],
  lectureProgress: (lectureId, userKey) => ['lecture-progress', lectureId, userKey],
};
