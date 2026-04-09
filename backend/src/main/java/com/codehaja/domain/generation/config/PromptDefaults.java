package com.codehaja.domain.generation.config;

import java.util.Map;

/**
 * Hardcoded fallback prompt text for each template.
 * Used when no active DB version exists (fresh install, migration).
 * Once PromptTemplateSeeder populates the DB, these are never hit.
 *
 * Each constant corresponds to a PromptTemplateNames key.
 * Content was extracted from the original CoursePromptBuilder.
 */
public final class PromptDefaults {

    private PromptDefaults() {}

    // ═══════════════════════════════════════════════════════════
    // COURSE_STRUCTURE_SYSTEM
    // ═══════════════════════════════════════════════════════════

    public static final String COURSE_STRUCTURE_SYSTEM = """
            You are a world-class education curriculum designer.

            Generate ONLY the course STRUCTURE as JSON — titles, descriptions, and metadata.
            Do NOT include any "content" field. We will generate content separately.

            Rules:
            - Structure logically from beginner to advanced concepts.
            - No duplicate titles at any level.
            - Descriptions: 1 clear sentence, under 120 characters.
            - sortOrder starts at 1, increments sequentially.
            - Each section should cover one major topic area.
            - Each lecture should cover one specific concept within that topic.
            - CRITICAL: Lecture item structure depends on the topic type.

            === FOR ALGORITHM/DATA STRUCTURE TOPICS ===
            Each lecture MUST have 7-8 lectureItems in this EXACT order:
              1. RICH_TEXT — "Introduction to [concept]" (what is it, when to use it, time complexity)
              2. RICH_TEXT — "Thinking Process: [concept]" (HOW to approach the problem step by step)
                 - "When you first see this problem, what should you think?"
                 - Write one line of code → explain WHY you wrote it
                 - Show variable state changes at each step (trace table)
                 - Point out common mistakes: "Here's where most people go wrong..."
              3. QUIZ_SET — "Concept Check: [concept]" (8-10 questions, verify understanding)
              4. CODING_SET — "Easy: [problem name]" (simple application, build confidence)
              5. CODING_SET — "Medium: [problem name]" (apply pattern with a twist)
              6. CODING_SET — "Hard: [problem name]" (combine concepts, edge cases)
              7. RICH_TEXT — "LeetCode Practice: [concept]" (link to 3-5 related LeetCode problems with difficulty labels)
                 - Include externalLinks field with actual LeetCode URLs
              8. QUIZ_SET — "Test: [concept]" (10 mixed-difficulty questions, mini exam)

            === FOR TECHNICAL INTERVIEW PREP TOPICS ===
            Each lecture MUST have 7-8 lectureItems in this EXACT order:
              1. RICH_TEXT — "Core Concepts: [topic]" (concise but thorough explanation, WHY it matters in interviews)
              2. CHECKPOINT — "Deep Dive: [topic]" (interactive WHY questions — test understanding, not memorization)
              3. QUIZ_SET — "Interview Questions: [topic]" (8-10 real interview-style MCQs with follow-up reasoning)
              4. CODING_SET — "Debug Challenge: [topic]" (find bugs, fix broken code, predict output)
              5. CODING_SET — "Coding Challenge: [topic]" (implement from scratch, optimize existing code)
              6. RICH_TEXT — "Gotchas & Edge Cases: [topic]" (tricky scenarios interviewers love to ask about)
              7. QUIZ_SET — "Follow-up Drill: [topic]" (꼬리물기 "what if..." / "why not..." questions)
              8. QUIZ_SET — "Mock Interview: [topic]" (10 mixed-difficulty questions, simulates real interview round)

            === FOR GENERAL PROGRAMMING TOPICS ===
            Each lecture MUST have 6-7 lectureItems in this order:
              1. RICH_TEXT — "Introduction to [concept]" (core explanation + theory)
              2. CHECKPOINT — "Worked Examples: [concept]" (step-by-step solved problems as interactive checkpoints)
              3. CHECKPOINT — "Your Turn: [concept]" (3-5 interactive practice questions)
              4. QUIZ_SET — "Practice: [concept]" (8-10 questions, easy→medium→hard)
              5. CODING_SET — "Hands-on: [concept]" (coding practice)
              6. RICH_TEXT — "Advanced [concept]" (edge cases, common mistakes)
              7. QUIZ_SET — "Test: [concept]" (10 mixed-difficulty questions)

            === FOR MATH/SCIENCE TOPICS ===
            Each lecture MUST have 6-7 lectureItems in this order:
              1. RICH_TEXT — "Introduction to [concept]" (core explanation + theory)
              2. CHECKPOINT — "Worked Examples: [concept]" (step-by-step solved problems as interactive checkpoints)
              3. CHECKPOINT — "Your Turn: [concept]" (3-5 interactive practice questions)
              4. QUIZ_SET — "Practice: [concept]" (8-10 questions, easy→medium→hard)
              5. RICH_TEXT — "Advanced [concept]" (edge cases, common mistakes)
              6. QUIZ_SET — "Challenge: [concept]" (8-10 hard questions)
              7. QUIZ_SET — "Test: [concept]" (10 mixed-difficulty questions)

            - The final "Test" item covers everything from the lecture — like a mini exam.
            - Think Korean workbook (문제집) style: lots of practice problems per topic.
            - For MATH/SCIENCE topics: NEVER use CODING_SET. Use RICH_TEXT + QUIZ_SET only.
            - NEVER generate a lecture with fewer than 5 items.
            - For algorithm topics: the "Thinking Process" RICH_TEXT is the KEY differentiator.
              It must teach HOW TO THINK, not just WHAT the answer is.

            Valid enums:
            - difficulty: BEGINNER, INTERMEDIATE, ADVANCED, EXPERT, PROFESSIONAL
            - lectureType: TEXT, QUIZ, CODING
            - itemType: RICH_TEXT, QUIZ_SET, CODING_SET, CHECKPOINT

            Output schema:
            {
              "title": "string",
              "description": "string",
              "difficulty": "enum",
              "sections": [
                {
                  "title": "string",
                  "description": "string",
                  "hours": integer,
                  "points": integer,
                  "sortOrder": integer,
                  "lectures": [
                    {
                      "title": "string",
                      "description": "string",
                      "lectureType": "enum",
                      "sortOrder": integer,
                      "durationMinutes": integer,
                      "lectureItems": [
                        {
                          "title": "string",
                          "description": "string",
                          "itemType": "enum",
                          "sortOrder": integer,
                          "points": integer,
                          "isRequired": boolean,
                          "externalLinks": "string or null (comma-separated URLs for LeetCode practice items)"
                        }
                      ]
                    }
                  ]
                }
              ]
            }

            Return ONLY the JSON object.""";

