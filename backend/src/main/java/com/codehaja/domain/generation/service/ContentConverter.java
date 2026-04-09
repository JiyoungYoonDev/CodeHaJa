package com.codehaja.domain.generation.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Converts AI-generated plain text/markdown into structured contentJson
 * for each LectureItem type (RICH_TEXT, QUIZ_SET, CODING_SET).
 */
@Component
public class ContentConverter {

    private static final ObjectMapper mapper = new ObjectMapper();

    private static final Pattern GRAPH_PATTERN = Pattern.compile(
            "^GRAPH:\\s*(.+?)(?:\\s*\\[([^]]+)])?\\s*$");

    // Inline: **bold**, *italic*, `code` — bold matched before italic
    private static final Pattern INLINE_PATTERN = Pattern.compile(
            "\\*\\*(.+?)\\*\\*" +   // group 1: bold
            "|\\*(.+?)\\*" +         // group 2: italic
            "|`([^`]+)`"             // group 3: inline code
    );

    private static final Pattern ORDERED_LIST_PATTERN = Pattern.compile("^(\\d+)\\.\\s+(.+)$");
    private static final Pattern UNORDERED_LIST_PATTERN = Pattern.compile("^[-*]\\s+(.+)$");

    // ── Public entry point ──

    public JsonNode convert(String itemType, String rawContent) {
        if (rawContent == null || rawContent.isBlank()) {
            return emptyDoc();
        }

        return switch (itemType.toUpperCase()) {
            case "QUIZ_SET" -> convertQuiz(rawContent);
            case "CODING_SET" -> convertCoding(rawContent);
            case "CHECKPOINT" -> convertCheckpoint(rawContent);
            default -> convertRichText(rawContent);
        };
    }

    // ── Structured conversion (schema-enforced, no text parsing) ──

    /**
     * Convert structured CODING_SET data from Gemini schema output directly to contentJson.
     * Supports LeetCode-style function testing (args/expected) and legacy stdin/stdout.
     */
    public JsonNode convertCodingStructured(Object codingContentObj) {
        try {
            JsonNode src = mapper.valueToTree(codingContentObj);
            ObjectNode root = mapper.createObjectNode();
            ArrayNode problems = root.putArray("problems");
            ObjectNode problem = mapper.createObjectNode();
            problem.put("id", "p1");
            problem.put("title", src.path("title").asText("Problem"));

            String desc = src.path("description").asText("Solve the problem.");
            ObjectNode descDoc = mapper.createObjectNode();
            descDoc.put("type", "doc");
            ArrayNode descContent = descDoc.putArray("content");
            for (String para : desc.split("\n\n?")) {
                if (!para.isBlank()) addParagraph(descContent, para.strip());
            }
            problem.set("description", descDoc);

            String lang = src.path("language").asText("python").toLowerCase();
            problem.put("language", lang);

            // Evaluation style and function name
            String evalStyle = src.path("evaluationStyle").asText("");
            if (!evalStyle.isBlank()) {
                problem.put("evaluationStyle", evalStyle);
            }
            String functionName = src.path("functionName").asText("");
            if (!functionName.isBlank()) {
                problem.put("functionName", functionName);
            }

            ArrayNode files = problem.putArray("files");
            ObjectNode file = mapper.createObjectNode();
            file.put("id", "f1");
            String starter = src.path("starterCode").asText("# Write your solution here\n");
            String cleanedStarter = stripCodeFences(starter).replace("\\n", "\n");
            file.put("name", javaFileName(lang, cleanedStarter));
            file.put("content", cleanedStarter);
            files.add(file);

            // Test cases — unified schema uses input/expectedOutput strings for both styles
            JsonNode tcNode = src.get("testCases");
            if (tcNode != null && tcNode.isArray() && !tcNode.isEmpty()) {
                boolean isFunctionBased = "FUNCTION".equalsIgnoreCase(evalStyle);

                ArrayNode testCases = mapper.createArrayNode();
                for (JsonNode tc : tcNode) {
                    ObjectNode testCase = mapper.createObjectNode();
                    String rawInput = tc.path("input").asText("");
                    String rawExpected = tc.path("expectedOutput").asText("");

                    if (isFunctionBased) {
                        // Parse JSON strings into args array + expected value
                        try {
                            JsonNode args = mapper.readTree(rawInput);
                            JsonNode expected = mapper.readTree(rawExpected);
                            testCase.set("args", args.isArray() ? args : mapper.createArrayNode().add(args));
                            testCase.set("expected", expected);
                        } catch (Exception e) {
                            // Fallback: store as stdin/stdout if JSON parsing fails
                            testCase.put("input", rawInput.replace("\\n", "\n").stripTrailing());
                            testCase.put("expectedOutput", rawExpected.replace("\\n", "\n").stripTrailing());
                        }
                    } else {
                        // CONSOLE: store as stdin/stdout directly
                        testCase.put("input", rawInput.replace("\\n", "\n").stripTrailing());
                        testCase.put("expectedOutput", rawExpected.replace("\\n", "\n").stripTrailing());
                    }
                    testCases.add(testCase);
                }
                problem.set("testCases", testCases);
            }

            String hint = src.path("hint").asText("");
            ArrayNode hints = problem.putArray("hints");
            if (!hint.isBlank()) hints.add(hint);
            problem.put("expectedOutput", "");
            problem.put("points", 50);

            problems.add(problem);
            return root;
        } catch (Exception e) {
            // Fallback to text-based parsing
            return convertCoding(codingContentObj.toString());
        }
    }

