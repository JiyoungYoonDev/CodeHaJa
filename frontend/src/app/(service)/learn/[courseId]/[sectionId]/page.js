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
import ProjectRepoPanel from '@/components/learn/content/ProjectRepoPanel';
import {
  useLectureItemQuery,
  useLectureItemsQuery,
  useSubmitCodeMutation,
  useSaveLectureProgressMutation,
  useSaveItemProgressMutation,
  useSectionLecturesQuery,
  useCourseLectureProgressQuery,
  useLatestSubmissionQuery,
  usePassedCodingItemsQuery,
  useCompletedItemIdsQuery,
  useSubmitProjectMutation,
  useLatestProjectSubmissionQuery,
  useSubmitQuizMutation,
  useLatestQuizSubmissionQuery,
} from '../../../../../../hooks/queries/use-learn';
import { useAuth } from '@/lib/auth-context';
import CorrectModal from '@/components/learn/CorrectModal';
import XpGainToast from '@/components/learn/XpGainToast';

// ─── helpers ──────────────────────────────────────────────────────────────────

// Normalise a problem's files: handles new files[] and legacy fileName/starterCode
function getProblemFiles(problem) {
  if (!problem) return [];
  if (Array.isArray(problem.files) && problem.files.length > 0) return problem.files;
  // Legacy single-file format
  return [{ id: 'legacy', name: problem.fileName ?? 'main.py', content: problem.starterCode ?? '' }];
}

// Build an initial filesContent map from a problem's files array
function buildDefaultFilesContent(problemFiles) {
  return Object.fromEntries(problemFiles.map((f) => [f.name, f.content ?? '']));
}

const FILES_KEY = (itemId, problemIndex) => `codehaja_files_${itemId}_p${problemIndex}`;

function loadFilesFromStorage(itemId, problemIndex) {
  try {
    const raw = localStorage.getItem(FILES_KEY(itemId, problemIndex));
    return raw ? JSON.parse(raw) : null;
  } catch {
    return null;
  }
}