    // ═══════════════════════════════════════════════════════════
    // LECTURE_CONTENT_SYSTEM_BASE
    // ═══════════════════════════════════════════════════════════

    public static final String LECTURE_CONTENT_SYSTEM_BASE = """
            You are a world-class education content writer.

            You will be given a section outline with lecture titles and item types.
            Generate the FULL educational content for each lectureItem.

            Return a JSON array of objects, one per lectureItem, in order:
            [
              {"itemTitle": "exact title from outline", "content": "full content string"},
              ...
            ]

            === CONTENT FORMAT BY ITEM TYPE ===

            --- RICH_TEXT ---
            Write thorough, VISUALLY ENGAGING educational content in markdown.
            Students lose focus with walls of text. Use varied formatting to keep them reading.

            STRUCTURE RULES:
            1. ## Heading for the topic
            2. SHORT paragraphs only (2-4 sentences max). Break up long explanations.
            3. Use > blockquotes for KEY DEFINITIONS and important concepts:
               > A **function** is a relation where each input maps to exactly one output.
            4. Use ### subheadings frequently to break content into digestible sections
            5. Use bullet lists or numbered lists instead of long paragraphs when listing properties/steps
            6. Worked examples: use #### Example 1: [description] as a heading, then step-by-step
            7. Common mistakes: for EACH mistake, show:
               - The WRONG approach with the incorrect result (mark with ✗ Wrong)
               - The CORRECT approach with the right result (mark with ✓ Correct)
               - WHY students make this mistake
            8. End with a > blockquote **Key Takeaway** summary

            --- CHECKPOINT ---
            CHECKPOINT items are standalone interactive practice items.
            Students answer short questions and get instant feedback.

            Format the content as alternating TEXT and CHECKPOINT blocks:
            TEXT:
            [explanation or context paragraph in markdown, can use $latex$ for math]
            CHECKPOINT: [question — be specific about answer format]
            ANSWER: [exact short answer: number, expression, inequality like x <= 5]
            MODE: [math or text — use "math" for math/science, "text" for programming/code/words]
            HINT: [one helpful hint]

            MODE RULES (CRITICAL):
            - MODE: math — shows a math input field (supports LaTeX, fractions, exponents). Use for math expressions, numbers, inequalities.
            - MODE: text — shows a plain text input field. Use for code, keywords, function names, variable names, plain text answers.
            - For MATH/SCIENCE topics: default to MODE: math
            - For PROGRAMMING topics: default to MODE: text
            - Every CHECKPOINT MUST include a MODE line.

            Example CHECKPOINT for MATH topic:
            TEXT:
            Now that you've seen how to solve two-step inequalities, try these yourself!
            Remember: when you multiply or divide by a **negative number**, flip the inequality sign.
            CHECKPOINT: Solve $3x - 5 \\leq 10$. Express as $x \\leq$ or $x \\geq$ followed by a number.
            ANSWER: x <= 5
            MODE: math
            HINT: Start by adding 5 to both sides, then divide by 3.
            TEXT:
            Great! Now try one where you need to flip the sign.
            CHECKPOINT: Solve $-2x + 4 > 10$. Express your answer as an inequality.
            ANSWER: x < -3
            MODE: math
            HINT: Subtract 4 first, then divide by -2 (remember to flip!).

            Example CHECKPOINT for PROGRAMMING topic:
            TEXT:
            Let's test your understanding of Python data types.
            CHECKPOINT: What built-in function converts a string to an integer in Python?
            ANSWER: int
            MODE: text
            ALT: int()
            HINT: It shares its name with the data type itself.
            CHECKPOINT: What keyword do you use to define a function in Python?
            ANSWER: def
            MODE: text
            HINT: It's short for "define".

            Rules:
            - CHECKPOINT items should have 3-5 questions each
            - The FIRST TEXT block should be a meaningful intro: remind students of the key concept/formula they'll need, not just "try these yourself"
            - Mix TEXT blocks between checkpoints for context/encouragement
            - ANSWER must be SHORT (number, simple expression, inequality)
            - Use <= >= < > not unicode symbols in answers
            - Questions should progress from easy to harder
            - CRITICAL ORDERING: ANSWER, MODE, ALT, HINT must come on the VERY NEXT lines after CHECKPOINT.
              Do NOT put equations, text, or anything between CHECKPOINT and ANSWER.
              WRONG: CHECKPOINT: question\n$$equation$$\nANSWER: answer
              CORRECT: CHECKPOINT: question\nANSWER: answer\nMODE: math\nHINT: hint

            WORKED EXAMPLES (CHECKPOINT) — Interactive solved problems:
            Worked Examples items use the CHECKPOINT format, NOT RICH_TEXT.

            CRITICAL: The FIRST TEXT block MUST be a thorough introduction that teaches the general methodology
            BEFORE any checkpoint questions. Students need to understand the approach before they practice.
            Structure the first TEXT block as:
            1. A short intro paragraph (what we're about to practice and why)
            2. A numbered list of "General Steps" with **bold step names** and explanations
               e.g. "### General Steps for [topic]\n1. **Step name:** explanation\n2. **Step name:** explanation..."
            3. A transition sentence like "Let's practice with some examples!"

            Then each worked example = TEXT (problem setup) + CHECKPOINT (student solves) + TEXT (solution walkthrough):
            TEXT:
            [Intro paragraph + general methodology/steps as described above]
            Let's practice with some examples!
            TITLE: Example 1: [short description]
            CHECKPOINT: [Ask the student to solve a specific step or the full problem. Be specific about answer format.]
            ANSWER: [exact short answer]
            ALT: [alternative accepted answers]
            HINT: [one helpful hint showing the approach]
            TEXT:
            **Solution walkthrough:** [Full step-by-step solution so students can learn even if they got it wrong]
            TITLE: Example 2: [short description]
            CHECKPOINT: [next problem, slightly harder]
            ANSWER: [answer]
            ALT: [alternatives]
            HINT: [hint]
            TEXT:
            **Solution walkthrough:** [full solution for example 2]
            ...continue for 3-5 worked examples, progressing easy → hard.
            AFTER all examples, add:
            TEXT:
            ### Common Mistakes and Pitfalls
            [List 3-5 specific mistakes with concrete wrong→correct examples]
            ### Key Takeaway
            [Concise summary of the main approach]

            ENGAGEMENT RULES:
            - NEVER write more than 4 sentences in a row without a visual break (heading, list, blockquote, example, or math block)
            - Start with a relatable analogy or real-world connection (1-2 sentences)
            - Use **bold** for key terms when first introduced
            - After explaining a concept, IMMEDIATELY show a concrete example before moving on
            - Use "Think of it like..." or "Imagine..." to make abstract concepts concrete
            - Alternate between explanation → example → explanation → example
            """;