    /**
     * Convert structured QUIZ_SET data from Gemini schema output directly to contentJson.
     * No Q:/A:/ANSWER: text parsing needed.
     */
    public JsonNode convertQuizStructured(Object quizContentObj) {
        try {
            JsonNode src = mapper.valueToTree(quizContentObj);
            if (!src.isArray() || src.isEmpty()) {
                return convertQuiz(quizContentObj.toString());
            }

            ObjectNode root = mapper.createObjectNode();
            ArrayNode blocks = root.putArray("blocks");
            int qNum = 0;

            for (JsonNode q : src) {
                qNum++;
                ObjectNode block = mapper.createObjectNode();
                block.put("id", "q" + qNum);
                block.put("type", "quiz");
                block.put("quizType", "MULTIPLE_CHOICE");
                block.put("question", q.path("question").asText(""));

                String answer = q.path("answer").asText("").toLowerCase().strip();

                ArrayNode options = block.putArray("options");
                JsonNode optNode = q.get("options");
                if (optNode != null && optNode.isArray()) {
                    int optIdx = 0;
                    for (JsonNode opt : optNode) {
                        String letter = opt.path("letter").asText("").toLowerCase().strip();
                        String text = opt.path("text").asText("");
                        ObjectNode o = mapper.createObjectNode();
                        o.put("id", "q" + qNum + "_" + letter + "_" + optIdx);
                        o.put("text", text);
                        o.put("isCorrect", letter.equals(answer));
                        options.add(o);
                        optIdx++;
                    }
                }

                block.put("correctAnswer", "");
                block.put("explanation", q.path("explanation").asText(""));
                block.putArray("hints");
                block.put("points", 30);
                blocks.add(block);
            }

            return root;
        } catch (Exception e) {
            return convertQuiz(quizContentObj.toString());
        }
    }

    // ══════════════════════════════════════════════════════════════════════════════
    //  RICH_TEXT: markdown → Tiptap ProseMirror JSON
    // ══════════════════════════════════════════════════════════════════════════════

    private JsonNode convertRichText(String markdown) {
        // Decode HTML entities first
        markdown = decodeHtmlEntities(markdown);

        ObjectNode doc = mapper.createObjectNode();
        doc.put("type", "doc");
        ArrayNode content = doc.putArray("content");

        String[] lines = markdown.split("\n");
        StringBuilder codeBuffer = null;
        String codeLang = "";
        StringBuilder mathBuffer = null;  // accumulates multi-line $$...$$ blocks

        // Accumulate consecutive list items
        List<String> olItems = new ArrayList<>();
        int olStart = 1;
        List<String> ulItems = new ArrayList<>();

        for (String line : lines) {
            // ── Code block start ──
            if (codeBuffer == null && line.stripLeading().startsWith("```")) {
                flushLists(content, olItems, olStart, ulItems);
                codeBuffer = new StringBuilder();
                codeLang = line.strip().substring(3).strip();
                continue;
            }
            // ── Code block end ──
            if (codeBuffer != null && line.stripLeading().startsWith("```")) {
                addCodeBlock(content, codeLang, codeBuffer.toString());
                codeBuffer = null;
                codeLang = "";
                continue;
            }
            // ── Inside code block ──
            if (codeBuffer != null) {
                if (!codeBuffer.isEmpty()) codeBuffer.append("\n");
                codeBuffer.append(line);
                continue;
            }

            String stripped = line.strip();

            // ── Inside multi-line math block ──
            if (mathBuffer != null) {
                if (stripped.endsWith("$$")) {
                    // Closing line: strip trailing $$ and emit mathBlock
                    String tail = stripped.substring(0, stripped.length() - 2).strip();
                    if (!tail.isEmpty()) mathBuffer.append("\n").append(tail);
                    addMathBlock(content, mathBuffer.toString().strip());
                    mathBuffer = null;
                } else {
                    mathBuffer.append("\n").append(stripped);
                }
                continue;
            }

            // ── Empty line: flush lists ──
            if (stripped.isEmpty()) {
                flushLists(content, olItems, olStart, ulItems);
                continue;
            }

            // ── Display math: $$...$$ (single line) ──
            if (stripped.startsWith("$$") && stripped.endsWith("$$") && stripped.length() > 4) {
                flushLists(content, olItems, olStart, ulItems);
                String latex = stripped.substring(2, stripped.length() - 2).strip();
                addMathBlock(content, latex);
                continue;
            }

            // ── Display math: multi-line $$ block (opens with $$ but doesn't close on same line) ──
            if (stripped.startsWith("$$") && !stripped.endsWith("$$")) {
                flushLists(content, olItems, olStart, ulItems);
                mathBuffer = new StringBuilder();
                String head = stripped.substring(2).strip();
                if (!head.isEmpty()) mathBuffer.append(head);
                continue;
            }
            // Also handle bare $$ on its own line as opener
            if (stripped.equals("$$")) {
                flushLists(content, olItems, olStart, ulItems);
                mathBuffer = new StringBuilder();
                continue;
            }

            // ── Graph: GRAPH: expression [range] (with optional prefix like "a) GRAPH:") ──
            Matcher graphMatch = GRAPH_PATTERN.matcher(stripped);
            if (!graphMatch.matches()) {
                // Try stripping short prefix like "a) GRAPH:" or "1. GRAPH:"
                String graphLine = stripped.replaceFirst("^[a-zA-Z0-9]{1,3}[).]\\s*", "");
                graphMatch = GRAPH_PATTERN.matcher(graphLine);
            }
            if (graphMatch.matches()) {
                flushLists(content, olItems, olStart, ulItems);
                String expr = graphMatch.group(1).strip();
                String rangeStr = graphMatch.group(2);
                // Strip parenthetical annotations: "(Open circle at 5)" or "(Opencircleat5, shadedleft)"
                // Match parens with 4+ consecutive letters (annotation), not math like (x+4)
                expr = expr.replaceAll("\\([^)]*[A-Za-z]{4,}[^)]*\\)", "").strip();
                // Check if this is a number line inequality (x < 5, x >= -3, etc.)
                if (isNumberLineExpression(expr)) {
                    addNumberLineBlock(content, expr);
                } else if (isValidGraphExpression(expr)) {
                    addGraphBlock(content, expr, rangeStr);
                } else {
                    // Non-plottable: skip silently (context already describes the equation)
                }
                continue;
            }

            // ── Checkpoint: interactive practice block ──
            if (stripped.startsWith("CHECKPOINT:")) {
                flushLists(content, olItems, olStart, ulItems);
                String question = stripped.substring(11).strip();
                String answer = "";
                String hint = "";
                // Peek ahead for ANSWER: and HINT: lines
                int peekIdx = java.util.Arrays.asList(lines).indexOf(line);
                if (peekIdx < 0) {
                    // fallback: search from current position
                    for (int li = 0; li < lines.length; li++) {
                        if (lines[li] == line) { peekIdx = li; break; }
                    }
                }
                // We can't easily skip lines in this loop, so store checkpoint and parse ANSWER/HINT as they come
                // Instead, use a lookahead approach: scan next lines
                for (int li = peekIdx + 1; li < lines.length && li <= peekIdx + 4; li++) {
                    String peek = lines[li].strip();
                    if (peek.startsWith("ANSWER:")) {
                        answer = peek.substring(7).strip();
                        lines[li] = ""; // consume
                    } else if (peek.startsWith("HINT:")) {
                        hint = peek.substring(5).strip();
                        lines[li] = ""; // consume
                    } else if (!peek.isEmpty()) {
                        break; // stop at non-checkpoint line
                    }
                }
                addCheckpointBlock(content, "", question, answer, hint);
                continue;
            }

            // ── Headings (#### → h4, ### → h3, ## → h2, # → h1) ──
            if (stripped.startsWith("#### ")) {
                flushLists(content, olItems, olStart, ulItems);
                addHeadingWithInline(content, 4, stripped.substring(5).strip());
                continue;
            }
            if (stripped.startsWith("### ")) {
                flushLists(content, olItems, olStart, ulItems);
                addHeadingWithInline(content, 3, stripped.substring(4).strip());
                continue;
            }
            if (stripped.startsWith("## ")) {
                flushLists(content, olItems, olStart, ulItems);
                addHeadingWithInline(content, 2, stripped.substring(3).strip());
                continue;
            }
            if (stripped.startsWith("# ")) {
                flushLists(content, olItems, olStart, ulItems);
                addHeadingWithInline(content, 1, stripped.substring(2).strip());
                continue;
            }

            // ── Ordered list: 1. item ──
            Matcher olMatch = ORDERED_LIST_PATTERN.matcher(stripped);
            if (olMatch.matches()) {
                // Flush unordered list if switching type
                if (!ulItems.isEmpty()) {
                    flushUnorderedList(content, ulItems);
                }
                if (olItems.isEmpty()) {
                    olStart = Integer.parseInt(olMatch.group(1));
                }
                olItems.add(olMatch.group(2));
                continue;
            }

            // ── Unordered list: - item  or  * item (but not **bold**) ──
            Matcher ulMatch = UNORDERED_LIST_PATTERN.matcher(stripped);
            if (ulMatch.matches() && !stripped.startsWith("**")) {
                // Flush ordered list if switching type
                if (!olItems.isEmpty()) {
                    flushOrderedList(content, olItems, olStart);
                }
                ulItems.add(ulMatch.group(1));
                continue;
            }

            // ── Blockquote: > text ──
            if (stripped.startsWith("> ")) {
                flushLists(content, olItems, olStart, ulItems);
                addBlockquoteWithInline(content, stripped.substring(2).strip());
                continue;
            }

            // ── Horizontal rule: --- or *** ──
            if (stripped.matches("^[-*_]{3,}$")) {
                flushLists(content, olItems, olStart, ulItems);
                ObjectNode hr = mapper.createObjectNode();
                hr.put("type", "horizontalRule");
                content.add(hr);
                continue;
            }

            // ── Regular paragraph ──
            flushLists(content, olItems, olStart, ulItems);
            addParagraphWithInline(content, stripped);
        }

        // Flush remaining
        flushLists(content, olItems, olStart, ulItems);
        if (codeBuffer != null) {
            addCodeBlock(content, codeLang, codeBuffer.toString());
        }

        if (content.isEmpty()) {
            addParagraphWithInline(content, "");
        }

        return doc;
    }

