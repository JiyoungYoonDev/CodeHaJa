package com.codehaja.domain.generation.service;

import com.codehaja.domain.generation.entity.TopicCategory;
import org.springframework.stereotype.Component;

/**
 * Classifies a topic string into a TopicCategory.
 * Determines which prompt overlays and pedagogical rules to apply.
 */
@Component
public class TopicClassifier {

    private static final String[] MATH_KEYWORDS = {
            "math", "calculus", "algebra", "geometry", "trigonometry",
            "statistics", "probability", "linear algebra", "differential",
            "integral", "physics", "chemistry", "precalculus", "pre-calculus",
            "pre calculus", "discrete math", "number theory", "topology",
            "수학", "미적분", "대수", "기하", "통계", "확률", "물리", "화학"
    };

    private static final String[] ALGO_KEYWORDS = {
            "algorithm", "data structure", "leetcode", "coding interview",
            "two pointer", "sliding window", "binary search", "dynamic programming",
            "greedy", "backtracking", "graph", "tree", "dfs", "bfs",
            "sorting", "linked list", "stack", "queue", "heap", "hash",
            "recursion", "divide and conquer", "topological sort",
            "알고리즘", "자료구조", "코딩 인터뷰", "코딩인터뷰", "코딩 테스트", "코딩테스트",
            "투 포인터", "슬라이딩 윈도우", "이진 탐색", "다이나믹 프로그래밍",
            "그리디", "백트래킹", "그래프", "트리", "정렬", "연결 리스트",
            "스택", "큐", "힙", "해시", "재귀"
    };

    private static final String[] INTERVIEW_KEYWORDS = {
            "interview prep", "interview preparation", "technical interview",
            "system design interview", "low level design", "api design interview",
            "interview questions", "면접", "기술 면접", "면접 준비", "인터뷰 준비",
            "interview guide", "senior interview", "junior interview",
            "backend interview", "frontend interview", "java interview",
            "spring interview", "python interview", "javascript interview"
    };

    public TopicCategory classify(String topic) {
        if (topic == null) return TopicCategory.GENERAL_PROGRAMMING;
        String lower = topic.toLowerCase();

        if (matchesAny(lower, MATH_KEYWORDS)) return TopicCategory.MATH_SCIENCE;
        if (matchesAny(lower, ALGO_KEYWORDS)) return TopicCategory.ALGORITHM;
        if (matchesAny(lower, INTERVIEW_KEYWORDS)) return TopicCategory.TECHNICAL_INTERVIEW;
        return TopicCategory.GENERAL_PROGRAMMING;
    }

    public boolean isMathOrScience(String topic) {
        return classify(topic) == TopicCategory.MATH_SCIENCE;
    }

    public boolean isAlgorithm(String topic) {
        return classify(topic) == TopicCategory.ALGORITHM;
    }

    private static boolean matchesAny(String lower, String[] keywords) {
        for (String kw : keywords) {
            if (lower.contains(kw)) return true;
        }
        return false;
    }
}