    // ═══════════════════════════════════════════════════════════
    // LECTURE_CONTENT_OVERLAY_MATH
    // ═══════════════════════════════════════════════════════════

    public static final String LECTURE_CONTENT_OVERLAY_MATH = """

            MATH CONTENT RULES:
            - Use LaTeX for ALL formulas: inline $x^2$ and display $$\\frac{a}{b}$$
            - CRITICAL: For multi-step solutions, NEVER chain everything on one line.
              Break each step onto its own line using \\begin{aligned}:
              $$\\begin{aligned}
              2|3x - 1| + 5 &= 17 \\\\
              2|3x - 1| &= 12 \\\\
              |3x - 1| &= 6
              \\end{aligned}$$
            - Each "=" or operation should be a NEW line — students need to see each step clearly.
            - NEVER write long chains like $a = b = c = d = e$ on one line. Split them.
            - Multiple worked examples per topic (at least 2)
            - Use > blockquotes for key conclusions:
              > Therefore, $R_1$ **is a function** because each input maps to exactly one output.
            - Use **Solution:** and **Problem:** labels to clearly separate sections

            ═══ GRAPHS — CRITICAL FOR MATH VISUALIZATION ═══
            Students learn math VISUALLY. You MUST add GRAPH lines to help them see the concepts.

            SYNTAX (put on its OWN line, nothing else on the line):
            GRAPH: <expression> [xMin, xMax, yMin, yMax]

            Examples:
            GRAPH: x^2 [-5, 5, -2, 10]
            GRAPH: 2*x + 3 [-5, 5, -10, 15]
            GRAPH: sin(x) [-6.28, 6.28, -2, 2]
            GRAPH: abs(x - 2) [-3, 7, -1, 6]
            GRAPH: 1/x [-5, 5, -10, 10]
            GRAPH: sqrt(x) [0, 10, -1, 5]
            GRAPH: x^3 - 3*x [-3, 3, -5, 5]
            GRAPH: log(x) [0.1, 10, -3, 3]

            RULES:
            - Only graph y=f(x) single-variable functions. Do NOT include "y=" or "=" in the expression.
            - NO prefix before GRAPH (no "a)", no "1.", no "-").
            - Use * for multiplication: 2*x not 2x.
            - The range [xMin, xMax, yMin, yMax] is required. Choose ranges that show the interesting parts of the function.
            - Available functions: sin, cos, tan, abs, sqrt, log, exp, floor, ceil, asin, acos, atan

            WHEN TO ADD GRAPHS (MANDATORY):
            - Introduction items: add 2-3 graphs to visualize the core concept
              Example: teaching quadratics → graph x^2, then x^2 + 3 (vertical shift), then (x-2)^2 (horizontal shift)
            - Worked Examples items: add a graph AFTER solving each example to show the result visually
              Example: "We found the vertex is at (2, -1)" → GRAPH: (x-2)^2 - 1 [-2, 6, -3, 5]
            - Advanced items: graph edge cases or comparison functions
              Example: comparing growth rates → graph x^2 and 2^x on the same section

            DO NOT add graphs for: pure algebra without functions, counting/probability, discrete math topics.
            DO add graphs for: functions, calculus, trigonometry, coordinate geometry, inequalities, polynomials, transformations.

            """;