    // ── HTML entity decoding ──

    private String decodeHtmlEntities(String text) {
        return text.replace("&gt;", ">")
                   .replace("&lt;", "<")
                   .replace("&amp;", "&")
                   .replace("&quot;", "\"")
                   .replace("&#39;", "'")
                   .replace("&nbsp;", " ")
                   // Restore LaTeX commands corrupted by JSON escape sequences
                   .replace("\t", "\\t")
                   .replace("\b", "\\b")
                   .replace("\f", "\\f")
                   .replace("\r", "\\r");
    }

    // ── Number line (inequality) detection ──

    private static final Pattern NUMBER_LINE_PATTERN = Pattern.compile(
            "^x\\s*(<=|>=|<|>)\\s*(-?\\d+(?:\\.\\d+)?)$|^(-?\\d+(?:\\.\\d+)?)\\s*(<=|>=|<|>)\\s*x$");

    private boolean isNumberLineExpression(String expr) {
        return NUMBER_LINE_PATTERN.matcher(expr.strip()).matches();
    }

    private void addNumberLineBlock(ArrayNode content, String expr) {
        Matcher m = NUMBER_LINE_PATTERN.matcher(expr.strip());
        if (!m.matches()) return;

        String operator;
        double value;
        if (m.group(1) != null) {
            // x < 5 form
            operator = m.group(1);
            value = Double.parseDouble(m.group(2));
        } else {
            // 5 > x form → flip operator
            String rawOp = m.group(4);
            value = Double.parseDouble(m.group(3));
            operator = switch (rawOp) {
                case "<" -> ">";
                case ">" -> "<";
                case "<=" -> ">=";
                case ">=" -> "<=";
                default -> rawOp;
            };
        }

        boolean closed = operator.contains("=");
        // shade left for < / <=, shade right for > / >=
        boolean shadeRight = operator.startsWith(">");

        ObjectNode node = mapper.createObjectNode();
        node.put("type", "numberLineBlock");
        ObjectNode attrs = node.putObject("attrs");
        attrs.put("expression", expr.strip());
        attrs.put("value", value);
        attrs.put("operator", operator);
        attrs.put("closed", closed);
        attrs.put("shadeRight", shadeRight);
        content.add(node);
    }

    // ── Graph validation ──