// ─── main page ────────────────────────────────────────────────────────────────

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

  // ── editor state ──────────────────────────────────────────────────────────
  // filesContent: { [fileName]: content } for the current problem
  const [filesContent, setFilesContent] = useState({});
  const [activeFile, setActiveFile] = useState(null);

  const [runResult, setRunResult] = useState(null);
  const [outputMode, setOutputMode] = useState('run');
  const [isRunning, setIsRunning] = useState(false);
  const [isGrading, setIsGrading] = useState(false);
  const [isCompleted, setIsCompleted] = useState(false);
  const [isSubmittingProject, setIsSubmittingProject] = useState(false);
  const [sidebarOpen, setSidebarOpen] = useState(true);
  const [showCorrectModal, setShowCorrectModal] = useState(false);
  const [visitedLectureIds, setVisitedLectureIds] = useState(new Set());
  const [passedItemIds, setPassedItemIds] = useState(new Set());
  const [problemIndex, setProblemIndex] = useState(0);
  const [hearts, setHearts] = useState(5);
  const [xpToastKey, setXpToastKey] = useState(0);
  const [xpToastAmount, setXpToastAmount] = useState(0);
  const [showHeartEmptyModal, setShowHeartEmptyModal] = useState(false);

  const { data: itemResponse, isLoading } = useLectureItemQuery(itemId);
  const item = itemResponse?.data ?? null;

  // Reset all user-specific state when the logged-in user changes
  useEffect(() => {
    setVisitedLectureIds(new Set());
    setPassedItemIds(new Set());
    setIsCompleted(false);
    if (user) setHearts(user.hearts ?? 5);
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

  // Per-item completion from DB
  const { data: completedItemIdsResponse } = useCompletedItemIdsQuery(courseId, { enabled: !!user });
  const completedItemIds = new Set(completedItemIdsResponse?.data ?? []);

  const isCodingItem = item?.itemType === 'CODING_SET';
  console.log('[learn] item?.itemType:', item?.itemType, 'isCodingItem:', isCodingItem);
  const isQuizItem = item?.itemType === 'QUIZ_SET';
  const isProjectItem = item?.itemType === 'PROJECT';
  const projectSubmissionType = isProjectItem ? (item?.contentJson?.submissionType ?? 'EDITOR') : null;
  const isProjectEditor = isProjectItem && projectSubmissionType === 'EDITOR';
  const isProjectRepo = isProjectItem && projectSubmissionType === 'REPO';
  const needsEditor = isCodingItem || isProjectEditor;

  // Multi-problem support
  const problems = item?.contentJson?.problems ?? null;
  const totalProblems = problems?.length ?? 1;
  const currentProblem = problems
    ? (problems[problemIndex] ?? problems[0])
    : item?.contentJson ?? null;

  const problemFiles = getProblemFiles(currentProblem);
  const language = currentProblem?.language ?? 'python';

  const { data: latestSubmissionResponse } = useLatestSubmissionQuery(itemId, {
    enabled: !!itemId && !!user && isCodingItem,
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
  const { mutateAsync: submitProject } = useSubmitProjectMutation();
  const { mutateAsync: submitQuiz } = useSubmitQuizMutation();

  const { data: latestProjectSubmissionResponse } = useLatestProjectSubmissionQuery(itemId, {
    enabled: !!itemId && !!user && isProjectItem,
  });

  const { data: latestQuizSubmissionResponse } = useLatestQuizSubmissionQuery(itemId, {
    enabled: !!itemId && !!user && isQuizItem,
  });

  // ── Reset UI state on item navigation ────────────────────────────────────
  useEffect(() => {
    if (!itemId) return;
    setShowCorrectModal(false);
    setRunResult(null);
    setIsCompleted(false);
    setProblemIndex(0);
  }, [itemId]);

  // ── Load files when item/problem changes ──────────────────────────────────
  useEffect(() => {
    if (!itemId || !needsEditor) return;
    setRunResult(null);

    const saved = loadFilesFromStorage(itemId, problemIndex);
    if (saved) {
      setFilesContent(saved);
      // Use first saved file that still exists in problemFiles, else first file
      const firstKnown = problemFiles.find((f) => saved[f.name] !== undefined)?.name
        ?? problemFiles[0]?.name ?? null;
      setActiveFile(firstKnown);
      return;
    }

    // No saved draft — try latest DB submission for problem 0 (single main file)
    let defaults = buildDefaultFilesContent(problemFiles);
    if (
      problemIndex === 0 &&
      latestSubmissionResponse?.data?.sourceCode &&
      problemFiles.length > 0
    ) {
      defaults = { ...defaults, [problemFiles[0].name]: latestSubmissionResponse.data.sourceCode };
    }
    setFilesContent(defaults);
    setActiveFile(problemFiles[0]?.name ?? null);
  }, [itemId, problemIndex]);

  // ── Sync isCompleted when DB data arrives ────────────────────────────────
  useEffect(() => {
    if (!itemId) return;
    const itemCompleted = completedItemIds.has(itemId);
    let alreadyCompleted = false;
    if (isCodingItem) {
      alreadyCompleted = itemCompleted && latestSubmissionResponse?.data?.submissionStatus === 'PASSED';
    } else if (isProjectItem) {
      alreadyCompleted = itemCompleted && !!latestProjectSubmissionResponse?.data;
    } else if (isQuizItem) {
      alreadyCompleted = itemCompleted && !!latestQuizSubmissionResponse?.data;
    } else {
      alreadyCompleted = itemCompleted;
    }
    setIsCompleted(alreadyCompleted);
  }, [itemId, completedItemIdsResponse, latestSubmissionResponse, latestProjectSubmissionResponse, latestQuizSubmissionResponse]);

  // ── Auto-save files draft to localStorage ────────────────────────────────
  useEffect(() => {
    if (!itemId || !needsEditor || Object.keys(filesContent).length === 0) return;
    const t = setTimeout(() => {
      localStorage.setItem(FILES_KEY(itemId, problemIndex), JSON.stringify(filesContent));
    }, 800);
    return () => clearTimeout(t);
  }, [filesContent, itemId, problemIndex, isCodingItem]);

  // ── Save resume position ─────────────────────────────────────────────────
  useEffect(() => {
    if (!courseId || !sectionId || !itemId) return;
    localStorage.setItem(`codehaja_resume_${courseId}`, JSON.stringify({ sectionId, itemId }));
  }, [courseId, sectionId, itemId]);

  // ── Editor change handler ─────────────────────────────────────────────────
  function handleCodeChange(newContent) {
    if (!activeFile) return;
    setFilesContent((prev) => ({ ...prev, [activeFile]: newContent }));
  }

  // ── Run / Grade ───────────────────────────────────────────────────────────
  function buildSubmissionFiles() {
    return Object.entries(filesContent).map(([name, content]) => ({ name, content }));
  }

  async function handleRun() {
    if (!itemId || isRunning || isGrading) return;
    setOutputMode('run');
    setIsRunning(true);
    try {
      const res = await submitCode({
        entryId: itemId,
        payload: {
          sourceCode: filesContent[activeFile] ?? '',
          language,
          problemIndex,
          files: buildSubmissionFiles(),
        },
      });
      setRunResult(res?.data ?? res);
    } catch (e) {
      setRunResult({ stderr: String(e?.message ?? '실행 실패') });
    } finally {
      setIsRunning(false);
    }
  }

  async function handleGrade() {
    console.log('[handleGrade] called', { itemId, isRunning, isGrading });
    if (!itemId || isRunning || isGrading) return;
    setOutputMode('grade');
    setIsGrading(true);
    try {
      const res = await submitCode({
        entryId: itemId,
        payload: {
          sourceCode: filesContent[activeFile] ?? '',
          language,
          grade: true,
          problemIndex,
          files: buildSubmissionFiles(),
        },
      });
      const result = res?.data ?? res;
      setRunResult(result);
      // Sync hearts from response
      if (result?.currentHearts != null) setHearts(result.currentHearts);
      queryClient.invalidateQueries({ queryKey: ['latest-submission', itemId] });
      if (result?.submissionStatus === 'PASSED') {
        setPassedItemIds((prev) => { const n = new Set(prev); n.add(itemId); return n; });
        queryClient.invalidateQueries({ queryKey: ['passed-coding-items', courseId] });
        if (result?.xpGained > 0) {
          setXpToastAmount(result.xpGained);
          setXpToastKey((k) => k + 1);
        }
        await handleComplete();
        setShowCorrectModal(true);
      }
    } catch (e) {
      if (e?.status === 422) {
        setShowHeartEmptyModal(true);
      } else {
        setRunResult({ stderr: String(e?.message ?? '채점 실패') });
      }
    } finally {
      setIsGrading(false);
    }
  }

  async function handleSubmitProject(payload) {
    if (!itemId || isSubmittingProject || isCompleted) return;
    setIsSubmittingProject(true);
    try {
      const submissionPayload = isProjectEditor
        ? { files: buildSubmissionFiles(), language }
        : (payload ?? {});
      await submitProject({ itemId, payload: submissionPayload });
      queryClient.invalidateQueries({ queryKey: ['latest-project-submission', itemId] });
      await handleComplete();
    } catch (e) {
      console.error('Project submission failed', e);
    } finally {
      setIsSubmittingProject(false);
    }
  }

  async function handleCompleteQuiz({ answers, totalPoints, earnedPoints } = {}) {
    if (!itemId || isCompleted) return;
    try {
      await submitQuiz({ itemId, payload: { answers: answers ?? [], totalPoints: totalPoints ?? 0, earnedPoints: earnedPoints ?? 0 } });
      queryClient.invalidateQueries({ queryKey: ['latest-quiz-submission', itemId] });
    } catch (e) {
      console.error('Quiz submission failed', e);
    }
    await handleComplete();
  }

  async function handleComplete() {
    if (!itemId || !item) return;
    const progressRes = await saveItemProgress({ itemId, courseId });
    const xpGained = progressRes?.data?.xpGained ?? 0;
    if (xpGained > 0) {
      setXpToastAmount(xpGained);
      setXpToastKey((k) => k + 1);
    }
    queryClient.invalidateQueries({ queryKey: ['completed-item-ids', courseId] });
    queryClient.invalidateQueries({ queryKey: ['completed-item-count', courseId] });
    const isLastItem = lectureItems.length > 0 && currentItemIndex === lectureItems.length - 1;
    if (item.lectureId && isLastItem) {
      await saveLectureProgress({ lectureId: item.lectureId, payload: { status: 'COMPLETED' } });
      queryClient.invalidateQueries({ queryKey: ['course-lecture-progress', courseId] });
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

    {/* Heart empty modal */}
    {showHeartEmptyModal && (
      <div className='fixed inset-0 z-50 flex items-center justify-center bg-black/60'>
        <div className='bg-[#12121e] border border-[#2a2a3e] rounded-2xl p-6 w-full max-w-xs text-center space-y-4 shadow-2xl'>
          <div className='text-4xl'>💔</div>
          <h2 className='text-white font-bold text-base'>하트가 없어요</h2>
          <p className='text-[#9090a8] text-sm leading-relaxed'>
            4시간 후 충전되거나<br />이전 강의를 복습하면 충전됩니다.
          </p>
          <button
            onClick={() => setShowHeartEmptyModal(false)}
            className='w-full py-2 rounded-lg bg-violet-600 hover:bg-violet-500 text-white text-sm font-semibold transition-colors'
          >
            확인
          </button>
        </div>
      </div>
    )}

    {/* XP gain animation */}
    {xpToastAmount > 0 && <XpGainToast key={xpToastKey} xp={xpToastAmount} />}

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
          onRun={needsEditor ? handleRun : null}
          onGrade={isCodingItem ? handleGrade : null}
          isCompleted={isCompleted}
          onComplete={(isCodingItem || isProjectItem) ? null : handleComplete}
          sidebarOpen={sidebarOpen}
          onToggleSidebar={() => setSidebarOpen((v) => !v)}
          itemType={item?.itemType}
          submissionType={projectSubmissionType}
          onSubmitProject={isProjectItem ? handleSubmitProject : null}
          isSubmittingProject={isSubmittingProject}
          hearts={hearts}
        />
      }
      sidebar={
        sidebarOpen ? (
          <LearnSidebar
            courseId={courseId}
            activeLectureId={item?.lectureId ?? null}
            activeItemId={itemId}
            visitedLectureIds={visitedLectureIds}
            completedItemIds={completedItemIds}
            passedItemIds={passedItemIds}
            onClose={() => setSidebarOpen(false)}
          />
        ) : null
      }
      leftPanel={
        <LessonContentPanel
          item={item}
          onComplete={isQuizItem ? handleCompleteQuiz : handleComplete}
          isCompleted={isCompleted}
          currentProblem={currentProblem}
          problemIndex={problemIndex}
          totalProblems={totalProblems}
          onPrevProblem={() => setProblemIndex((i) => Math.max(0, i - 1))}
          onNextProblem={() => setProblemIndex((i) => Math.min(totalProblems - 1, i + 1))}
        />
      }
      rightPanel={
        needsEditor ? (
          <CodeEditorPanel
            item={item}
            currentProblem={currentProblem}
            problemFiles={problemFiles}
            filesContent={filesContent}
            activeFile={activeFile}
            onActiveFileChange={setActiveFile}
            onCodeChange={handleCodeChange}
            runResult={runResult}
            outputMode={outputMode}
          />
        ) : isProjectRepo ? (
          <ProjectRepoPanel
            contentJson={item?.contentJson}
            onSubmit={handleSubmitProject}
            isCompleted={isCompleted}
            isSubmitting={isSubmittingProject}
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
