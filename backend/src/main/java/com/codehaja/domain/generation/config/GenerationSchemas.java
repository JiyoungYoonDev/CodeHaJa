package com.codehaja.domain.generation.config;

/**
 * Gemini responseSchema definitions for structured JSON output.
 * These match the expected DTO shapes so Gemini returns clean, parseable JSON.
 */
public final class GenerationSchemas {

    private GenerationSchemas() {}

    /**
     * Schema for Phase 1: course outline structure.
     * Matches CourseGenerationDto.GeneratedCourse.
     */
    public static final String COURSE_OUTLINE = """
            {
              "type": "OBJECT",
              "properties": {
                "title": { "type": "STRING" },
                "description": { "type": "STRING" },
                "difficulty": {
                  "type": "STRING",
                  "enum": ["BEGINNER", "INTERMEDIATE", "ADVANCED", "EXPERT", "PROFESSIONAL"]
                },
                "sections": {
                  "type": "ARRAY",
                  "items": {
                    "type": "OBJECT",
                    "properties": {
                      "title": { "type": "STRING" },
                      "description": { "type": "STRING" },
                      "hours": { "type": "INTEGER" },
                      "points": { "type": "INTEGER" },
                      "sortOrder": { "type": "INTEGER" },
                      "lectures": {
                        "type": "ARRAY",
                        "items": {
                          "type": "OBJECT",
                          "properties": {
                            "title": { "type": "STRING" },
                            "description": { "type": "STRING" },
                            "lectureType": {
                              "type": "STRING",
                              "enum": ["TEXT", "QUIZ", "CODING"]
                            },
                            "sortOrder": { "type": "INTEGER" },
                            "durationMinutes": { "type": "INTEGER" },
                            "lectureItems": {
                              "type": "ARRAY",
                              "items": {
                                "type": "OBJECT",
                                "properties": {
                                  "title": { "type": "STRING" },
                                  "description": { "type": "STRING" },
                                  "itemType": {
                                    "type": "STRING",
                                    "enum": ["RICH_TEXT", "QUIZ_SET", "CODING_SET", "CHECKPOINT"]
                                  },
                                  "sortOrder": { "type": "INTEGER" },
                                  "points": { "type": "INTEGER" },
                                  "isRequired": { "type": "BOOLEAN" },
                                  "externalLinks": { "type": "STRING" }
                                },
                                "required": ["title", "itemType", "sortOrder"]
                              }
                            }
                          },
                          "required": ["title", "lectureType", "sortOrder"]
                        }
                      }
                    },
                    "required": ["title", "sortOrder"]
                  }
                }
              },
              "required": ["title", "sections"]
            }
            """;

    /**
     * Schema for Phase 2: lecture content entries.
     * Each item specifies its type and uses the corresponding content field:
     * - RICH_TEXT / CHECKPOINT → "content" (markdown/text string)
     * - CODING_SET → "codingContent" (structured object with testCases)
     * - QUIZ_SET → "quizContent" (array of structured quiz questions)
     */
    public static final String LECTURE_CONTENT = """
            {
              "type": "ARRAY",
              "items": {
                "type": "OBJECT",
                "properties": {
                  "itemTitle": { "type": "STRING" },
                  "itemType": {
                    "type": "STRING",
                    "enum": ["RICH_TEXT", "QUIZ_SET", "CODING_SET", "CHECKPOINT"]
                  },
                  "content": { "type": "STRING" },
                  "codingContent": {
                    "type": "OBJECT",
                    "properties": {
                      "title": { "type": "STRING" },
                      "language": { "type": "STRING" },
                      "description": { "type": "STRING" },
                      "functionName": { "type": "STRING" },
                      "starterCode": { "type": "STRING" },
                      "hint": { "type": "STRING" },
                      "evaluationStyle": {
                        "type": "STRING",
                        "enum": ["FUNCTION", "CONSOLE"]
                      },
                      "testCases": {
                        "type": "ARRAY",
                        "items": {
                          "type": "OBJECT",
                          "properties": {
                            "input": { "type": "STRING" },
                            "expectedOutput": { "type": "STRING" }
                          },
                          "required": ["input", "expectedOutput"]
                        }
                      }
                    },
                    "required": ["title", "language", "description", "starterCode", "testCases", "evaluationStyle"]
                  },
                  "quizContent": {
                    "type": "ARRAY",
                    "items": {
                      "type": "OBJECT",
                      "properties": {
                        "question": { "type": "STRING" },
                        "options": {
                          "type": "ARRAY",
                          "items": {
                            "type": "OBJECT",
                            "properties": {
                              "letter": { "type": "STRING" },
                              "text": { "type": "STRING" }
                            },
                            "required": ["letter", "text"]
                          }
                        },
                        "answer": { "type": "STRING" },
                        "explanation": { "type": "STRING" }
                      },
                      "required": ["question", "options", "answer"]
                    }
                  }
                },
                "required": ["itemTitle", "itemType"]
              }
            }
            """;
}