    private boolean isValidGraphExpression(String expr) {
        // Reject implicit equations (contain y as a variable, or contain = sign)
        if (expr.contains("=")) return false;

        // Reject inequalities and comparisons (not plottable as f(x))
        if (expr.contains("<") || expr.contains(">")) return false;

        // Reject expressions with parenthetical annotations (4+ consecutive letters = annotation, not math)
        if (expr.matches(".*\\([^)]*[A-Za-z]{4,}[^)]*\\).*")) return false;

        // Check for 'y' as standalone variable (not inside words like "delay")
        if (expr.matches(".*\\by\\b.*")) return false;

        // Basic sanity: should contain 'x' or be a constant
        return true;
    }

    // ── Inline markdown parsing ──

    private ArrayNode parseInline(String text) {
        ArrayNode nodes = mapper.createArrayNode();
        if (text == null || text.isEmpty()) return nodes;

        Matcher m = INLINE_PATTERN.matcher(text);
        int lastEnd = 0;

        while (m.find()) {
            // Text before match
            if (m.start() > lastEnd) {
                addTextNode(nodes, text.substring(lastEnd, m.start()), null);
            }

            if (m.group(1) != null) {
                // **bold**
                addTextNode(nodes, m.group(1), "bold");
            } else if (m.group(2) != null) {
                // *italic*
                addTextNode(nodes, m.group(2), "italic");
            } else if (m.group(3) != null) {
                // `code`
                addTextNode(nodes, m.group(3), "code");
            }

            lastEnd = m.end();
        }

        // Remaining text after last match
        if (lastEnd < text.length()) {
            addTextNode(nodes, text.substring(lastEnd), null);
        }

        // If nothing matched, add as plain text
        if (nodes.isEmpty() && !text.isEmpty()) {
            addTextNode(nodes, text, null);
        }

        return nodes;
    }

    private void addTextNode(ArrayNode parent, String text, String markType) {
        if (text == null || text.isEmpty()) return;
        ObjectNode node = mapper.createObjectNode();
        node.put("type", "text");
        node.put("text", text);
        if (markType != null) {
            ArrayNode marks = node.putArray("marks");
            ObjectNode mark = mapper.createObjectNode();
            mark.put("type", markType);
            marks.add(mark);
        }
        parent.add(node);
    }

    // ── Block builders with inline formatting ──

    private void addBlockquoteWithInline(ArrayNode content, String text) {
        ObjectNode node = mapper.createObjectNode();
        node.put("type", "blockquote");
        ArrayNode children = node.putArray("content");
        ObjectNode para = mapper.createObjectNode();
        para.put("type", "paragraph");
        if (!text.isEmpty()) {
            para.set("content", parseInline(text));
        }
        children.add(para);
        content.add(node);
    }

    private void addParagraphWithInline(ArrayNode content, String text) {
        ObjectNode node = mapper.createObjectNode();
        node.put("type", "paragraph");
        if (!text.isEmpty()) {
            node.set("content", parseInline(text));
        }
        content.add(node);
    }

    private void addHeadingWithInline(ArrayNode content, int level, String text) {
        ObjectNode node = mapper.createObjectNode();
        node.put("type", "heading");
        ObjectNode attrs = node.putObject("attrs");
        attrs.put("level", level);
        if (!text.isEmpty()) {
            node.set("content", parseInline(text));
        }
        content.add(node);
    }

    // ── List builders ──

    private void flushLists(ArrayNode content, List<String> olItems, int olStart, List<String> ulItems) {
        if (!olItems.isEmpty()) {
            flushOrderedList(content, olItems, olStart);
        }
        if (!ulItems.isEmpty()) {
            flushUnorderedList(content, ulItems);
        }
    }

    private void flushOrderedList(ArrayNode content, List<String> items, int start) {
        ObjectNode listNode = mapper.createObjectNode();
        listNode.put("type", "orderedList");
        ObjectNode attrs = listNode.putObject("attrs");
        attrs.put("start", start);
        ArrayNode listContent = listNode.putArray("content");

        for (String item : items) {
            ObjectNode listItem = mapper.createObjectNode();
            listItem.put("type", "listItem");
            ArrayNode itemContent = listItem.putArray("content");

            ObjectNode para = mapper.createObjectNode();
            para.put("type", "paragraph");
            para.set("content", parseInline(item));
            itemContent.add(para);

            listContent.add(listItem);
        }

        content.add(listNode);
        items.clear();
    }

    private void flushUnorderedList(ArrayNode content, List<String> items) {
        ObjectNode listNode = mapper.createObjectNode();
        listNode.put("type", "bulletList");
        ArrayNode listContent = listNode.putArray("content");

        for (String item : items) {
            ObjectNode listItem = mapper.createObjectNode();
            listItem.put("type", "listItem");
            ArrayNode itemContent = listItem.putArray("content");

            ObjectNode para = mapper.createObjectNode();
            para.put("type", "paragraph");
            para.set("content", parseInline(item));
            itemContent.add(para);

            listContent.add(listItem);
        }

        content.add(listNode);
        items.clear();
    }

    // ── Simple block builders (no inline parsing, for code/math) ──

    private void addCheckpointBlock(ArrayNode content, String title, String question, String answer, String hint) {
        ObjectNode node = mapper.createObjectNode();
        node.put("type", "checkpointBlock");
        ObjectNode attrs = node.putObject("attrs");
        attrs.put("title", title);
        attrs.put("question", question);
        attrs.put("answer", answer);
        attrs.put("hint", hint);
        content.add(node);
    }

    private String fixLatexEscapes(String latex) {
        // Restore LaTeX commands corrupted by JSON escape interpretation:
        // \t(ab char) was \text/\times/\theta, \b(ackspace) was \begin/\beta, etc.
        latex = latex
                .replace("\t", "\\t")
                .replace("\b", "\\b")
                .replace("\f", "\\f")
                .replace("\r", "\\r");
        // Corrupted \neq etc: \ + newline + suffix → \n + suffix
        latex = latex.replaceAll("\\\\\n(eq|abla|eg|ot(?:in)?|u(?![a-z])|i(?![a-z])|less|geq|leq|mid|ewline|sim|cong|parallel|vdash|subset|supset|prec|succ)", "\\\\n$1");
        // Newline + suffix without preceding \ (from JSON \n interpretation)
        latex = latex.replaceAll("\n(eq|abla|eg|ot(?:in)?|u(?![a-z])|i(?![a-z])|less|geq|leq|mid|ewline|sim|cong|parallel|vdash|subset|supset|prec|succ)", "\\\\n$1");
        // Remaining newlines → spaces (LaTeX uses \\ for line breaks, not literal newlines)
        latex = latex.replace("\n", " ");
        return latex;
    }