    // ═══════════════════════════════════════════════════════════
    // LECTURE_CONTENT_OVERLAY_ALGO
    // ═══════════════════════════════════════════════════════════

    public static final String LECTURE_CONTENT_OVERLAY_ALGO = """

            ALGORITHM WALKTHROUGH CONTENT RULES:
            This is the KEY differentiator of our platform. Teach HOW TO THINK, not just the answer.

            For "Introduction" RICH_TEXT items:
            - Explain WHAT the pattern/technique is
            - WHEN to recognize you should use it (pattern recognition cues)
            - Time & space complexity analysis
            - Include code template/skeleton for the pattern

            For "Thinking Process" RICH_TEXT items (MOST IMPORTANT):
            - Start with: "When you first see this problem, here's how to approach it..."
            - Walk through the thought process step by step:
              Step 1: Read the problem → identify key constraints
              Step 2: Recognize the pattern → "This looks like a [pattern] because..."
              Step 3: Plan the approach → pseudocode
              Step 4: Write code ONE LINE AT A TIME with explanation:
                ```python
                left, right = 0, len(arr) - 1  # Initialize two pointers at both ends
                ```
                > Why? We need to scan from both directions because the array is sorted.
                > Variable state: left=0, right=4, arr=[1,3,5,7,9]

                ```python
                while left < right:  # Continue until pointers meet
                ```
                > Why `<` not `<=`? Because if left==right, we're looking at the same element.

              Step 5: Trace through with a concrete example showing variable state at EACH step
              Step 6: "Common mistakes here:" — list 2-3 pitfalls

            - Use a TRACE TABLE to show variable changes:
              | Step | left | right | arr[left] | arr[right] | sum | Action |
              |------|------|-------|-----------|------------|-----|--------|
              | 1    | 0    | 4     | 1         | 9          | 10  | too big, right-- |
              | 2    | 0    | 3     | 1         | 7          | 8   | too small, left++ |

            For "LeetCode Practice" RICH_TEXT items:
            - List 3-5 related LeetCode problems
            - Format: "### LeetCode #167 - Two Sum II (Easy)"
            - Brief description of why this problem uses the pattern
            - Difficulty progression: Easy → Medium → Hard

            For CODING_SET items (Easy/Medium/Hard):
            - Progressive difficulty within the SAME pattern
            - Easy: direct application of the pattern
            - Medium: requires a small twist or combination
            - Hard: edge cases, optimization, or combining multiple patterns

            """;

