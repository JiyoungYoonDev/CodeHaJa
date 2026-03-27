import { apiFetch } from "@/lib/api-client";

const COURSE_API = process.env.NEXT_PUBLIC_API_COURSES;

export const getAllCourses = async (categoryId) => {
    const url = categoryId
        ? `${COURSE_API}?categoryId=${categoryId}`
        : COURSE_API
    const response = await apiFetch(url)
    return Array.isArray(response) ? response : (response.data ?? [])
}

export const getCoursesByCategoryId = async (categoryId) => {
    const response = await apiFetch(`${COURSE_API}?categoryId=${categoryId}`)
    return response
}

export const getCourseByCourseId = async (courseId) => {
    const response = await apiFetch(`${COURSE_API}/${courseId}`)
    return response
}