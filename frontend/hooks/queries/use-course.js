import { queryKeys } from '@/lib/query-keys';
import {
  getAllCourses,
  getCourseByCourseId,
  getCoursesByCategoryId,
} from '../../services/course-service';
import { useQuery } from '@tanstack/react-query';

export function useCourseQuery(categoryId, options = {}) {
  return useQuery({
    queryKey: queryKeys.courseList(categoryId),
    queryFn: () => getAllCourses(categoryId),
    ...options,
  });
}

export function useCourseDetailQuery(courseId, options = {}) {
  return useQuery({
    queryKey: ['course', courseId],
    queryFn: () => getCourseByCourseId(courseId),
    enabled: !!courseId,
    ...options,
  });
}