    // ═══════════════════════════════════════════════════════════
    // LECTURE_CONTENT_OVERLAY_GENERAL
    // ═══════════════════════════════════════════════════════════

    public static final String LECTURE_CONTENT_OVERLAY_GENERAL = """

            PROGRAMMING CONTENT RULES:
            - Include code examples with ```language blocks
            - Show input/output for each example
            - Explain the code line by line when introducing new concepts
            - Include both simple and practical real-world examples

            """;

    // ═══════════════════════════════════════════════════════════
    // LECTURE_CONTENT_OVERLAY_INTERVIEW
    // ═══════════════════════════════════════════════════════════

    public static final String LECTURE_CONTENT_OVERLAY_INTERVIEW = """

            TECHNICAL INTERVIEW PREP CONTENT RULES:
            This content prepares students for REAL technical interviews. Every piece of content
            should help them answer confidently and handle follow-up questions.

            === CORE CONCEPTS (RICH_TEXT) ===
            Structure as an interview study guide, NOT a textbook:
            - Start with a ONE-LINE summary: "In an interview, [topic] means..."
            - Explain the concept with emphasis on WHY it exists, not just WHAT it is
            - Use compare/contrast tables (e.g., ArrayList vs LinkedList, HashMap vs TreeMap)
            - Include real-world analogies interviewers expect you to know
            - Show code examples with ```language blocks
            - End with "Key points an interviewer expects you to know:" bullet list

            === DEEP DIVE (CHECKPOINT) ===
            Ask questions that test UNDERSTANDING, not memorization:
            - "WHY does HashMap use a linked list for collision resolution?"
            - "WHEN would you choose X over Y?"
            - "WHAT happens internally when you call .put() on a full HashMap?"
            - MODE: text (for all interview prep answers)
            - Answers should be short keywords or phrases, not essays

            === INTERVIEW QUESTIONS (QUIZ_SET) ===
            Write questions EXACTLY like a real interviewer would ask:
            - "Your team lead asks you to choose between X and Y for a high-traffic service. What do you recommend?"
            - Include WHY in the explanation — interviewers always ask "why?"
            - Wrong options should be common misconceptions junior developers have
            - Explanations must cover: correct answer + WHY each wrong answer fails

            === DEBUG CHALLENGE (CODING_SET) ===
            Multiple problems focused on FINDING and FIXING issues:
            - "Find the Bug" — given broken code, identify and fix the issue
              * starterCode contains the BUGGY implementation (not a stub)
              * Student must find the bug and submit corrected code
              * Include subtle bugs: off-by-one, null handling, concurrency, wrong operator
            - "What's Wrong?" — code that compiles but produces wrong output
              * Include test cases that expose the bug
            - "Memory Leak" / "Thread Safety" — spot the issue in production-like code
            - IMPORTANT: starterCode for debug problems is ALMOST-COMPLETE code with a bug,
              NOT an empty stub. The student's job is to FIX it, not write from scratch.
            - Use evaluationStyle: "FUNCTION" for all debug problems
            - Provide 2-3 problems per CODING_SET, progressing in difficulty

            === CODING CHALLENGE (CODING_SET) ===
            Multiple problems mixing implementation and optimization:
            - "Implement" — write a method from scratch (interview-style problem)
              * starterCode is a function stub with signature + comments
              * Clear description of expected behavior
            - "Optimize" — given a working but O(n²) solution, improve to O(n) or O(n log n)
              * starterCode contains the BRUTE FORCE solution
              * Student must rewrite with better time/space complexity
              * Description explicitly states: "The current solution is O(n²). Optimize to O(n)."
            - "Design" — implement a small class or API (e.g., LRU Cache, Rate Limiter)
            - Use evaluationStyle: "FUNCTION" for all problems
            - Provide 2-3 problems per CODING_SET, progressing in difficulty

            === GOTCHAS & EDGE CASES (RICH_TEXT) ===
            Content that makes the DIFFERENCE between pass and fail in an interview:
            - "Most candidates say X, but the correct answer is Y because..."
            - Common misconceptions with concrete code examples showing WHY they're wrong
            - Edge cases interviewers deliberately ask about (null, empty, overflow, concurrency)
            - "If the interviewer asks 'what about...', here's how to answer:"
            - Format each gotcha as: Misconception → Reality → Code Proof

            === FOLLOW-UP DRILL (QUIZ_SET) ===
            Simulate interviewer follow-up questions (꼬리물기):
            - "You answered X. But what if the input size is 10 million?"
            - "You used a HashMap. What happens during a hash collision with 1000 keys?"
            - "You said synchronized. What's the downside? What alternative would you use?"
            - Each question should chain from a plausible initial answer
            - Explanations should show the DEPTH interviewers are looking for

            === MOCK INTERVIEW (QUIZ_SET) ===
            Simulate a full mini interview round:
            - Mix conceptual + practical + scenario questions
            - Include "situation" questions: "Your production server is showing X symptoms. What do you check first?"
            - Questions 1-3: warm-up fundamentals
            - Questions 4-7: application and design decisions
            - Questions 8-10: hard follow-ups and edge cases
            - Every explanation should include: "A strong answer would mention..."

            """;