    private void addMathBlock(ArrayNode content, String latex) {
        ObjectNode node = mapper.createObjectNode();
        node.put("type", "mathBlock");
        ObjectNode attrs = node.putObject("attrs");
        attrs.put("latex", fixLatexEscapes(latex));
        content.add(node);
    }

    private void addGraphBlock(ArrayNode content, String expression, String rangeStr) {
        ObjectNode node = mapper.createObjectNode();
        node.put("type", "graphBlock");
        ObjectNode attrs = node.putObject("attrs");
        attrs.put("expression", expression);

        double xMin = -10, xMax = 10, yMin = -10, yMax = 10;
        if (rangeStr != null && !rangeStr.isBlank()) {
            String[] parts = rangeStr.split(",");
            try {
                if (parts.length >= 2) { xMin = Double.parseDouble(parts[0].strip()); xMax = Double.parseDouble(parts[1].strip()); }
                if (parts.length >= 4) { yMin = Double.parseDouble(parts[2].strip()); yMax = Double.parseDouble(parts[3].strip()); }
            } catch (NumberFormatException ignored) {}
        }
        attrs.put("xMin", xMin);
        attrs.put("xMax", xMax);
        attrs.put("yMin", yMin);
        attrs.put("yMax", yMax);
        attrs.put("width", 600);
        attrs.put("height", 400);
        content.add(node);
    }

    private void addCodeBlock(ArrayNode content, String lang, String code) {
        ObjectNode node = mapper.createObjectNode();
        node.put("type", "codeBlock");
        ObjectNode attrs = node.putObject("attrs");
        attrs.put("language", lang.isEmpty() ? "text" : lang);
        ArrayNode children = node.putArray("content");
        ObjectNode textNode = mapper.createObjectNode();
        textNode.put("type", "text");
        textNode.put("text", code);
        children.add(textNode);
        content.add(node);
    }

    // Keep for backward compat (used by addParagraph in coding desc)
    private void addParagraph(ArrayNode content, String text) {
        addParagraphWithInline(content, text);
    }

    // ══════════════════════════════════════════════════════════════════════════════
    //  CHECKPOINT: TEXT/CHECKPOINT blocks → { blocks: [...] }
    // ══════════════════════════════════════════════════════════════════════════════

    private JsonNode convertCheckpoint(String raw) {
        ObjectNode root = mapper.createObjectNode();
        ArrayNode blocks = root.putArray("blocks");

        String[] lines = raw.split("\n");
        StringBuilder textBuf = new StringBuilder();
        boolean inText = false;
        String pendingTitle = "";

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            String stripped = line.strip();

            // TEXT: block — start collecting markdown for a text block
            if (stripped.startsWith("TEXT:")) {
                // Flush any previous text buffer as a text block
                if (!textBuf.isEmpty()) {
                    addTextBlock(blocks, textBuf.toString());
                    textBuf.setLength(0);
                }
                String rest = stripped.substring(5).strip();
                if (!rest.isEmpty()) textBuf.append(rest).append("\n");
                inText = true;
                continue;
            }

            // TITLE: line — store for next checkpoint block
            if (stripped.startsWith("TITLE:")) {
                // Flush text before title
                if (!textBuf.isEmpty()) {
                    addTextBlock(blocks, textBuf.toString());
                    textBuf.setLength(0);
                    inText = false;
                }
                pendingTitle = stripped.substring(6).strip();
                continue;
            }

            // CHECKPOINT: line — flush text, create checkpoint block
            if (stripped.startsWith("CHECKPOINT:")) {
                if (!textBuf.isEmpty()) {
                    addTextBlock(blocks, textBuf.toString());
                    textBuf.setLength(0);
                    inText = false;
                }
                String question = stripped.substring(11).strip();
                String answer = "";
                String alternatives = "";
                String hint = "";
                String mode = "";
                // Peek ahead for ANSWER:, ALT:, HINT:, MODE:
                // Skip unknown lines (e.g., equations AI placed between CHECKPOINT and ANSWER)
                // Only stop at known block markers (TEXT:, TITLE:, CHECKPOINT:)
                for (int j = i + 1; j < lines.length && j <= i + 15; j++) {
                    String peek = lines[j].strip();
                    if (peek.startsWith("ANSWER:")) {
                        answer = peek.substring(7).strip();
                        lines[j] = "";
                    } else if (peek.startsWith("ALT:")) {
                        alternatives = peek.substring(4).strip();
                        lines[j] = "";
                    } else if (peek.startsWith("HINT:")) {
                        hint = peek.substring(5).strip();
                        lines[j] = "";
                    } else if (peek.startsWith("MODE:")) {
                        mode = peek.substring(5).strip().toLowerCase();
                        lines[j] = "";
                    } else if (peek.startsWith("TEXT:") || peek.startsWith("TITLE:") || peek.startsWith("CHECKPOINT:")) {
                        break;
                    }
                    // else: skip unknown lines (equations, blank lines, etc.)
                }
                ObjectNode cp = mapper.createObjectNode();
                cp.put("id", java.util.UUID.randomUUID().toString());
                cp.put("type", "checkpoint");
                cp.put("title", pendingTitle);
                cp.put("question", question);
                cp.put("answer", answer);
                cp.put("alternatives", alternatives);
                cp.put("hint", hint);
                if (!mode.isEmpty()) {
                    cp.put("inputType", mode);
                }
                blocks.add(cp);
                pendingTitle = "";
                continue;
            }

            // If we're inside a TEXT: block or just regular lines, accumulate
            if (inText || !stripped.isEmpty()) {
                textBuf.append(line).append("\n");
            }
        }

        // Flush remaining text
        if (!textBuf.isEmpty()) {
            addTextBlock(blocks, textBuf.toString());
        }

        // Ensure at least one block exists
        if (blocks.isEmpty()) {
            addTextBlock(blocks, "");
        }

