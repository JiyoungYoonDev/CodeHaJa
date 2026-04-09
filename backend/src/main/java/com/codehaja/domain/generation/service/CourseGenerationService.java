package com.codehaja.domain.generation.service;

import java.util.ArrayList;
import java.util.List;

import com.codehaja.common.exception.BusinessException;
import com.codehaja.common.exception.ErrorCode;
import com.codehaja.domain.category.entity.CourseCategory;
import com.codehaja.domain.category.repository.CourseCategoryRepository;
import com.codehaja.domain.course.entity.Course;
import com.codehaja.domain.course.entity.CourseStatus;
import com.codehaja.domain.course.entity.Difficulty;
import com.codehaja.domain.course.repository.CourseRepository;
import com.codehaja.domain.generation.config.GenerationSchemas;
import com.codehaja.domain.generation.config.PromptTemplateNames;
import com.codehaja.domain.generation.dto.AiGenerationResult;
import com.codehaja.domain.generation.dto.CourseGenerationDto;
import com.codehaja.domain.generation.entity.*;
import com.codehaja.domain.lecture.entity.Lecture;
import com.codehaja.domain.lecture.entity.LectureType;
import com.codehaja.domain.lecture.repository.LectureRepository;
import com.codehaja.domain.lectureitem.entity.LectureItem;
import com.codehaja.domain.lectureitem.entity.LectureItemType;
import com.codehaja.domain.lectureitem.entity.ReviewStatus;
import com.codehaja.domain.lectureitem.repository.LectureItemRepository;
import com.codehaja.domain.section.entity.CourseSection;
import com.codehaja.domain.section.repository.CourseSectionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CourseGenerationService {

    private static final Logger log = LoggerFactory.getLogger(CourseGenerationService.class);
    private static final ObjectMapper objectMapper;
    static {
        com.fasterxml.jackson.core.StreamReadConstraints constraints =
                com.fasterxml.jackson.core.StreamReadConstraints.builder()
                        .maxNumberLength(Integer.MAX_VALUE)
                        .maxStringLength(Integer.MAX_VALUE)
                        .build();
        objectMapper = new ObjectMapper();
        objectMapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.getFactory().setStreamReadConstraints(constraints);

        // Gemini sometimes returns garbled giant numbers for integer fields.
        // Register a lenient Integer deserializer that defaults to 0 on overflow.
        com.fasterxml.jackson.databind.module.SimpleModule lenientModule = new com.fasterxml.jackson.databind.module.SimpleModule();
        lenientModule.addDeserializer(Integer.class, new com.fasterxml.jackson.databind.JsonDeserializer<Integer>() {
            @Override
            public Integer deserialize(com.fasterxml.jackson.core.JsonParser p,
                                        com.fasterxml.jackson.databind.DeserializationContext ctxt) throws java.io.IOException {
                try {
                    return p.getIntValue();
                } catch (Exception e) {
                    // Skip the garbled token and return default
                    return 0;
                }
            }
        });
        objectMapper.registerModule(lenientModule);
    }

    private final AiClient aiClient;
    private final CoursePromptBuilder promptBuilder;
    private final PromptContentProvider promptContentProvider;
    private final TopicClassifier topicClassifier;
    private final CourseContentGenerationService contentGenerationService;
    private final GenerationOutputLogger outputLogger;
    private final GenerationJobManager jobManager;
    private final CourseRepository courseRepository;
    private final CourseSectionRepository courseSectionRepository;
    private final LectureRepository lectureRepository;
    private final LectureItemRepository lectureItemRepository;
    private final CourseCategoryRepository courseCategoryRepository;

    @Transactional
    public CourseGenerationDto.GenerateResponse generateCourse(CourseGenerationDto.GenerateRequest request) {
        validateRequest(request);

        CourseCategory category = getCategory(request.getCategoryId());
        TopicCategory topicCategory = topicClassifier.classify(request.getTopic());

        // ── Create generation job (committed immediately so logger's REQUIRES_NEW can see it) ──
        CourseGenerationJob job = jobManager.createJob(request.getTopic(), getModelName());

        // ── Phase 1: Generate structure ──
        log.info("Phase 1: Generating course structure for topic: {}", request.getTopic());

        CourseGenerationDto.GeneratedCourse structure;
        try {
            structure = generateFullStructure(request, job, topicCategory);
            validateGeneratedCourse(structure);
        } catch (Exception e) {
            job.setStatus(GenerationJobStatus.FAILED);
            job.setErrorMessage(e.getMessage());
            jobManager.updateJob(job);
            throw e;
        }

        // Debug: log parsed structure stats
        for (var s : structure.getSections()) {
            int lCount = s.getLectures() != null ? s.getLectures().size() : 0;
            int iCount = s.getLectures() != null ? s.getLectures().stream()
                    .mapToInt(l -> l.getLectureItems() != null ? l.getLectureItems().size() : 0).sum() : 0;
            log.info("  Section '{}': {} lectures, {} items", s.getTitle(), lCount, iCount);
        }

        // ── Persist structure ──
        List<Long> sectionIds = new ArrayList<>();
        Course savedCourse = persistStructure(structure, category, sectionIds);

        // ── Update job with course info ──
        int totalLectures = structure.getSections().stream()
                .mapToInt(s -> s.getLectures() != null ? s.getLectures().size() : 0).sum();
        int totalItems = structure.getSections().stream()
                .flatMap(s -> s.getLectures() != null ? s.getLectures().stream() : java.util.stream.Stream.empty())
                .mapToInt(l -> l.getLectureItems() != null ? l.getLectureItems().size() : 0).sum();

        job.setCourseId(savedCourse.getId());
        job.setCourseTitle(savedCourse.getTitle());
        job.setTotalLectures(totalLectures);
        job.setCompletedLectures(0);
        job.setFailedLectures(0);
        jobManager.updateJob(job);

        log.info("Phase 1 complete. courseId={}, sections={}, jobId={}. Starting background content generation.",
                savedCourse.getId(), sectionIds.size(), job.getId());

        // ── Phase 2: Generate content (async — AFTER transaction commits) ──
        final Long jobId = job.getId();
        final TopicCategory topicCat = topicCategory;
        org.springframework.transaction.support.TransactionSynchronizationManager
                .registerSynchronization(new org.springframework.transaction.support.TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        contentGenerationService.generateContentAsync(
                                savedCourse.getId(), sectionIds, topicCat, request, jobId);
                    }
                });

        // ── Return immediately ──
        CourseGenerationDto.GenerateResponse response = new CourseGenerationDto.GenerateResponse();
        response.setCourseId(savedCourse.getId());
        response.setCourseTitle(savedCourse.getTitle());
        response.setStatus("DRAFT");
        response.setTotalSections(sectionIds.size());
        response.setTotalLectures(totalLectures);
        response.setTotalLectureItems(totalItems);

        return response;
    }

    // ── Structure generation with structured output + continuation ──

    private CourseGenerationDto.GeneratedCourse generateFullStructure(
            CourseGenerationDto.GenerateRequest request, CourseGenerationJob job,
            TopicCategory topicCategory) {

        int targetSections = (request.getNumberOfSections() != null && request.getNumberOfSections() > 0)
                ? request.getNumberOfSections() : 10;

        String systemPrompt = promptBuilder.buildStructureSystemPrompt();
        String userPrompt = promptBuilder.buildStructureUserPrompt(request, topicCategory);

        // First call — with structured schema
        AiGenerationResult result = aiClient.generateStructured(
                systemPrompt, userPrompt, 65536, GenerationSchemas.COURSE_OUTLINE);

        // Determine parse strategy
        OutputParseStrategy strategy = result.structuredSchemaUsed()
                ? OutputParseStrategy.STRUCTURED_SCHEMA : OutputParseStrategy.DIRECT_JSON;

        CourseGenerationDto.GeneratedCourse structure;
        try {
            structure = parseJson(result.content());
        } catch (Exception e) {
            strategy = OutputParseStrategy.REPAIRED_JSON;
            structure = parseJson(result.content()); // repairJson runs inside parseJson
        }

        // Log the call
        PromptTemplateVersion structureVersion = promptContentProvider
                .getActiveVersion(PromptTemplateNames.COURSE_STRUCTURE_SYSTEM).orElse(null);
        GenerationOutput output = outputLogger.logSuccess(
                result, GenerationTaskType.COURSE_OUTLINE, getModelName(),
                systemPrompt, userPrompt, job, null, strategy, structureVersion);
        job.setStructureOutput(output);
        accumulateJobTokens(job, result);

        if (structure.getSections() == null) {
            return structure;
        }

        log.info("Initial structure: {} sections generated (target: {})",
                structure.getSections().size(), targetSections);

        // Continue if truncated
        int maxRetries = 3;
        int attempt = 0;
        while (structure.getSections().size() < targetSections && attempt < maxRetries) {
            attempt++;
            List<String> existingTitles = structure.getSections().stream()
                    .map(CourseGenerationDto.GeneratedSection::getTitle)
                    .toList();

            log.info("Continuation attempt {}: have {}/{} sections, generating more...",
                    attempt, existingTitles.size(), targetSections);

            String contUserPrompt = promptBuilder.buildContinuationUserPrompt(request, existingTitles, topicCategory);
            AiGenerationResult contResult = aiClient.generateStructured(
                    systemPrompt, contUserPrompt, 65536, GenerationSchemas.COURSE_OUTLINE);

            OutputParseStrategy contStrategy = contResult.structuredSchemaUsed()
                    ? OutputParseStrategy.STRUCTURED_SCHEMA : OutputParseStrategy.DIRECT_JSON;

            try {
                CourseGenerationDto.GeneratedCourse continuation = parseJson(contResult.content());
                outputLogger.logSuccess(
                        contResult, GenerationTaskType.COURSE_OUTLINE_CONTINUATION, getModelName(),
                        systemPrompt, contUserPrompt, job, null, contStrategy, structureVersion);
                accumulateJobTokens(job, contResult);

                if (continuation.getSections() != null && !continuation.getSections().isEmpty()) {
                    structure.getSections().addAll(continuation.getSections());
                    log.info("Continuation added {} sections, total now: {}",
                            continuation.getSections().size(), structure.getSections().size());
                } else {
                    log.warn("Continuation returned no sections, stopping.");
                    break;
                }
            } catch (Exception e) {
                log.warn("Continuation parse failed: {}. Using what we have.", e.getMessage());
                break;
            }
        }

        return structure;
    }

    // ── Token accumulation for job totals ──

    private void accumulateJobTokens(CourseGenerationJob job, AiGenerationResult result) {
        if (result.promptTokens() != null) {
            job.setTotalPromptTokens(
                    (job.getTotalPromptTokens() != null ? job.getTotalPromptTokens() : 0) + result.promptTokens());
        }
        if (result.candidatesTokens() != null) {
            job.setTotalCompletionTokens(
                    (job.getTotalCompletionTokens() != null ? job.getTotalCompletionTokens() : 0) + result.candidatesTokens());
        }
        job.setTotalLatencyMs(
                (job.getTotalLatencyMs() != null ? job.getTotalLatencyMs() : 0L) + result.latencyMs());

        var cost = GenerationOutputLogger.estimateCost(
                getModelName(), result.promptTokens(), result.candidatesTokens(), result.thinkingTokens());
        if (cost != null) {
            job.setTotalCostUsd(
                    job.getTotalCostUsd() != null ? job.getTotalCostUsd().add(cost) : cost);
        }
    }

    private String getModelName() {
        if (aiClient instanceof GeminiApiClient gemini) {
            return gemini.getModel();
        }
        return "unknown";
    }

    // ── Parsing ──

    private CourseGenerationDto.GeneratedCourse parseJson(String aiOutput) {
        String cleaned = cleanJsonOutput(aiOutput);
        try {
            return objectMapper.readValue(cleaned, CourseGenerationDto.GeneratedCourse.class);
        } catch (Exception e) {
            log.warn("First parse failed, attempting repair: {}", e.getMessage());
        }

        String repaired = repairJson(cleaned);
        try {
            return objectMapper.readValue(repaired, CourseGenerationDto.GeneratedCourse.class);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.AI_INVALID_OUTPUT,
                    "AI output is not valid JSON: " + e.getMessage());
        }
    }

    private String cleanJsonOutput(String output) {
        String cleaned = output.strip();
        if (cleaned.startsWith("```")) {
            cleaned = cleaned.replaceFirst("^```[a-zA-Z]*\\n?", "");
            cleaned = cleaned.replaceFirst("\\n?```$", "");
            cleaned = cleaned.strip();
        }
        return cleaned;
    }

    private String repairJson(String json) {
        // Strategy 1: Cut at section boundaries — find last complete section
        String sectionRepair = repairAtSectionBoundary(json);
        if (sectionRepair != null) {
            try {
                CourseGenerationDto.GeneratedCourse parsed =
                        objectMapper.readValue(sectionRepair, CourseGenerationDto.GeneratedCourse.class);
                // Verify we actually got lectures (not just empty sections)
                boolean hasLectures = parsed.getSections() != null && parsed.getSections().stream()
                        .anyMatch(s -> s.getLectures() != null && !s.getLectures().isEmpty());
                if (hasLectures) {
                    log.info("JSON repaired at section boundary, kept {} chars", sectionRepair.length());
                    return sectionRepair;
                }
                log.warn("Section boundary repair parsed but has no lectures, trying character-level repair");
            } catch (Exception e) {
                log.debug("Section boundary repair failed: {}", e.getMessage());
            }
        }

        // Strategy 2: Character-level fallback
        String attempt = json;
        for (int i = 0; i < 30; i++) {
            int lastBrace = attempt.lastIndexOf('}');
            int lastBracket = attempt.lastIndexOf(']');
            int cutPoint = Math.max(lastBrace, lastBracket);
            if (cutPoint <= 0) break;

            String cut = attempt.substring(0, cutPoint + 1);
            cut = cut.replaceAll(",\\s*}", "}");
            cut = cut.replaceAll(",\\s*]", "]");

            int braces = 0, brackets = 0;
            boolean inString = false;
            boolean escaped = false;
            for (char c : cut.toCharArray()) {
                if (escaped) { escaped = false; continue; }
                if (c == '\\') { escaped = true; continue; }
                if (c == '"') { inString = !inString; continue; }
                if (inString) continue;
                if (c == '{') braces++;
                else if (c == '}') braces--;
                else if (c == '[') brackets++;
                else if (c == ']') brackets--;
            }
            StringBuilder sb = new StringBuilder(cut);
            while (brackets > 0) { sb.append(']'); brackets--; }
            while (braces > 0) { sb.append('}'); braces--; }

            try {
                String candidate = sb.toString();
                objectMapper.readValue(candidate, CourseGenerationDto.GeneratedCourse.class);
                log.info("JSON repaired successfully after cutting {} chars (attempt {})", json.length() - cutPoint, i + 1);
                return candidate;
            } catch (Exception e) {
                attempt = attempt.substring(0, cutPoint);
            }
        }

        log.error("JSON repair failed after all attempts");
        return json;
    }

    /**
     * Find the last complete section object in truncated JSON by looking for
     * the pattern: }], which closes a lectureItems array inside a lectures array.
     * Then close the remaining sections/course arrays.
     */
    private String repairAtSectionBoundary(String json) {
        // Find all positions where a section ends: "}]}" followed by comma or end
        // A complete section object ends with: ...lectureItems: [...]}]}
        // In the sections array, sections are separated by commas.
        // We look for the last occurrence of "}]}" which indicates end of a section
        // (closing lectureItems ] + closing lecture } + possibly more)

        // Find "sections" array start
        int sectionsStart = json.indexOf("\"sections\"");
        if (sectionsStart < 0) return null;

        // Track through the JSON properly, counting nested depth
        // Find positions where we're at sections array depth and see a complete section close
        int pos = json.indexOf('[', sectionsStart);
        if (pos < 0) return null;

        int depth = 0;
        boolean inString = false;
        boolean escaped = false;
        int lastSectionEnd = -1;

        for (int i = pos; i < json.length(); i++) {
            char c = json.charAt(i);
            if (escaped) { escaped = false; continue; }
            if (c == '\\' && inString) { escaped = true; continue; }
            if (c == '"') { inString = !inString; continue; }
            if (inString) continue;

            if (c == '{' || c == '[') depth++;
            else if (c == '}' || c == ']') {
                depth--;
                // depth 1 means we just closed a direct child of the sections array
                // (the sections [ is depth 1, each section {} goes to depth 2 then back to 1)
                if (depth == 1 && c == '}') {
                    lastSectionEnd = i;
                }
                if (depth == 0) {
                    // Closed the sections array itself — JSON was not truncated here
                    return null;
                }
            }
        }

        if (lastSectionEnd < 0) return null;

        // Cut after the last complete section, close the remaining structure
        String cut = json.substring(0, lastSectionEnd + 1);
        // Close: sections array ] + course object }
        cut = cut.replaceAll(",\\s*$", ""); // remove trailing comma
        cut += "]}";

        log.info("Section boundary repair: cut at pos {}, kept {} of {} chars, trimmed {} chars",
                lastSectionEnd, cut.length(), json.length(), json.length() - cut.length());
        return cut;
    }

    // ── Validation ──

    private void validateRequest(CourseGenerationDto.GenerateRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "Request body is required.");
        }
        if (request.getTopic() == null || request.getTopic().isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "Topic is required.");
        }
        if (request.getCategoryId() == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "Category ID is required.");
        }
    }

    private CourseCategory getCategory(Long categoryId) {
        return courseCategoryRepository.findById(categoryId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CATEGORY_NOT_FOUND));
    }

    private void validateGeneratedCourse(CourseGenerationDto.GeneratedCourse course) {
        if (course.getTitle() == null || course.getTitle().isBlank()) {
            throw new BusinessException(ErrorCode.AI_INVALID_OUTPUT, "Generated course has no title.");
        }
        if (course.getSections() == null || course.getSections().isEmpty()) {
            throw new BusinessException(ErrorCode.AI_INVALID_OUTPUT, "Generated course has no sections.");
        }
    }

    // ── Persistence (structure only, no content) ──

    private Course persistStructure(CourseGenerationDto.GeneratedCourse generated,
                                    CourseCategory category, List<Long> sectionIds) {
        Course course = new Course();
        course.setTitle(generated.getTitle());
        course.setDescription(generated.getDescription() != null ? generated.getDescription() : "");
        course.setCategory(category);
        course.setDifficulty(parseDifficulty(generated.getDifficulty()));
        course.setStatus(CourseStatus.DRAFT);
        course.setRating(0f);
        course.setProjectsCount(0);
        course.setHours(0);
        course.setLearnersCount(0);
        course.setBadgeType("New");
        course.setProvider("Codehaja");
        course.setImageUrl("");

        Course savedCourse = courseRepository.save(course);
        int totalHours = 0;
        int sectionOrder = 1;

        for (var genSection : generated.getSections()) {
            CourseSection section = new CourseSection();
            section.setCourse(savedCourse);
            section.setTitle(genSection.getTitle());
            section.setDescription(genSection.getDescription() != null ? genSection.getDescription() : "");
            section.setHours(genSection.getHours() != null ? genSection.getHours() : 0);
            section.setPoints(genSection.getPoints() != null ? genSection.getPoints() : 0);
            section.setSortOrder(sectionOrder++);

            CourseSection savedSection = courseSectionRepository.save(section);
            sectionIds.add(savedSection.getId());
            totalHours += savedSection.getHours();

            if (genSection.getLectures() != null) {
                int lectureOrder = 1;
                for (var genLecture : genSection.getLectures()) {
                    Lecture lecture = new Lecture();
                    lecture.setCourseSection(savedSection);
                    lecture.setTitle(genLecture.getTitle());
                    lecture.setDescription(genLecture.getDescription() != null ? genLecture.getDescription() : "");
                    lecture.setLectureType(parseLectureType(genLecture.getLectureType()));
                    lecture.setSortOrder(lectureOrder++);
                    lecture.setDurationMinutes(genLecture.getDurationMinutes() != null ? genLecture.getDurationMinutes() : 0);
                    lecture.setIsPreview(false);
                    lecture.setIsPublished(false);

                    Lecture savedLecture = lectureRepository.save(lecture);

                    if (genLecture.getLectureItems() != null) {
                        int itemOrder = 1;
                        for (var genItem : genLecture.getLectureItems()) {
                            LectureItem item = new LectureItem();
                            item.setLecture(savedLecture);
                            item.setTitle(genItem.getTitle());
                            item.setDescription(genItem.getDescription() != null ? genItem.getDescription() : "");
                            item.setItemType(parseLectureItemType(genItem.getItemType()));
                            item.setReviewStatus(ReviewStatus.DRAFT);
                            item.setSortOrder(itemOrder++);
                            item.setPoints(genItem.getPoints() != null ? genItem.getPoints() : 0);
                            item.setIsRequired(genItem.getIsRequired() != null ? genItem.getIsRequired() : true);
                            item.setExternalLinks(genItem.getExternalLinks());
                            lectureItemRepository.save(item);
                        }
                    }
                }
            }
        }

        savedCourse.setHours(totalHours);
        return savedCourse;
    }

    // ── Granular generation: Add Section / Add Lecture ──

    @Transactional
    public CourseGenerationDto.AddSectionResponse addSection(CourseGenerationDto.AddSectionRequest request) {
        if (request.getCourseId() == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "courseId is required.");
        }
        if (request.getSectionTopic() == null || request.getSectionTopic().isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "sectionTopic is required.");
        }

        Course course = courseRepository.findById(request.getCourseId())
                .orElseThrow(() -> new BusinessException(ErrorCode.COURSE_NOT_FOUND));

        List<CourseSection> existingSections = courseSectionRepository.findAllByCourseIdOrderBySortOrderAsc(course.getId());
        List<String> existingSectionTitles = existingSections.stream()
                .map(CourseSection::getTitle).toList();

        TopicCategory topicCategory = topicClassifier.classify(course.getTitle());
        CourseGenerationJob job = jobManager.createJob(request.getSectionTopic(), getModelName());
        job.setCourseId(course.getId());
        job.setCourseTitle(course.getTitle());

        String systemPrompt = promptBuilder.buildStructureSystemPrompt();
        String userPrompt = promptBuilder.buildAddSectionUserPrompt(
                course.getTitle(), course.getDescription(),
                course.getDifficulty() != null ? course.getDifficulty().name() : null,
                existingSectionTitles,
                request.getSectionTopic(), request.getNumberOfLectures(),
                request.getExtraInstructions(), topicCategory);

        AiGenerationResult result = aiClient.generateStructured(
                systemPrompt, userPrompt, 65536, GenerationSchemas.COURSE_OUTLINE);

        OutputParseStrategy strategy = result.structuredSchemaUsed()
                ? OutputParseStrategy.STRUCTURED_SCHEMA : OutputParseStrategy.DIRECT_JSON;

        CourseGenerationDto.GeneratedCourse parsed = parseJson(result.content());

        PromptTemplateVersion structureVersion = promptContentProvider
                .getActiveVersion(PromptTemplateNames.COURSE_STRUCTURE_SYSTEM).orElse(null);
        outputLogger.logSuccess(result, GenerationTaskType.SECTION_OUTLINE, getModelName(),
                systemPrompt, userPrompt, job, null, strategy, structureVersion);
        accumulateJobTokens(job, result);

        if (parsed.getSections() == null || parsed.getSections().isEmpty()) {
            job.setStatus(GenerationJobStatus.FAILED);
            job.setErrorMessage("AI returned no sections");
            jobManager.updateJob(job);
            throw new BusinessException(ErrorCode.AI_INVALID_OUTPUT, "AI returned no sections.");
        }

        // Take only the first section
        CourseGenerationDto.GeneratedSection genSection = parsed.getSections().get(0);
        CourseSection savedSection = persistSectionStructure(course, genSection);

        int totalLectures = genSection.getLectures() != null ? genSection.getLectures().size() : 0;
        int totalItems = genSection.getLectures() != null
                ? genSection.getLectures().stream()
                    .mapToInt(l -> l.getLectureItems() != null ? l.getLectureItems().size() : 0).sum()
                : 0;

        job.setTotalLectures(totalLectures);
        job.setCompletedLectures(0);
        job.setFailedLectures(0);
        jobManager.updateJob(job);

        log.info("addSection: courseId={}, new sectionId={}, lectures={}, items={}, jobId={}",
                course.getId(), savedSection.getId(), totalLectures, totalItems, job.getId());

        // Phase 2: content generation after commit
        final Long jobId = job.getId();
        final Long sectionId = savedSection.getId();
        final TopicCategory topicCat = topicCategory;
        final String courseTopic = course.getTitle();
        org.springframework.transaction.support.TransactionSynchronizationManager
                .registerSynchronization(new org.springframework.transaction.support.TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        CourseGenerationDto.GenerateRequest syntheticRequest = new CourseGenerationDto.GenerateRequest();
                        syntheticRequest.setTopic(courseTopic);
                        contentGenerationService.generateContentAsync(
                                course.getId(), List.of(sectionId), topicCat, syntheticRequest, jobId);
                    }
                });

        CourseGenerationDto.AddSectionResponse response = new CourseGenerationDto.AddSectionResponse();
        response.setCourseId(course.getId());
        response.setSectionId(savedSection.getId());
        response.setSectionTitle(savedSection.getTitle());
        response.setTotalLectures(totalLectures);
        response.setTotalLectureItems(totalItems);
        return response;
    }

    @Transactional
    public CourseGenerationDto.AddLectureResponse addLecture(CourseGenerationDto.AddLectureRequest request) {
        if (request.getSectionId() == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "sectionId is required.");
        }
        if (request.getLectureTopic() == null || request.getLectureTopic().isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "lectureTopic is required.");
        }

        CourseSection section = courseSectionRepository.findWithCourseById(request.getSectionId())
                .orElseThrow(() -> new BusinessException(ErrorCode.SECTION_NOT_FOUND));
        Course course = section.getCourse();

        List<Lecture> existingLectures = lectureRepository.findAllByCourseSectionIdOrderBySortOrderAsc(section.getId());
        List<String> existingLectureTitles = existingLectures.stream()
                .map(Lecture::getTitle).toList();

        TopicCategory topicCategory = topicClassifier.classify(course.getTitle());
        CourseGenerationJob job = jobManager.createJob(request.getLectureTopic(), getModelName());
        job.setCourseId(course.getId());
        job.setCourseTitle(course.getTitle());

        String systemPrompt = promptBuilder.buildStructureSystemPrompt();
        String userPrompt = promptBuilder.buildAddLectureUserPrompt(
                course.getTitle(), section.getTitle(),
                existingLectureTitles,
                request.getLectureTopic(), request.getExtraInstructions(),
                topicCategory);

        AiGenerationResult result = aiClient.generateStructured(
                systemPrompt, userPrompt, 65536, GenerationSchemas.COURSE_OUTLINE);

        OutputParseStrategy strategy = result.structuredSchemaUsed()
                ? OutputParseStrategy.STRUCTURED_SCHEMA : OutputParseStrategy.DIRECT_JSON;

        CourseGenerationDto.GeneratedCourse parsed = parseJson(result.content());

        PromptTemplateVersion structureVersion = promptContentProvider
                .getActiveVersion(PromptTemplateNames.COURSE_STRUCTURE_SYSTEM).orElse(null);
        outputLogger.logSuccess(result, GenerationTaskType.LECTURE_OUTLINE, getModelName(),
                systemPrompt, userPrompt, job, null, strategy, structureVersion);
        accumulateJobTokens(job, result);

        if (parsed.getSections() == null || parsed.getSections().isEmpty()
                || parsed.getSections().get(0).getLectures() == null
                || parsed.getSections().get(0).getLectures().isEmpty()) {
            job.setStatus(GenerationJobStatus.FAILED);
            job.setErrorMessage("AI returned no lectures");
            jobManager.updateJob(job);
            throw new BusinessException(ErrorCode.AI_INVALID_OUTPUT, "AI returned no lectures.");
        }

        CourseGenerationDto.GeneratedLecture genLecture = parsed.getSections().get(0).getLectures().get(0);
        Lecture savedLecture = persistLectureStructure(section, genLecture);

        int totalItems = genLecture.getLectureItems() != null ? genLecture.getLectureItems().size() : 0;

        job.setTotalLectures(1);
        job.setCompletedLectures(0);
        job.setFailedLectures(0);
        jobManager.updateJob(job);

        log.info("addLecture: sectionId={}, new lectureId={}, items={}, jobId={}",
                section.getId(), savedLecture.getId(), totalItems, job.getId());

        // Phase 2: content generation after commit
        final Long jobId = job.getId();
        final Long lectureId = savedLecture.getId();
        final TopicCategory topicCat = topicCategory;
        org.springframework.transaction.support.TransactionSynchronizationManager
                .registerSynchronization(new org.springframework.transaction.support.TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        contentGenerationService.generateContentForNewLectureAsync(
                                lectureId, topicCat, jobId);
                    }
                });

        CourseGenerationDto.AddLectureResponse response = new CourseGenerationDto.AddLectureResponse();
        response.setSectionId(section.getId());
        response.setLectureId(savedLecture.getId());
        response.setLectureTitle(savedLecture.getTitle());
        response.setTotalLectureItems(totalItems);
        return response;
    }

    @Transactional
    public CourseGenerationDto.AddItemResponse addItem(CourseGenerationDto.AddItemRequest request) {
        if (request.getLectureId() == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "lectureId is required.");
        }
        if (request.getItemTopic() == null || request.getItemTopic().isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "itemTopic is required.");
        }

        Lecture lecture = lectureRepository.findWithSectionAndCourseById(request.getLectureId())
                .orElseThrow(() -> new BusinessException(ErrorCode.LECTURE_NOT_FOUND));
        CourseSection section = lecture.getCourseSection();
        Course course = section.getCourse();

        List<LectureItem> existingItems = lectureItemRepository.findAllByLectureIdOrderBySortOrderAsc(lecture.getId());
        List<String> existingItemTitles = existingItems.stream()
                .map(LectureItem::getTitle).toList();

        TopicCategory topicCategory = topicClassifier.classify(course.getTitle());
        CourseGenerationJob job = jobManager.createJob(request.getItemTopic(), getModelName());
        job.setCourseId(course.getId());
        job.setCourseTitle(course.getTitle());

        String systemPrompt = promptBuilder.buildStructureSystemPrompt();
        String userPrompt = promptBuilder.buildAddItemUserPrompt(
                course.getTitle(), section.getTitle(), lecture.getTitle(),
                existingItemTitles,
                request.getItemTopic(), request.getItemType(),
                request.getExtraInstructions(), topicCategory);

        AiGenerationResult result = aiClient.generateStructured(
                systemPrompt, userPrompt, 65536, GenerationSchemas.COURSE_OUTLINE);

        OutputParseStrategy strategy = result.structuredSchemaUsed()
                ? OutputParseStrategy.STRUCTURED_SCHEMA : OutputParseStrategy.DIRECT_JSON;

        CourseGenerationDto.GeneratedCourse parsed = parseJson(result.content());

        PromptTemplateVersion structureVersion = promptContentProvider
                .getActiveVersion(PromptTemplateNames.COURSE_STRUCTURE_SYSTEM).orElse(null);
        outputLogger.logSuccess(result, GenerationTaskType.ITEM_OUTLINE, getModelName(),
                systemPrompt, userPrompt, job, null, strategy, structureVersion);
        accumulateJobTokens(job, result);

        // Extract the single item from the nested structure
        if (parsed.getSections() == null || parsed.getSections().isEmpty()
                || parsed.getSections().get(0).getLectures() == null
                || parsed.getSections().get(0).getLectures().isEmpty()
                || parsed.getSections().get(0).getLectures().get(0).getLectureItems() == null
                || parsed.getSections().get(0).getLectures().get(0).getLectureItems().isEmpty()) {
            job.setStatus(GenerationJobStatus.FAILED);
            job.setErrorMessage("AI returned no items");
            jobManager.updateJob(job);
            throw new BusinessException(ErrorCode.AI_INVALID_OUTPUT, "AI returned no items.");
        }

        CourseGenerationDto.GeneratedLectureItem genItem =
                parsed.getSections().get(0).getLectures().get(0).getLectureItems().get(0);

        Integer maxOrder = lectureItemRepository.findMaxSortOrderByLectureId(lecture.getId());
        int nextOrder = (maxOrder != null ? maxOrder : 0) + 1;

        LectureItem item = new LectureItem();
        item.setLecture(lecture);
        item.setTitle(genItem.getTitle());
        item.setDescription(genItem.getDescription() != null ? genItem.getDescription() : "");
        item.setItemType(parseLectureItemType(genItem.getItemType()));
        item.setReviewStatus(ReviewStatus.DRAFT);
        item.setSortOrder(nextOrder);
        item.setPoints(genItem.getPoints() != null ? genItem.getPoints() : 0);
        item.setIsRequired(genItem.getIsRequired() != null ? genItem.getIsRequired() : true);
        item.setExternalLinks(genItem.getExternalLinks());

        LectureItem savedItem = lectureItemRepository.save(item);

        job.setTotalLectures(0);
        job.setStatus(GenerationJobStatus.COMPLETED);
        jobManager.updateJob(job);

        log.info("addItem: lectureId={}, new itemId={}, type={}, jobId={}",
                lecture.getId(), savedItem.getId(), savedItem.getItemType(), job.getId());

        // Phase 2: generate content for the new item after commit
        final Long itemId = savedItem.getId();
        org.springframework.transaction.support.TransactionSynchronizationManager
                .registerSynchronization(new org.springframework.transaction.support.TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        try {
                            contentGenerationService.regenerateItem(itemId);
                        } catch (Exception e) {
                            log.error("Content generation failed for new itemId={}: {}", itemId, e.getMessage(), e);
                        }
                    }
                });

        CourseGenerationDto.AddItemResponse response = new CourseGenerationDto.AddItemResponse();
        response.setLectureId(lecture.getId());
        response.setItemId(savedItem.getId());
        response.setItemTitle(savedItem.getTitle());
        response.setItemType(savedItem.getItemType().name());
        return response;
    }

    // ── Persistence helpers for granular generation ──

    private CourseSection persistSectionStructure(Course course,
                                                   CourseGenerationDto.GeneratedSection genSection) {
        Integer maxOrder = courseSectionRepository.findMaxSortOrderByCourseId(course.getId());
        int nextOrder = (maxOrder != null ? maxOrder : 0) + 1;

        CourseSection section = new CourseSection();
        section.setCourse(course);
        section.setTitle(genSection.getTitle());
        section.setDescription(genSection.getDescription() != null ? genSection.getDescription() : "");
        section.setHours(genSection.getHours() != null ? genSection.getHours() : 0);
        section.setPoints(genSection.getPoints() != null ? genSection.getPoints() : 0);
        section.setSortOrder(nextOrder);

        CourseSection savedSection = courseSectionRepository.save(section);

        if (genSection.getLectures() != null) {
            int lectureOrder = 1;
            for (var genLecture : genSection.getLectures()) {
                persistLectureAndItems(savedSection, genLecture, lectureOrder++);
            }
        }

        return savedSection;
    }

    private Lecture persistLectureStructure(CourseSection section,
                                            CourseGenerationDto.GeneratedLecture genLecture) {
        Integer maxOrder = lectureRepository.findMaxSortOrderByCourseSectionId(section.getId());
        int nextOrder = (maxOrder != null ? maxOrder : 0) + 1;

        return persistLectureAndItems(section, genLecture, nextOrder);
    }

    private Lecture persistLectureAndItems(CourseSection section,
                                            CourseGenerationDto.GeneratedLecture genLecture,
                                            int sortOrder) {
        Lecture lecture = new Lecture();
        lecture.setCourseSection(section);
        lecture.setTitle(genLecture.getTitle());
        lecture.setDescription(genLecture.getDescription() != null ? genLecture.getDescription() : "");
        lecture.setLectureType(parseLectureType(genLecture.getLectureType()));
        lecture.setSortOrder(sortOrder);
        lecture.setDurationMinutes(genLecture.getDurationMinutes() != null ? genLecture.getDurationMinutes() : 0);
        lecture.setIsPreview(false);
        lecture.setIsPublished(false);

        Lecture savedLecture = lectureRepository.save(lecture);

        if (genLecture.getLectureItems() != null) {
            int itemOrder = 1;
            for (var genItem : genLecture.getLectureItems()) {
                LectureItem item = new LectureItem();
                item.setLecture(savedLecture);
                item.setTitle(genItem.getTitle());
                item.setDescription(genItem.getDescription() != null ? genItem.getDescription() : "");
                item.setItemType(parseLectureItemType(genItem.getItemType()));
                item.setReviewStatus(ReviewStatus.DRAFT);
                item.setSortOrder(itemOrder++);
                item.setPoints(genItem.getPoints() != null ? genItem.getPoints() : 0);
                item.setIsRequired(genItem.getIsRequired() != null ? genItem.getIsRequired() : true);
                item.setExternalLinks(genItem.getExternalLinks());
                lectureItemRepository.save(item);
            }
        }

        return savedLecture;
    }

    // ── Enum parsers ──

    private Difficulty parseDifficulty(String value) {
        if (value == null || value.isBlank()) return Difficulty.BEGINNER;
        try { return Difficulty.valueOf(value.toUpperCase().strip()); }
        catch (IllegalArgumentException e) { return Difficulty.BEGINNER; }
    }

    private LectureType parseLectureType(String value) {
        if (value == null || value.isBlank()) return LectureType.TEXT;
        try { return LectureType.valueOf(value.toUpperCase().strip()); }
        catch (IllegalArgumentException e) { return LectureType.TEXT; }
    }

    private LectureItemType parseLectureItemType(String value) {
        if (value == null || value.isBlank()) return LectureItemType.RICH_TEXT;
        try { return LectureItemType.valueOf(value.toUpperCase().strip()); }
        catch (IllegalArgumentException e) { return LectureItemType.RICH_TEXT; }
    }
}