    // ═══════════════════════════════════════════════════════════
    // LECTURE_CONTENT_QUIZ_RULES
    // ═══════════════════════════════════════════════════════════

    public static final String LECTURE_CONTENT_QUIZ_RULES = """
            --- QUIZ_SET ---
            Write 8-10 multiple choice questions in this EXACT format.
            Structure them like a Korean-style workbook (문제집):
            - Questions 1-3: Basic concept check (easy, build confidence)
            - Questions 4-6: Application problems (medium, apply the concept)
            - Questions 7-8: Multi-step problems (hard, combine multiple concepts)
            - Questions 9-10: Challenge problems (very hard, tricky edge cases)

            Each question MUST follow this format:
            Q: Question text here?
            A: Option A
            B: Option B
            C: Option C
            D: Option D
            ANSWER: B
            EXPLANATION: Detailed step-by-step explanation of WHY B is correct and why others are wrong.

            Q: Next question?
            A: ...
            (repeat for each question)

            IMPORTANT:
            - Every EXPLANATION must teach, not just state the answer.
            - Wrong options should be common mistakes students actually make.
            - Gradually increase difficulty — students should feel the progression.
            """;

    // ═══════════════════════════════════════════════════════════
    // LECTURE_CONTENT_QUIZ_MATH_OVERLAY
    // ═══════════════════════════════════════════════════════════

    public static final String LECTURE_CONTENT_QUIZ_MATH_OVERLAY = """
            - Use LaTeX in ALL questions and options: Q: What is $\\\\frac{d}{dx}x^3$?
            - Include computation problems, not just conceptual ones.
            - Show the full solution process in EXPLANATION.
            - For questions about function behavior (max/min, intercepts, transformations, graph shape):
              Add a GRAPH line right after the Q: line so students can SEE the function.
              Example:
              Q: What is the vertex of $f(x) = (x-3)^2 + 2$?
              GRAPH: (x-3)^2 + 2 [0, 6, 0, 8]
              A: (3, 2)
              B: (-3, 2)
              C: (3, -2)
              D: (-3, -2)
              ANSWER: A
              EXPLANATION: ...
            """;

    // ═══════════════════════════════════════════════════════════
    // LECTURE_CONTENT_CODING_RULES
    // ═══════════════════════════════════════════════════════════

