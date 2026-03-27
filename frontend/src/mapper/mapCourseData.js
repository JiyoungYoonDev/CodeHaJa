export function mapCourseCardData(course) {
  return {
    id: course.id,
    title: course.problem_title ?? 'Untitled',
    summary: course.book_description ?? 'No description available.',
    problemCount: course.book_count ?? 0,
    submissionCount: course.book_submission_count ?? 0,
    projectsCount: course.book_projects_count ?? 0,
    hours: course.hours ?? 0,
    categoryName: course.course_category?.category_name ?? 'General',
    categorySlug: course.course_category?.category_slug ?? 'general',
    isJoined: !!course.book_user_joined,
    badgeType: course.badge_type ?? null,
    accent: course.accent ?? 'from-slate-400 to-slate-500',
  };
}
