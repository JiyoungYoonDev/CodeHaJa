'use client';

import { Suspense } from 'react';
import { useState, useEffect, useCallback } from 'react';
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
  useSaveQuizProgressMutation,
  useLatestQuizSubmissionQuery,
} from '../../../../../../hooks/queries/use-learn';
import { useCourseDetailQuery } from '../../../../../../hooks/queries/use-course';
import { useAuth } from '@/lib/auth-context';
import { apiFetch } from '@/lib/api-client';
import CorrectModal from '@/components/learn/CorrectModal';
import XpGainToast from '@/components/learn/XpGainToast';
import HeartLostToast from '@/components/learn/HeartLostToast';

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
const STARTER_KEY = (itemId, problemIndex) => `codehaja_starter_${itemId}_p${problemIndex}`;

/**
 * Returns { files, contentChanged }.
 * - files: saved draft object or null
 * - contentChanged: true if starter code was regenerated (old draft discarded)
 */
function loadFilesFromStorage(itemId, problemIndex, currentStarter) {
  try {
    const savedStarter = localStorage.getItem(STARTER_KEY(itemId, problemIndex));
    const raw = localStorage.getItem(FILES_KEY(itemId, problemIndex));

    if (currentStarter && raw) {
      if (!savedStarter) {
        // No starter key saved yet — discard stale draft from before this feature
        localStorage.removeItem(FILES_KEY(itemId, problemIndex));
        return { files: null, contentChanged: true };
      }
      if (savedStarter !== currentStarter) {
        // Starter code changed → content was regenerated, discard draft
        localStorage.removeItem(FILES_KEY(itemId, problemIndex));
        localStorage.removeItem(STARTER_KEY(itemId, problemIndex));
        return { files: null, contentChanged: true };
      }
    }
    return { files: raw ? JSON.parse(raw) : null, contentChanged: false };
  } catch {
    return { files: null, contentChanged: false };
  }
}