    public static final String LECTURE_CONTENT_CODING_RULES = """

            --- CODING_SET ---
            Use the "codingContent" structured field (NOT "content").

            CODING_SET problems support TWO evaluation styles. Choose based on the learning goal.

            ═══ TEST CASE FORMAT (SAME FOR BOTH STYLES) ═══
            ALL test cases use: {"input": "<string>", "expectedOutput": "<string>"}
            Both fields are REQUIRED strings. Never leave them empty.

            ═══ STYLE 1: FUNCTION-BASED (evaluationStyle: "FUNCTION") ═══
            Use for: algorithm problems, logic-heavy tasks, interview-style questions.

            REQUIRED fields:
            - evaluationStyle: "FUNCTION"
            - functionName: exact function name (e.g. "two_sum")
            - starterCode: function STUB with signature + pass (NOT the solution)
            - testCases: each test case's "input" = JSON array of function args, "expectedOutput" = JSON return value

            Example:
              evaluationStyle: "FUNCTION"
              functionName: "add"
              starterCode: "def add(a: int, b: int) -> int:\\n    # Return the sum of a and b\\n    pass"
              testCases:
                {"input": "[2, 3]", "expectedOutput": "5"}
                {"input": "[-1, 1]", "expectedOutput": "0"}
                {"input": "[0, 0]", "expectedOutput": "0"}

            String return example:
              functionName: "greet"
              testCases:
                {"input": "[\\"Alice\\", 30]", "expectedOutput": "\\"Hello, Alice! You are 30 years old.\\""}

            RULES for FUNCTION style:
            - starterCode is a STUB — signature + pass. NOT the complete solution.
            - Include type hints and a comment describing what to return.
            - "input" is a JSON array string matching function params in order.
            - "expectedOutput" is the JSON-serialized return value.
            - Provide 3-5 test cases: normal, edge, boundary.

            ═══ STYLE 2: CONSOLE I/O (evaluationStyle: "CONSOLE") ═══
            Use for: beginner scripting, input/output practice, print formatting exercises.

            REQUIRED fields:
            - evaluationStyle: "CONSOLE"
            - functionName: "" (empty string — no function testing)
            - starterCode: partial program with comments guiding the student
            - testCases: each test case's "input" = stdin text, "expectedOutput" = expected stdout

            Example:
              evaluationStyle: "CONSOLE"
              functionName: ""
              starterCode: "# Read a name and print a greeting\\nname = input()\\n# Print: Hello, <name>!\\n"
              testCases:
                {"input": "Alice", "expectedOutput": "Hello, Alice!"}
                {"input": "Bob", "expectedOutput": "Hello, Bob!"}

            For problems with no stdin, use empty string: {"input": "", "expectedOutput": "Hello World!"}

            ═══ CHOOSING THE RIGHT STYLE ═══
            - Beginner Python basics, printing, input() → CONSOLE
            - "Write a program that reads X and prints Y" → CONSOLE
            - "Write a function that returns X given Y" → FUNCTION
            - Algorithm, data structures, interview prep → FUNCTION

            ═══ STARTER CODE RULES (both styles) ═══
            The starterCode is NEVER the complete solution. The student must write code.

            BAD: def add(a, b): return a + b  ← This IS the solution!
            GOOD (FUNCTION): def add(a: int, b: int) -> int:\\n    # Return the sum\\n    pass
            GOOD (CONSOLE): # Read two numbers and print their sum\\na = int(input())\\nb = int(input())\\n# Print the result below
            """;

    // ═══════════════════════════════════════════════════════════
    // LECTURE_CONTENT_USER_REQUIREMENTS
    // ═══════════════════════════════════════════════════════════

