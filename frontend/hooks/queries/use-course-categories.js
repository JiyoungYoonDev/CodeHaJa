import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
// import { createCourseCategory } from '@/services/create-service';

import { queryKeys } from '@/lib/query-keys';
import { getCourseCategories } from '../../services/get-service';

export function useCourseCategoriesQuery(options = {}) {
  return useQuery({
    queryKey: queryKeys.courseCategories,
    queryFn: getCourseCategories, 
    ...options,
  });
}

// export function useCreateCourseCategoryMutation(options = {}) {
//   const queryClient = useQueryClient();

//   return useMutation({
//     mutationFn: createCourseCategory,
//     onSuccess: (...args) => {
//       queryClient.invalidateQueries({ queryKey: queryKeys.courseCategories });
//       options.onSuccess?.(...args);
//     },
//     ...options,
//   });
// }