function saveStarterCode(itemId, problemIndex, starterCode) {
  try {
    if (starterCode) localStorage.setItem(STARTER_KEY(itemId, problemIndex), starterCode);
  } catch { /* ignore */ }
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
  const [contentRegenerated, setContentRegenerated] = useState(false);
  const [hearts, setHearts] = useState(5);
  const [showHeartLost, setShowHeartLost] = useState(false);
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

  // Load full course structure for cross-section navigation
  const { data: courseResponse } = useCourseDetailQuery(courseId);
  const allSections = courseResponse?.data?.sections ?? [];
  // Flat list of all lectures across all sections, preserving sectionId
  const allLectures = allSections.flatMap((s) =>
    (s.lectures ?? []).map((l) => ({ ...l, sectionId: s.id }))
  );

  const currentItemIndex = lectureItems.findIndex((i) => i.id === itemId);
  const globalLectureIndex = allLectures.findIndex((l) => l.id === item?.lectureId);

  // Prev: previous item in same lecture, or previous lecture's firstItemId (cross-section)
  const prevLectureEntry = globalLectureIndex > 0 ? allLectures[globalLectureIndex - 1] : null;
  const prevItemId =
    currentItemIndex > 0
      ? lectureItems[currentItemIndex - 1].id
      : prevLectureEntry?.firstItemId ?? null;
  const prevSectionId = currentItemIndex > 0 ? sectionId : prevLectureEntry?.sectionId ?? sectionId;

  // Next: next item in same lecture, or next lecture's firstItemId (cross-section)
  const nextLectureEntry = globalLectureIndex !== -1 && globalLectureIndex < allLectures.length - 1
    ? allLectures[globalLectureIndex + 1] : null;
  const nextItemId =
    currentItemIndex !== -1 && currentItemIndex < lectureItems.length - 1
      ? lectureItems[currentItemIndex + 1].id
      : nextLectureEntry?.firstItemId ?? null;
  const nextSectionId =
    currentItemIndex !== -1 && currentItemIndex < lectureItems.length - 1
      ? sectionId : nextLectureEntry?.sectionId ?? sectionId;

  const prevNav = prevItemId ? { href: `/learn/${courseId}/${prevSectionId}?itemId=${prevItemId}` } : null;
  const nextNav = nextItemId ? { href: `/learn/${courseId}/${nextSectionId}?itemId=${nextItemId}` } : null;

  // Progress within current lecture (item-level)
  const progress =
    lectureItems.length > 0 && currentItemIndex !== -1
      ? (currentItemIndex + 1) / lectureItems.length
      : 0;

  const { mutateAsync: submitCode } = useSubmitCodeMutation();
  const { mutateAsync: saveLectureProgress } = useSaveLectureProgressMutation();
  const { mutateAsync: saveItemProgress } = useSaveItemProgressMutation();
  const { mutateAsync: submitProject } = useSubmitProjectMutation();
  const { mutateAsync: submitQuiz } = useSubmitQuizMutation();
  const { mutateAsync: saveQuizProgress } = useSaveQuizProgressMutation();

  const handleSaveQuizProgress = useCallback(
    (payload) => isQuizItem ? saveQuizProgress({ itemId, payload }) : Promise.resolve(),
    [isQuizItem, itemId, saveQuizProgress]
  );

  // Reusable heart handler — called by quiz/checkpoint on wrong answer
  // onWrongAnswer()          → calls /api/hearts/deduct (quiz: client-side check)
  // onWrongAnswer(number)    → syncs hearts from server response (checkpoint: server deducted)
  // onWrongAnswer('empty')   → shows heart empty modal (checkpoint: 422 from server)
  const handleHeartDeduction = useCallback(async (heartsOrSignal) => {
    if (typeof heartsOrSignal === 'number') {
      setHearts(heartsOrSignal);
      setShowHeartLost(true);
      return;
    }
    if (heartsOrSignal === 'empty') {
      setShowHeartEmptyModal(true);
      return;
    }
    try {
      const res = await apiFetch('/api/hearts/deduct', { method: 'POST' });
      const data = res?.data ?? res;
      if (data?.currentHearts != null) {
        setHearts(data.currentHearts);
        setShowHeartLost(true);
      }
    } catch (e) {
      if (e?.status === 422) {
        setShowHeartEmptyModal(true);
      }
    }
  }, []);

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
    setContentRegenerated(false);
    setProblemIndex(0);
  }, [itemId]);

  // ── Load files when item/problem changes ──────────────────────────────────
  useEffect(() => {
    if (!itemId || !needsEditor) return;
    setRunResult(null);

    // Current starter code — used to detect content regeneration
    const currentStarter = problemFiles[0]?.content ?? '';

    const { files: saved, contentChanged } = loadFilesFromStorage(itemId, problemIndex, currentStarter);
    setContentRegenerated(contentChanged);
    if (saved) {
      setFilesContent(saved);
      const firstKnown = problemFiles.find((f) => saved[f.name] !== undefined)?.name
        ?? problemFiles[0]?.name ?? null;
      setActiveFile(firstKnown);
      return;
    }

    // No saved draft — try latest DB submission (skip if content was regenerated)
    let defaults = buildDefaultFilesContent(problemFiles);
    if (
      !contentChanged &&
      problemIndex === 0 &&
      latestSubmissionResponse?.data?.sourceCode &&
      problemFiles.length > 0
    ) {
      defaults = { ...defaults, [problemFiles[0].name]: latestSubmissionResponse.data.sourceCode };
    }
    setFilesContent(defaults);
    setActiveFile(problemFiles[0]?.name ?? null);

    // Remember starter code so we can detect future regeneration
    saveStarterCode(itemId, problemIndex, currentStarter);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [itemId, problemIndex, needsEditor]);

  // ── Sync isCompleted when DB data arrives ────────────────────────────────
  useEffect(() => {
    if (!itemId) return;
    // Content was regenerated — old submission doesn't count
    if (contentRegenerated) { setIsCompleted(false); return; }
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
  }, [itemId, contentRegenerated, completedItemIdsResponse, latestSubmissionResponse, latestProjectSubmissionResponse, latestQuizSubmissionResponse]);

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

  function handleRun() {
    if (!itemId || isRunning || isGrading) return;
    setOutputMode('run');
    setRunResult(null);
    setIsRunning(true);
    // Interactive terminal handles execution via WebSocket
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
      if (result?.currentHearts != null) {
        if (result.currentHearts < hearts) setShowHeartLost(true);
        setHearts(result.currentHearts);
      }
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
      const res = await submitQuiz({ itemId, payload: { answers: answers ?? [], totalPoints: totalPoints ?? 0, earnedPoints: earnedPoints ?? 0 } });
      const result = res?.data ?? res;
      if (result?.currentHearts != null) setHearts(result.currentHearts);
      queryClient.invalidateQueries({ queryKey: ['latest-quiz-submission', itemId] });
    } catch (e) {
      if (e?.status === 422) {
        setShowHeartEmptyModal(true);
        return;
      }
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

    {/* Heart lost animation */}
    {showHeartLost && <HeartLostToast hearts={hearts} onClose={() => setShowHeartLost(false)} />}

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
          previousQuizSubmission={latestQuizSubmissionResponse?.data ?? null}
          onSaveQuizProgress={isQuizItem ? handleSaveQuizProgress : null}
          onWrongAnswer={handleHeartDeduction}
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
            isRunning={isRunning}
            onRunExit={() => setIsRunning(false)}
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