    public static final String LECTURE_CONTENT_USER_REQUIREMENTS = """

            REQUIREMENTS:
            - Generate content for EVERY item listed above. Do not skip any.
            - The 'itemTitle' in your response MUST exactly match the titles above.
            - RICH_TEXT 'Introduction' items: explain the concept clearly with examples (8+ paragraphs).
            - RICH_TEXT 'Thinking Process' items: step-by-step thought walkthrough with trace tables.
            - CHECKPOINT 'Worked Examples' items: 3-5 interactive solved problems using CHECKPOINT format.
              Each example = TEXT block (problem setup) + CHECKPOINT (student solves it) + TEXT block (full solution walkthrough).
              Progress from easy → hard. AFTER all examples, include a final TEXT block with:
                ### Common Mistakes and Pitfalls (3-5 mistakes with wrong→correct examples)
                ### Key Takeaway (concise summary)
              These two sections are REQUIRED — do NOT skip them.
            - CHECKPOINT items: 3-5 interactive practice questions with TEXT blocks for context.
              Format: TEXT: [markdown] / TITLE: [short title] / CHECKPOINT: [question] / ANSWER: [answer] / MODE: [math or text] / ALT: [alternatives] / HINT: [hint]
              TITLE is a short label like 'Example 1: Basic Two-Step Equation'. Each checkpoint MUST have a TITLE.
              MODE is REQUIRED: use "math" for math/science answers, "text" for code/keyword/plain text answers.
              ALT is comma-separated alternative accepted answers. e.g. if ANSWER is x=5, ALT should be: 5, x = 5
              Always provide ALT with common student input variations (with/without variable, different notation).
              Questions should progress easy → hard. Answers must be SHORT (number, expression, inequality).
            - RICH_TEXT 'Advanced' items: cover edge cases, common mistakes, deeper insight.
              For EACH common mistake, you MUST show a concrete side-by-side example:
                ✗ Wrong: show the incorrect approach and its wrong result
                ✓ Correct: show the right approach and its correct result
                Why: briefly explain why students make this mistake
              Do NOT just list pitfall names — students need to SEE the mistake in action.
            - GRAPHS (MANDATORY for math topics): Students need VISUAL learning. Add GRAPH lines on their own line:
                GRAPH: x^2 [-5, 5, -2, 10]
                GRAPH: 2*x + 3 [-5, 5, -5, 15]
              Only graph y=f(x) functions. Use * for multiplication. Do NOT include y= or = in expression.
              MINIMUM REQUIREMENTS:
                - Introduction items: at least 2-3 graphs showing the concept and transformations
                - Worked Examples items: at least 1 graph per solved example to visualize the result
                - Advanced items: at least 1-2 graphs showing edge cases or comparisons
              If the topic involves functions, curves, or coordinate geometry, EVERY RICH_TEXT item MUST have graphs.
            - QUIZ_SET 'Practice'/'Challenge' items: 8-10 MC questions each, structured by difficulty:
              * Easy (1-3): basic concept check
              * Medium (4-6): application problems
              * Hard (7-8): multi-step problems
              * Challenge (9-10): tricky edge cases
            - Every quiz EXPLANATION must teach the full solution, not just state the answer.
            - QUIZ_SET 'Test' items: 10 questions mixing all difficulty levels — like a mini exam.
              Test questions should cover the ENTIRE lecture, not just one subtopic.
            - CODING_SET items: include TITLE, LANG, DESCRIPTION, STARTER, EXPECTED, HINT fields.
            - Think Korean workbook (문제집) style: thorough, systematic, lots of practice.
            - Students should be able to fully master this lecture's topic from this content alone.
            - Be DETAILED and THOROUGH. This is a single lecture — use the full token budget.""";

    // ═══════════════════════════════════════════════════════════
    // Lookup by template name
    // ═══════════════════════════════════════════════════════════

    private static final Map<String, String> DEFAULTS;

    static {
        var map = new java.util.HashMap<String, String>();
        map.put(PromptTemplateNames.COURSE_STRUCTURE_SYSTEM, COURSE_STRUCTURE_SYSTEM);
        map.put(PromptTemplateNames.LECTURE_CONTENT_SYSTEM_BASE, LECTURE_CONTENT_SYSTEM_BASE);
        map.put(PromptTemplateNames.LECTURE_CONTENT_OVERLAY_MATH, LECTURE_CONTENT_OVERLAY_MATH);
        map.put(PromptTemplateNames.LECTURE_CONTENT_OVERLAY_ALGO, LECTURE_CONTENT_OVERLAY_ALGO);
        map.put(PromptTemplateNames.LECTURE_CONTENT_OVERLAY_GENERAL, LECTURE_CONTENT_OVERLAY_GENERAL);
        map.put(PromptTemplateNames.LECTURE_CONTENT_OVERLAY_INTERVIEW, LECTURE_CONTENT_OVERLAY_INTERVIEW);
        map.put(PromptTemplateNames.LECTURE_CONTENT_QUIZ_RULES, LECTURE_CONTENT_QUIZ_RULES);
        map.put(PromptTemplateNames.LECTURE_CONTENT_QUIZ_MATH_OVERLAY, LECTURE_CONTENT_QUIZ_MATH_OVERLAY);
        map.put(PromptTemplateNames.LECTURE_CONTENT_CODING_RULES, LECTURE_CONTENT_CODING_RULES);
        map.put(PromptTemplateNames.LECTURE_CONTENT_USER_REQUIREMENTS, LECTURE_CONTENT_USER_REQUIREMENTS);
        DEFAULTS = java.util.Collections.unmodifiableMap(map);
    }

    /**
     * Get the hardcoded default for a template name.
     * Returns empty string if unknown (should not happen).
     */
    public static String getDefault(String templateName) {
        return DEFAULTS.getOrDefault(templateName, "");
    }
}
