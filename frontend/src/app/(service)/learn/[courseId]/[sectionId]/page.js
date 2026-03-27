'use client';

import { Suspense } from 'react';
import { useState, useEffect } from 'react';
import { useParams, useSearchParams, useRouter } from 'next/navigation';
import { useQueryClient } from '@tanstack/react-query';
import LearnLayout from '@/components/learn/LearnLayout';
import LearnTopBar from '@/components/learn/LearnTopBar';
import LearnSidebar from '@/components/learn/LearnSidebar';
import LessonContentPanel from '@/components/learn/content/LessonContentPanel';
import CodeEditorPanel from '@/components/learn/editor/CodeEditorPanel';
import {
  useLectureItemQuery,
  useLectureItemsQuery,
  useSubmitCodeMutation,
  useSaveLectureProgressMutation,
  useSaveItemProgressMutation,
  useSectionLecturesQuery,
  useCourseLectureProgressQuery,
  useLectureProgressQuery,
  useLatestSubmissionQuery,
  usePassedCodingItemsQuery,
} from '../../../../../../hooks/queries/use-learn';
import { useAuth } from '@/lib/auth-context';
import CorrectModal from '@/components/learn/CorrectModal';

const PLACEHOLDER = '# 여기에 코드를 작성하세요\n';

function LearnPageInner() {
  const params = useParams();
  const searchParams = useSearchParams();
  const router = useRouter();
  const { user, loading: authLoading } = useAuth();
  const queryClient = useQueryClient();
  const courseId = Number(params.courseId);
  const sectionId = Number(params.sectionId);
  const itemId = searchParams.get('itemId') ? Number(searchParams.get('itemId')) : null;

  // 비로그인 시 로그인 페이지로 리다이렉트
  useEffect(() => {
    if (!authLoading && user === null) {
      const from = encodeURIComponent(window.location.pathname + window.location.search);
      router.replace(`/login?from=${from}`);
    }
  }, [authLoading, user, router]);

  const { data: sectionLecturesResponse } = useSectionLecturesQuery(sectionId);

  useEffect(() => {
    if (itemId) return;
    const lectures = sectionLecturesResponse?.data?.content ?? [];
    const firstItemId = lectures.find((l) => l.firstItemId)?.firstItemId;
    if (firstItemId) {
      router.replace(`/learn/${courseId}/${sectionId}?itemId=${firstItemId}`);
    }
  }, [sectionLecturesResponse, itemId, courseId, sectionId, router]);

  const [code, setCode] = useState(PLACEHOLDER);
  const [runResult, setRunResult] = useState(null);
  const [outputMode, setOutputMode] = useState('run');
  const [isRunning, setIsRunning] = useState(false);
  const [isGrading, setIsGrading] = useState(false);
  const [isCompleted, setIsCompleted] = useState(false);
  const [sidebarOpen, setSidebarOpen] = useState(true);
  const [showCorrectModal, setShowCorrectModal] = useState(false);
  const [visitedLectureIds, setVisitedLectureIds] = useState(new Set());
  const [visitedItemIds, setVisitedItemIds] = useState(new Set());
  const [passedItemIds, setPassedItemIds] = useState(new Set());

  const { data: itemResponse, isLoading } = useLectureItemQuery(itemId);
  const item = itemResponse?.data ?? null;

  // Reset all user-specific state when the logged-in user changes
  useEffect(() => {
    setVisitedLectureIds(new Set());
    setVisitedItemIds(new Set());
    setPassedItemIds(new Set());
    setIsCompleted(false);
  }, [user?.id]);

  // Load passed coding item IDs from DB
  const { data: passedCodingItemsResponse } = usePassedCodingItemsQuery(courseId, { enabled: !!user });
  useEffect(() => {
    const ids = passedCodingItemsResponse?.data ?? [];
    if (ids.length === 0) return;
    setPassedItemIds(new Set(ids));
  }, [passedCodingItemsResponse]);

  // Load completed lectures from DB (for sidebar visited state)
  const { data: lectureProgressResponse } = useCourseLectureProgressQuery(courseId, { enabled: !!user });
  useEffect(() => {
    const ids = lectureProgressResponse?.data?.completedLectureIds ?? [];
    if (ids.length === 0) return;
    setVisitedLectureIds((prev) => {
      const next = new Set(prev);
      ids.forEach((id) => next.add(id));
      return next;
    });
  }, [lectureProgressResponse]);

  // Direct lecture progress for current item (isCompleted + code restore)
  const { data: currentLectureProgressResponse } = useLectureProgressQuery(item?.lectureId, {
    enabled: !!item?.lectureId && !!user,
    retry: false,
  });
  const { data: latestSubmissionResponse } = useLatestSubmissionQuery(itemId, {
    enabled: !!itemId && !!user,
    retry: false,
  });


  const { data: lectureItemsResponse } = useLectureItemsQuery(item?.lectureId);
  const lectureItems = lectureItemsResponse?.data?.content ?? [];
  const lectures = sectionLecturesResponse?.data?.content ?? [];

  const currentItemIndex = lectureItems.findIndex((i) => i.id === itemId);
  const currentLectureIndex = lectures.findIndex((l) => l.id === item?.lectureId);

  const prevItemId =
    currentItemIndex > 0
      ? lectureItems[currentItemIndex - 1].id
      : lectures[currentLectureIndex - 1]?.firstItemId ?? null;

  const nextItemId =
    currentItemIndex !== -1 && currentItemIndex < lectureItems.length - 1
      ? lectureItems[currentItemIndex + 1].id
      : lectures[currentLectureIndex + 1]?.firstItemId ?? null;

  const base = `/learn/${courseId}/${sectionId}`;
  const prevNav = prevItemId ? { href: `${base}?itemId=${prevItemId}` } : null;
  const nextNav = nextItemId ? { href: `${base}?itemId=${nextItemId}` } : null;

  // Progress: (completed lectures + fraction of current lecture) / total lectures
  const progress =
    lectures.length > 0 && currentLectureIndex !== -1
      ? (currentLectureIndex +
          (lectureItems.length > 0 ? (currentItemIndex + 1) / lectureItems.length : 0)) /
        lectures.length
      : 0;
  const { mutateAsync: submitCode } = useSubmitCodeMutation();
  const { mutateAsync: saveLectureProgress } = useSaveLectureProgressMutation();
  const { mutateAsync: saveItemProgress } = useSaveItemProgressMutation();

  const language = item?.contentJson?.language ?? 'python';
  const isCodingItem = item?.itemType === 'CODING_SET';

  // Reset UI state on item navigation
  useEffect(() => {
    if (!itemId) return;
    setShowCorrectModal(false);
    setRunResult(null);
    setIsCompleted(false);
  }, [itemId]);

  // Load code + sync isCompleted when DB data arrives/changes
  useEffect(() => {
    if (!itemId) return;
    // Code: localStorage draft > last DB submission > starter code > placeholder
    const draft = localStorage.getItem(`codehaja_code_${itemId}`);
    if (draft !== null) {
      setCode(draft);
    } else if (latestSubmissionResponse?.data?.sourceCode) {
      setCode(latestSubmissionResponse.data.sourceCode);
    } else if (item?.contentJson?.starterCode) {
      setCode(item.contentJson.starterCode);
    } else {
      setCode(PLACEHOLDER);
    }
    // isCompleted: lecture must be COMPLETED in DB; coding items also require a PASSED submission
    const progressCompleted = currentLectureProgressResponse?.data?.status === 'COMPLETED';
    const alreadyCompleted = progressCompleted &&
      (!isCodingItem || latestSubmissionResponse?.data?.submissionStatus === 'PASSED');
    setIsCompleted(alreadyCompleted);
  }, [itemId, item?.contentJson?.starterCode, currentLectureProgressResponse, latestSubmissionResponse]);

  // Auto-save code draft to localStorage
  useEffect(() => {
    if (!itemId || code === PLACEHOLDER) return;
    const t = setTimeout(() => {
      localStorage.setItem(`codehaja_code_${itemId}`, code);
    }, 800);
    return () => clearTimeout(t);
  }, [code, itemId]);

  // Save resume position + mark item as visited (in-session only)
  useEffect(() => {
    if (!courseId || !sectionId || !itemId) return;
    localStorage.setItem(`codehaja_resume_${courseId}`, JSON.stringify({ sectionId, itemId }));

    setVisitedItemIds((prev) => {
      if (prev.has(itemId)) return prev;
      const next = new Set(prev);
      next.add(itemId);
      return next;
    });
  }, [courseId, sectionId, itemId]);

  async function handleRun() {
    if (!itemId || isRunning || isGrading) return;
    setOutputMode('run');
    setIsRunning(true);
    try {
      const res = await submitCode({ entryId: itemId, payload: { sourceCode: code, language } });
      setRunResult(res?.data ?? res);
    } catch (e) {
      setRunResult({ stderr: String(e?.message ?? '실행 실패') });
    } finally {
      setIsRunning(false);
    }
  }

  async function handleGrade() {
    if (!itemId || isRunning || isGrading) return;
    setOutputMode('grade');
    setIsGrading(true);
    try {
      const res = await submitCode({ entryId: itemId, payload: { sourceCode: code, language } });
      const result = res?.data ?? res;
      setRunResult(result);
      queryClient.invalidateQueries({ queryKey: ['latest-submission', itemId] });
      if (result?.submissionStatus === 'PASSED') {
        setPassedItemIds((prev) => { const n = new Set(prev); n.add(itemId); return n; });
        queryClient.invalidateQueries({ queryKey: ['passed-coding-items', courseId] });
        await handleComplete();
        setShowCorrectModal(true);
      }
    } catch (e) {
      setRunResult({ stderr: String(e?.message ?? '채점 실패') });
    } finally {
      setIsGrading(false);
    }
  }

  async function handleComplete() {
    if (!itemId || !item) return;
    await saveItemProgress({ itemId, courseId });
    const isLastItem = lectureItems.length > 0 && currentItemIndex === lectureItems.length - 1;
    if (item.lectureId && isLastItem) {
      await saveLectureProgress({ lectureId: item.lectureId, payload: { status: 'COMPLETED' } });
      queryClient.invalidateQueries({ queryKey: ['course-lecture-progress', courseId] });
      queryClient.invalidateQueries({ queryKey: ['lecture-progress', item.lectureId] });
      setVisitedLectureIds((prev) => {
        if (prev.has(item.lectureId)) return prev;
        const next = new Set(prev);
        next.add(item.lectureId);
        return next;
      });
    }
    setIsCompleted(true);
  }

  if (!itemId) {
    return (
      <div className='h-full flex items-center justify-center text-[#5a5a72] text-sm'>
        불러오는 중...
      </div>
    );
  }

  if (isLoading) {
    return (
      <div className='h-full flex items-center justify-center text-[#5a5a72] text-sm'>
        불러오는 중...
      </div>
    );
  }

  function handleCorrectNext() {
    setShowCorrectModal(false);
    if (nextItemId) {
      router.push(`${base}?itemId=${nextItemId}`);
    }
  }

  return (
    <>
    <CorrectModal
      open={showCorrectModal}
      onNext={handleCorrectNext}
      onClose={() => setShowCorrectModal(false)}
      hasNext={!!nextItemId}
    />
    <LearnLayout
      topBar={
        <LearnTopBar
          courseId={courseId}
          sectionId={sectionId}
          prevLecture={prevNav}
          nextLecture={nextNav}
          progress={progress}
          isRunning={isRunning}
          isGrading={isGrading}
          onRun={handleRun}
          onGrade={handleGrade}
          isCompleted={isCompleted}
          onComplete={isCodingItem ? null : handleComplete}
          sidebarOpen={sidebarOpen}
          onToggleSidebar={() => setSidebarOpen((v) => !v)}
        />
      }
      sidebar={
        sidebarOpen ? (
          <LearnSidebar
            courseId={courseId}
            activeLectureId={item?.lectureId ?? null}
            activeItemId={itemId}
            visitedLectureIds={visitedLectureIds}
            visitedItemIds={visitedItemIds}
            passedItemIds={passedItemIds}
            onClose={() => setSidebarOpen(false)}
          />
        ) : null
      }
      leftPanel={<LessonContentPanel item={item} />}
      rightPanel={
        isCodingItem ? (
          <CodeEditorPanel
            item={item}
            code={code}
            onCodeChange={setCode}
            runResult={runResult}
            outputMode={outputMode}
          />
        ) : null
      }
    />
    </>
  );
}

export default function LearnPage() {
  return (
    <Suspense>
      <LearnPageInner />
    </Suspense>
  );
}
