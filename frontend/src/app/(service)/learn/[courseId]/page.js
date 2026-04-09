'use client';

import { useParams, useRouter } from 'next/navigation';
import React from 'react';
import { useCourseDetailQuery } from '../../../../../hooks/queries/use-course';
import {
  useEnrollMutation,
  useEnrollmentStatusQuery,
} from '../../../../../hooks/queries/use-enrollment';
import {
  useCourseLectureProgressQuery,
  useCompletedItemCountQuery,
} from '../../../../../hooks/queries/use-learn';
import { useRequireAuthAction } from '../../../../../hooks/use-require-auth-action';
import { useAuth } from '@/lib/auth-context';
import AuthRequiredModal from '@/components/common/AuthRequiredModal';
import HeaderCard from '@/components/cards/header-card';
import ContentBox from '@/components/ui/content-box';
import PageContainer from '@/components/ui/page-container';
import { BookText, ChartLine, Clock4, Code } from 'lucide-react';
import CurriculumRenderer from '@/components/course/CurriculumRenderer';
import CourseCurriculum from '@/components/course/CourseCurriculum';
import { PageLoading, PageError } from '@/components/ui/page-status';

export default function CourseDetailPage() {
  const params = useParams();
  const router = useRouter();
  const courseId = Number(params.courseId);
  const { user } = useAuth();

  const { data: response, isLoading, error } = useCourseDetailQuery(courseId);
  const course = response?.data;

  const { data: statusResponse } = useEnrollmentStatusQuery(courseId, {
    enabled: !!user,
  });
  const isEnrolled = statusResponse?.data?.enrolled ?? false;

  const { mutateAsync: enroll } = useEnrollMutation();

  const { data: lectureProgressResponse } = useCourseLectureProgressQuery(
    courseId,
    { enabled: !!user },
  );
  const completedLectureIds = new Set(
    lectureProgressResponse?.data?.completedLectureIds ?? [],
  );

  const { data: completedItemCountResponse } = useCompletedItemCountQuery(
    courseId,
    { enabled: !!user },
  );
  const completedItemCount = completedItemCountResponse?.data ?? 0;

  const allLectures = (course?.sections ?? []).flatMap((s) => s.lectures ?? []);
  const totalItemCount = allLectures.reduce(
    (sum, l) => sum + (l.itemCount ?? 0),
    0,
  );
  const overallProgress =
    totalItemCount > 0
      ? Math.round((completedItemCount / totalItemCount) * 100)
      : 0;

  const visitedItemIds = new Set(
    allLectures
      .filter((l) => completedLectureIds.has(l.id) && l.firstItemId)
      .map((l) => l.firstItemId),
  );

  async function doEnroll() {
    await enroll({ courseId });
    const saved = localStorage.getItem(`codehaja_resume_${courseId}`);
    if (saved) {
      try {
        const { sectionId, itemId } = JSON.parse(saved);
        router.push(`/learn/${courseId}/${sectionId}?itemId=${itemId}`);
        return;
      } catch (_) {}
    }
    const firstSection = course?.sections?.[0];
    const firstLecture = firstSection?.lectures?.[0];
    const firstItemId = firstLecture?.firstItemId;
    const sectionId = firstSection?.id;
    if (sectionId) {
      router.push(
        firstItemId
          ? `/learn/${courseId}/${sectionId}?itemId=${firstItemId}`
          : `/learn/${courseId}/${sectionId}`,
      );
    }
  }

  const {
    guard: handleEnroll,
    showAuthModal,
    closeAuthModal,
  } = useRequireAuthAction(doEnroll);

  async function doStartLecture(cId, sectionId, firstItemId) {
    if (!isEnrolled) {
      await enroll({ courseId: cId });
    }
    router.push(
      firstItemId
        ? `/learn/${cId}/${sectionId}?itemId=${firstItemId}`
        : `/learn/${cId}/${sectionId}`,
    );
  }

  const { guard: handleStartLecture } = useRequireAuthAction(doStartLecture);

  if (isLoading) return <PageLoading />;
  if (error) return <PageError message='Error loading course.' />;

  const lectureCount = allLectures.length;

  return (
    <PageContainer className='min-h-screen pb-20'>
      <AuthRequiredModal open={showAuthModal} onClose={closeAuthModal} />

      <ContentBox className='mt-10 md:mt-15 border-none shadow-[0_8px_40px_rgba(0,0,0,0.04)] rounded-[32px] bg-card overflow-hidden'>
        <HeaderCard
          data={course}
          isEnrolled={isEnrolled}
          onEnroll={handleEnroll}
        />
        {isEnrolled && totalItemCount > 0 && (
          <div className='px-4 pb-6'>
            <div className='rounded-2xl bg-card px-6 py-4'>
              <div className='flex items-center justify-between mb-3'>
                <div className='flex items-center gap-2'>
                  <div className='w-2 h-2 rounded-full bg-emerald-500' />
                  <span className='text-sm font-semibold text-muted-foreground'>
                    Your Progress
                  </span>
                </div>
                <span className='text-sm font-black text-emerald-500'>
                  {overallProgress}% Completed
                </span>
              </div>
              <div className='h-2 w-full bg-muted rounded-full overflow-hidden'>
                <div
                  className='h-full bg-linear-to-r from-emerald-500 to-emerald-400 rounded-full transition-all duration-700'
                  style={{ width: `${overallProgress}%` }}
                />
              </div>
              <div className='flex justify-between mt-2'>
                <span className='text-[11px] text-muted-foreground'>
                  {completedItemCount} / {totalItemCount} items
                </span>
                <span className='text-[11px] text-muted-foreground'>
                  {totalItemCount - completedItemCount} remaining
                </span>
              </div>
            </div>
          </div>
        )}
      </ContentBox>

      <div className='mt-10 flex justify-center'>
        <ContentBox
          direction='row'
          className='inline-flex items-center justify-center gap-10 md:gap-20 py-6 px-12 rounded-full border-border shadow-sm bg-card'
        >
          <InfoItem
            icon={<BookText className='text-primary' />}
            label='Lessons'
            value={`${lectureCount} Lectures`}
          />
          <div className='w-px h-8 bg-border' />
          <InfoItem
            icon={<ChartLine className='text-primary' />}
            label='Level'
            value={
              course?.difficulty ? course.difficulty.toUpperCase() : 'BEGINNER'
            }
          />
          <div className='w-px h-8 bg-border' />
          <InfoItem
            icon={<Clock4 className='text-primary' />}
            label='Time'
            value={`${course?.hours || 0}h+`}
          />
          <div className='w-px h-8 bg-border' />
          <InfoItem
            icon={<Code className='text-primary' />}
            label='Projects'
            value={`${course?.projectsCount || 0}`}
          />
        </ContentBox>
      </div>

      <div className='mt-20 max-w-4xl mx-auto px-4'>
        <div className='flex items-center gap-3 mb-8'>
          <div className='w-1.5 h-8 bg-primary rounded-full' />
          <h2 className='text-3xl font-black tracking-tight text-foreground'>
            What you'll learn
          </h2>
        </div>

        <ContentBox className='bg-card border-border p-10 rounded-[32px]'>
          {course?.detailedCurriculum ? (
            <CurriculumRenderer content={course?.detailedCurriculum} />
          ) : (
            <p className='text-muted-foreground'>
              No detailed curriculum available yet.
            </p>
          )}
        </ContentBox>
      </div>
      <div className='mt-20 max-w-4xl mx-auto px-4 pb-20'>
        <div className='flex items-center gap-3 mb-8'>
          <div className='w-1.5 h-8 bg-primary rounded-full' />
          <h2 className='text-3xl font-black tracking-tight text-foreground'>
            Curriculum
          </h2>
        </div>
        <CourseCurriculum
          sections={course?.sections ?? []}
          courseId={courseId}
          difficulty={course?.difficulty}
          visitedItemIds={visitedItemIds}
          onStartLecture={handleStartLecture}
        />
      </div>
    </PageContainer>
  );
}

function InfoItem({ icon, label, value }) {
  return (
    <div className='flex items-center gap-4'>
      <div className='p-3 bg-accent rounded-2xl'>
        {React.cloneElement(icon, { size: 20 })}
      </div>
      <div className='flex flex-col'>
        <span className='text-[18px] font-black text-muted-foreground uppercase tracking-widest leading-none mb-1'>
          {label}
        </span>
        <span className='text-sm font-bold text-foreground'>{value}</span>
      </div>
    </div>
  );
}
