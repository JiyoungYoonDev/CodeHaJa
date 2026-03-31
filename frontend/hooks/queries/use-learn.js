import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  getLectureItemEntry,
  getLectureItem,
  getLectureItems,
  submitCode,
  getSubmissions,
  getSectionLectures,
  saveEntryProgress,
  getEntryProgress,
  saveLectureProgress,
  getLectureProgress,
  getCourseLectureProgress,
  getLatestSubmission,
  getPassedCodingItems,
  saveItemProgress,
  getCompletedItemCount,
  getCompletedItemIds,
  submitProjectSubmission,
  getLatestProjectSubmission,
  submitQuizSubmission,
  getLatestQuizSubmission,
} from '../../services/learn-service';
import { queryKeys } from '@/lib/query-keys';

export function useLectureItemEntryQuery(entryId, options = {}) {
  return useQuery({
    queryKey: ['lectureItemEntry', entryId],
    queryFn: () => getLectureItemEntry(entryId),
    enabled: !!entryId,
    retry: false,
    ...options,
  });
}

export function useSubmissionsQuery(entryId, options = {}) {
  return useQuery({
    queryKey: ['codingSubmissions', entryId],
    queryFn: () => getSubmissions(entryId),
    enabled: !!entryId,
    ...options,
  });
}

export function useSubmitCodeMutation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ entryId, payload }) => submitCode(entryId, payload),
    onSuccess: (_data, variables) => {
      queryClient.invalidateQueries({ queryKey: ['codingSubmissions', variables.entryId] });
    },
  });
}

export function useLectureItemQuery(itemId, options = {}) {
  return useQuery({
    queryKey: ['lectureItem', itemId],
    queryFn: () => getLectureItem(itemId),
    enabled: !!itemId,
    ...options,
  });
}

export function useLectureItemsQuery(lectureId, options = {}) {
  return useQuery({
    queryKey: ['lecture-items', lectureId],
    queryFn: () => getLectureItems(lectureId),
    enabled: !!lectureId,
    ...options,
  });
}

export function useSectionLecturesQuery(sectionId, options = {}) {
  return useQuery({
    queryKey: queryKeys.sectionLectures(sectionId),
    queryFn: () => getSectionLectures(sectionId),
    enabled: !!sectionId,
    ...options,
  });
}

export function useLectureProgressQuery(lectureId, options = {}) {
  return useQuery({
    queryKey: ['lecture-progress', lectureId],
    queryFn: () => getLectureProgress(lectureId),
    enabled: !!lectureId,
    retry: false,
    ...options,
  });
}

export function useEntryProgressQuery(entryId, options = {}) {
  return useQuery({
    queryKey: ['entry-progress', entryId],
    queryFn: () => getEntryProgress(entryId),
    enabled: !!entryId,
    retry: false,
    ...options,
  });
}

export function useSaveEntryProgressMutation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ entryId, payload }) => saveEntryProgress(entryId, payload),
    onSuccess: (_data, variables) => {
      queryClient.invalidateQueries({ queryKey: ['entry-progress', variables.entryId] });
    },
  });
}

export function useLatestSubmissionQuery(itemId, options = {}) {
  return useQuery({
    queryKey: ['latest-submission', itemId],
    queryFn: () => getLatestSubmission(itemId),
    enabled: !!itemId,
    retry: false,
    ...options,
  });
}

export function useCourseLectureProgressQuery(courseId, options = {}) {
  return useQuery({
    queryKey: ['course-lecture-progress', courseId],
    queryFn: () => getCourseLectureProgress(courseId),
    enabled: !!courseId,
    retry: false,
    ...options,
  });
}

export function usePassedCodingItemsQuery(courseId, options = {}) {
  return useQuery({
    queryKey: ['passed-coding-items', courseId],
    queryFn: () => getPassedCodingItems(courseId),
    enabled: !!courseId,
    retry: false,
    ...options,
  });
}

export function useSaveItemProgressMutation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ itemId }) => saveItemProgress(itemId),
    onSuccess: (_data, variables) => {
      queryClient.invalidateQueries({ queryKey: ['completed-item-count', variables.courseId] });
    },
  });
}

export function useCompletedItemCountQuery(courseId, options = {}) {
  return useQuery({
    queryKey: ['completed-item-count', courseId],
    queryFn: () => getCompletedItemCount(courseId),
    enabled: !!courseId,
    retry: false,
    ...options,
  });
}

export function useCompletedItemIdsQuery(courseId, options = {}) {
  return useQuery({
    queryKey: ['completed-item-ids', courseId],
    queryFn: () => getCompletedItemIds(courseId),
    enabled: !!courseId,
    retry: false,
    ...options,
  });
}

export function useSaveLectureProgressMutation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ lectureId, payload }) => saveLectureProgress(lectureId, payload),
    onSuccess: (_data, variables) => {
      queryClient.invalidateQueries({ queryKey: ['lecture-progress', variables.lectureId] });
    },
  });
}

export function useSubmitProjectMutation() {
  return useMutation({
    mutationFn: ({ itemId, payload }) => submitProjectSubmission(itemId, payload),
  });
}

export function useLatestProjectSubmissionQuery(itemId, options = {}) {
  return useQuery({
    queryKey: ['latest-project-submission', itemId],
    queryFn: () => getLatestProjectSubmission(itemId),
    enabled: !!itemId,
    retry: false,
    ...options,
  });
}

export function useSubmitQuizMutation() {
  return useMutation({
    mutationFn: ({ itemId, payload }) => submitQuizSubmission(itemId, payload),
  });
}

export function useLatestQuizSubmissionQuery(itemId, options = {}) {
  return useQuery({
    queryKey: ['latest-quiz-submission', itemId],
    queryFn: () => getLatestQuizSubmission(itemId),
    enabled: !!itemId,
    retry: false,
    ...options,
  });
}