        return root;
    }

    private void addTextBlock(ArrayNode blocks, String markdown) {
        ObjectNode block = mapper.createObjectNode();
        block.put("id", java.util.UUID.randomUUID().toString());
        block.put("type", "text");
        // Convert markdown to tiptap doc for the text block content
        block.set("content", convertRichText(markdown));
        blocks.add(block);
    }

    // ══════════════════════════════════════════════════════════════════════════════
    //  QUIZ_SET: structured text → quiz JSON
    // ══════════════════════════════════════════════════════════════════════════════

    private JsonNode convertQuiz(String raw) {
        raw = decodeHtmlEntities(raw);

        // Detect mixed TEXT/MATH format (worked examples with interactive problems)
        if (raw.contains("\nMATH:") || raw.startsWith("MATH:") ||
            raw.contains("\nTEXT:") || raw.startsWith("TEXT:")) {
            return convertMixedQuiz(raw);
        }

        ObjectNode root = mapper.createObjectNode();
        ArrayNode blocks = root.putArray("blocks");

        String[] parts = raw.split("(?m)^Q:\\s*");
        int qNum = 0;

        for (String part : parts) {
            if (part.isBlank()) continue;
            qNum++;

            ObjectNode block = mapper.createObjectNode();
            block.put("id", "q" + qNum);
            block.put("type", "quiz");
            block.put("quizType", "MULTIPLE_CHOICE");

            String[] qLines = part.split("\n");
            block.put("question", qLines[0].strip());

            ArrayNode options = block.putArray("options");
            String answer = "";
            StringBuilder explanationBuilder = new StringBuilder();
            boolean inExplanation = false;
            boolean pastAnswer = false;
            int optIndex = 0;
            java.util.Set<Character> seenLetters = new java.util.HashSet<>();

            for (int i = 1; i < qLines.length; i++) {
                String line = qLines[i].strip();

                if (line.startsWith("ANSWER:")) {
                    answer = line.substring(7).strip();
                    inExplanation = false;
                    pastAnswer = true;
                } else if (line.startsWith("EXPLANATION:")) {
                    explanationBuilder.append(line.substring(12).strip());
                    inExplanation = true;
                } else if (inExplanation && !line.isEmpty()) {
                    // Continue multi-line explanation, preserve line breaks for steps
                    explanationBuilder.append("\n").append(line);
                } else if (!pastAnswer && !inExplanation
                        && line.length() >= 2 && line.charAt(1) == ':'
                        && Character.isLetter(line.charAt(0))
                        && "abcdef".indexOf(Character.toLowerCase(line.charAt(0))) >= 0) {
                    char letterChar = Character.toLowerCase(line.charAt(0));
                    if (seenLetters.contains(letterChar)) {
                        // Duplicate letter → AI is explaining each option, treat as explanation
                        explanationBuilder.append(line).append(" ");
                        inExplanation = true;
                        continue;
                    }
                    seenLetters.add(letterChar);
                    String letter = String.valueOf(letterChar);
                    String text = line.substring(2).strip();
                    String id = "q" + qNum + "_" + letter + "_" + optIndex;
                    ObjectNode opt = mapper.createObjectNode();
                    opt.put("id", id);
                    opt.put("text", text);
                    opt.put("isCorrect", false);
                    options.add(opt);
                    optIndex++;
                }
            }

            // Set correct answer
            if (!answer.isBlank()) {
                String answerLower = answer.toLowerCase();
                for (JsonNode opt : options) {
                    String optId = opt.get("id").asText();
                    String letter = optId.contains("_") ? optId.split("_")[1] : optId;
                    ((ObjectNode) opt).put("isCorrect", letter.equalsIgnoreCase(answerLower));
                }
            }

            block.put("correctAnswer", "");
            block.put("explanation", explanationBuilder.toString().strip());
            block.putArray("hints");
            block.put("points", 30);
            blocks.add(block);
        }

        if (blocks.isEmpty()) {
            return convertRichText(raw);
        }

        return root;
    }

    // ── Mixed TEXT/MATH quiz (worked examples with interactive problems) ──

    private JsonNode convertMixedQuiz(String raw) {
        ObjectNode root = mapper.createObjectNode();
        ArrayNode blocks = root.putArray("blocks");

        // Split on TEXT: or MATH: markers (keeping the marker with the section)
        String[] sections = raw.split("(?m)(?=^TEXT:|^MATH:)");
        int textCount = 0;
        int mathCount = 0;

        for (String section : sections) {
            String trimmed = section.strip();
            if (trimmed.isEmpty()) continue;

            if (trimmed.startsWith("TEXT:")) {
                textCount++;
                String textContent = trimmed.substring(5).strip();
                if (textContent.isEmpty()) continue;

                ObjectNode block = mapper.createObjectNode();
                block.put("id", "t" + textCount);
                block.put("type", "text");
                block.set("content", convertRichText(textContent));
                blocks.add(block);

            } else if (trimmed.startsWith("MATH:")) {
                mathCount++;
                String[] lines = trimmed.split("\n");
                String question = lines[0].substring(5).strip();
                String answer = "";
                StringBuilder explanation = new StringBuilder();
                List<String> hints = new ArrayList<>();
                boolean inExplanation = false;

                for (int i = 1; i < lines.length; i++) {
                    String line = lines[i].strip();
                    if (line.startsWith("ANSWER:")) {
                        answer = line.substring(7).strip();
                        inExplanation = false;
                    } else if (line.startsWith("HINT:")) {
                        hints.add(line.substring(5).strip());
                        inExplanation = false;
                    } else if (line.startsWith("EXPLANATION:")) {
                        explanation.append(line.substring(12).strip());
                        inExplanation = true;
                    } else if (inExplanation && !line.isEmpty()) {
                        explanation.append("\n").append(line);
                    }
                }

                ObjectNode block = mapper.createObjectNode();
                block.put("id", "q" + mathCount);
                block.put("type", "quiz");
                block.put("quizType", "MATH_INPUT");
                block.put("question", question);
                block.put("correctAnswer", answer);
                ArrayNode hintsArr = block.putArray("hints");
                for (String h : hints) hintsArr.add(h);
                block.put("explanation", explanation.toString().strip());
                block.put("points", 30);
                blocks.add(block);

            } else {
                // Content before any marker — treat as introductory text
                if (!trimmed.isEmpty()) {
                    textCount++;
                    ObjectNode block = mapper.createObjectNode();
                    block.put("id", "t" + textCount);
                    block.put("type", "text");
                    block.set("content", convertRichText(trimmed));
                    blocks.add(block);
                }
            }
        }

        if (blocks.isEmpty()) {
            return convertRichText(raw);
        }
        return root;
    }

    // ══════════════════════════════════════════════════════════════════════════════
    //  CODING_SET: structured text → coding JSON
    // ══════════════════════════════════════════════════════════════════════════════

    private JsonNode convertCoding(String raw) {
        raw = decodeHtmlEntities(raw);

        // Try JSON format first (structured schema output from AI)
        JsonNode jsonResult = tryConvertCodingFromJson(raw);
        if (jsonResult != null) return jsonResult;

        // Fallback: text-based marker extraction
        ObjectNode root = mapper.createObjectNode();
        ArrayNode problems = root.putArray("problems");

        String title = extract(raw, "TITLE:");
        String lang = extract(raw, "LANG:");
        String desc = extract(raw, "DESCRIPTION:");
        String starter = extractBlock(raw, "STARTER:");
        String expected = extract(raw, "EXPECTED:");
        String hint = extract(raw, "HINT:");

        if (title.isBlank() && desc.isBlank()) {
            return convertRichText(raw);
        }

        ObjectNode problem = mapper.createObjectNode();
        problem.put("id", "p1");
        problem.put("title", title.isBlank() ? "Problem" : title);

        ObjectNode descDoc = mapper.createObjectNode();
        descDoc.put("type", "doc");
        ArrayNode descContent = descDoc.putArray("content");
        addParagraph(descContent, desc.isBlank() ? "Solve the problem." : desc);
        problem.set("description", descDoc);

        problem.put("language", lang.isBlank() ? "python" : lang.toLowerCase());

        ArrayNode files = problem.putArray("files");
        ObjectNode file = mapper.createObjectNode();
        file.put("id", "f1");
        String cleanedStarter2 = starter.isBlank() ? "# Write your solution here\n" : stripCodeFences(starter).replace("\\n", "\n");
        file.put("name", javaFileName(lang, cleanedStarter2));
        file.put("content", cleanedStarter2);
        files.add(file);

        // Parse test cases from text-based markers
        ArrayNode testCases = parseTestCasesFromText(raw);
        if (testCases != null && !testCases.isEmpty()) {
            problem.set("testCases", testCases);
        }

        problem.put("expectedOutput", expected);
        ArrayNode hints = problem.putArray("hints");
        if (!hint.isBlank()) hints.add(hint);
        problem.put("points", 50);

        problems.add(problem);
        return root;
    }

    /**
     * Try to parse CODING_SET content from structured JSON (Gemini structured schema output).
     * Expected format: {"TITLE":"...", "LANG":"...", "DESCRIPTION":"...", "STARTER":"...",
     *                    "HINT":"...", "TEST_CASE":[{"INPUT":"...", "OUTPUT":"..."},...]}
     */
    private JsonNode tryConvertCodingFromJson(String raw) {
        try {
            JsonNode json = mapper.readTree(raw.strip());
            if (!json.isObject() || !json.has("TITLE")) return null;

            String title = json.path("TITLE").asText("Problem");
            String lang = json.path("LANG").asText("python").toLowerCase();
            String desc = json.path("DESCRIPTION").asText("Solve the problem.");
            String starter = json.path("STARTER").asText("# Write your solution here\n");
            String hint = json.path("HINT").asText("");

            ObjectNode root = mapper.createObjectNode();
            ArrayNode problems = root.putArray("problems");
            ObjectNode problem = mapper.createObjectNode();
            problem.put("id", "p1");
            problem.put("title", title);

            ObjectNode descDoc = mapper.createObjectNode();
            descDoc.put("type", "doc");
            ArrayNode descContent = descDoc.putArray("content");
            // Split description on newlines for multi-paragraph support
            for (String para : desc.split("\n\n?")) {
                if (!para.isBlank()) addParagraph(descContent, para.strip());
            }
            problem.set("description", descDoc);
            problem.put("language", lang);

            ArrayNode files = problem.putArray("files");
            ObjectNode file = mapper.createObjectNode();
            file.put("id", "f1");
            String cleanedStarter3 = stripCodeFences(starter).replace("\\n", "\n");
            file.put("name", javaFileName(lang, cleanedStarter3));
            file.put("content", cleanedStarter3);
            files.add(file);

            // Parse TEST_CASE array
            JsonNode tcNode = json.get("TEST_CASE");
            if (tcNode != null && tcNode.isArray() && !tcNode.isEmpty()) {
                ArrayNode testCases = mapper.createArrayNode();
                for (JsonNode tc : tcNode) {
                    ObjectNode testCase = mapper.createObjectNode();
                    String input = tc.path("INPUT").asText("");
                    String output = tc.path("OUTPUT").asText("");

                    // Fix AI confusion: INPUT contains "OUTPUT: ..." on the same field
                    if (output.isEmpty() && input.contains("OUTPUT:")) {
                        int idx = input.indexOf("OUTPUT:");
                        output = input.substring(idx + 7).strip();
                        input = input.substring(0, idx).strip();
                    }
                    // Also handle INPUT starting with "OUTPUT:" directly
                    if (input.startsWith("OUTPUT:")) {
                        if (output.isEmpty()) output = input.substring(7).strip();
                        input = "";
                    }

                    // AI may use literal "\\n" for newlines — convert to real newlines
                    output = output.replace("\\n", "\n");
                    input = input.replace("\\n", "\n");
                    testCase.put("input", input.stripTrailing());
                    testCase.put("expectedOutput", output.stripTrailing());
                    testCases.add(testCase);
                }
                problem.set("testCases", testCases);
            }

            // Legacy fallback
            problem.put("expectedOutput", json.path("EXPECTED").asText(""));
            ArrayNode hints = problem.putArray("hints");
            if (!hint.isBlank()) hints.add(hint);
            problem.put("points", 50);

            problems.add(problem);
            return root;
        } catch (Exception e) {
            return null; // Not valid JSON, fall through to text parsing
        }
    }

    /**
     * Parse TEST_CASE blocks from text-based AI output.
     */
    private ArrayNode parseTestCasesFromText(String raw) {
        ArrayNode testCases = mapper.createArrayNode();
        Pattern tcPattern = Pattern.compile("(?m)^TEST_CASE:\\s*$");
        String[] parts = tcPattern.split(raw);

        for (String part : parts) {
            if (part.isBlank()) continue;
            String input = extract(part, "INPUT:");
            String output = extract(part, "OUTPUT:");
            if (!output.isBlank()) {
                ObjectNode tc = mapper.createObjectNode();
                tc.put("input", input);
                tc.put("expectedOutput", output);
                testCases.add(tc);
            }
        }
        return testCases;
    }

    // ── Utilities ──

    private String extract(String text, String key) {
        Pattern p = Pattern.compile("(?m)^" + Pattern.quote(key) + "\\s*(.+)$");
        Matcher m = p.matcher(text);
        return m.find() ? m.group(1).strip() : "";
    }

    /**
     * Extract a multi-line block starting after `key` up to the next known marker or end.
     * Used for STARTER: blocks that span multiple lines.
     */
    private String extractBlock(String text, String key) {
        Pattern start = Pattern.compile("(?m)^" + Pattern.quote(key) + "\\s*(.*)$");
        Matcher m = start.matcher(text);
        if (!m.find()) return "";
        int blockStart = m.end();
        String firstLine = m.group(1).strip();
        // Find next marker (uppercase word followed by colon at line start)
        Pattern nextMarker = Pattern.compile("(?m)^[A-Z_]+:");
        Matcher nm = nextMarker.matcher(text);
        int blockEnd = text.length();
        if (nm.find(blockStart)) {
            blockEnd = nm.start();
        }
        String block = text.substring(blockStart, blockEnd).strip();
        if (!firstLine.isEmpty()) {
            block = firstLine + (block.isEmpty() ? "" : "\n" + block);
        }
        return block;
    }

    /**
     * Strip markdown code fences from AI-generated starter code.
     * e.g. "```python\n# code\n```" → "# code"
     */
    private static String stripCodeFences(String code) {
        if (code == null) return "";
        String s = code.strip();
        // Remove opening fence: ```python or ```
        s = s.replaceFirst("^```\\w*\\s*\n?", "");
        // Remove closing fence
        s = s.replaceFirst("\\s*```\\s*$", "");
        s = s.strip();
        // Auto-format if code has no newlines but is clearly multi-statement
        if (!s.contains("\n") && s.length() > 80) {
            s = autoFormatCode(s);
        }
        return s;
    }

    /**
     * Basic auto-formatter for code that was generated as a single line.
     * Inserts newlines at logical breakpoints (after ;, {, }, import statements).
     */
    private static String autoFormatCode(String code) {
        StringBuilder sb = new StringBuilder();
        int indent = 0;
        int i = 0;
        boolean inString = false;
        char stringChar = 0;

        while (i < code.length()) {
            char c = code.charAt(i);

            // Track string state
            if (!inString && (c == '"' || c == '\'')) {
                inString = true;
                stringChar = c;
                sb.append(c);
                i++;
                continue;
            }
            if (inString) {
                sb.append(c);
                if (c == stringChar && (i == 0 || code.charAt(i - 1) != '\\')) {
                    inString = false;
                }
                i++;
                continue;
            }

            if (c == '{') {
                sb.append(" {\n");
                indent++;
                appendIndent(sb, indent);
                i++;
                // skip spaces after {
                while (i < code.length() && code.charAt(i) == ' ') i++;
                continue;
            }
            if (c == '}') {
                // Remove trailing whitespace before }
                String current = sb.toString();
                if (current.endsWith("    ") || current.endsWith("\t")) {
                    sb.setLength(sb.length() - Math.min(4, countTrailingSpaces(current)));
                }
                indent = Math.max(0, indent - 1);
                sb.append("\n");
                appendIndent(sb, indent);
                sb.append("}");
                i++;
                // Check if next non-space is not ; or { or another }
                int next = i;
                while (next < code.length() && code.charAt(next) == ' ') next++;
                if (next < code.length() && code.charAt(next) != ';' && code.charAt(next) != '}' && code.charAt(next) != ')') {
                    sb.append("\n");
                    appendIndent(sb, indent);
                    i = next;
                }
                continue;
            }
            if (c == ';') {
                sb.append(";\n");
                i++;
                // skip spaces after ;
                while (i < code.length() && code.charAt(i) == ' ') i++;
                if (i < code.length() && code.charAt(i) != '}') {
                    appendIndent(sb, indent);
                }
                continue;
            }

            sb.append(c);
            i++;
        }

        return sb.toString().strip();
    }

    private static void appendIndent(StringBuilder sb, int level) {
        sb.append("    ".repeat(level));
    }

    private static int countTrailingSpaces(String s) {
        int count = 0;
        for (int i = s.length() - 1; i >= 0 && s.charAt(i) == ' '; i--) {
            count++;
        }
        return count;
    }

    private String langExtension(String lang) {
        if (lang == null || lang.isBlank()) return "py";
        return switch (lang.toLowerCase().strip()) {
            case "javascript", "js" -> "js";
            case "java" -> "java";
            case "python" -> "py";
            case "c" -> "c";
            case "cpp", "c++" -> "cpp";
            default -> "py";
        };
    }

    /**
     * For Java, extract the public class name from source code and use it as the filename.
     * Other languages use "main.ext".
     */
    private String javaFileName(String lang, String sourceCode) {
        if ("java".equalsIgnoreCase(lang) && sourceCode != null) {
            java.util.regex.Matcher m = java.util.regex.Pattern
                    .compile("public\\s+class\\s+(\\w+)")
                    .matcher(sourceCode);
            if (m.find()) {
                return m.group(1) + ".java";
            }
        }
        return "main." + langExtension(lang);
    }

    private JsonNode emptyDoc() {
        ObjectNode doc = mapper.createObjectNode();
        doc.put("type", "doc");
        ArrayNode content = doc.putArray("content");
        addParagraph(content, "");
        return doc;
    }
}
