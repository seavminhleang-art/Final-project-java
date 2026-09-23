package com.proctor.model.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.proctor.model.entity.AIGradeResult;
import com.proctor.model.entity.AIQuestionDraft;
import com.proctor.model.enums.Difficulty;
import com.proctor.model.enums.QuestionType;
import com.proctor.exception.AIException;
import com.proctor.model.entity.QuestionOption;

import java.util.ArrayList;
import java.util.List;

public class AIService {
    private final OllamaClient ollamaClient;
    private final ObjectMapper objectMapper;

    public AIService() {
        this(new OllamaClient());
    }

    public AIService(OllamaClient ollamaClient) {
        this.ollamaClient = ollamaClient;
        this.objectMapper = new ObjectMapper();
    }

    public List<AIQuestionDraft> generateQuestions(String topic, int count, QuestionType type, Difficulty difficulty) {
        return generateQuestions(topic, count, type, difficulty, 4, null);
    }

    public List<AIQuestionDraft> generateQuestions(String topic, int count, QuestionType type, Difficulty difficulty, int mcqOptionCount) {
        return generateQuestions(topic, count, type, difficulty, mcqOptionCount, null);
    }

    public List<AIQuestionDraft> generateQuestions(String topic, int count, QuestionType type, Difficulty difficulty, int mcqOptionCount, String customInstructions) {
        if (topic == null || topic.isBlank() || count <= 0) {
            return new ArrayList<>();
        }
        List<AIQuestionDraft> accumulated = new ArrayList<>();
        int attempts = 0;
        int maxAttempts = 3;
        AIException lastException = null;
        while (accumulated.size() < count && attempts < maxAttempts) {
            attempts++;
            int needed = count - accumulated.size();
            try {
                String prompt = buildGenerationPrompt(topic, needed, type, difficulty, mcqOptionCount, customInstructions);
                String rawJson = ollamaClient.generateJson(prompt);
                List<AIQuestionDraft> batch = parseGeneratedQuestions(rawJson, type, difficulty, mcqOptionCount);
                if (batch != null && !batch.isEmpty()) {
                    accumulated.addAll(batch);
                }
            } catch (Exception e) {
                lastException = (e instanceof AIException ai) ? ai : new AIException("Failed to generate questions: " + e.getMessage(), e);
            }
        }
        if (accumulated.isEmpty() && lastException != null) {
            throw lastException;
        }
        if (accumulated.size() > count) {
            return new ArrayList<>(accumulated.subList(0, count));
        }
        return accumulated;
    }

    public List<AIQuestionDraft> generateMixedQuestions(String topic, int totalCount, Difficulty difficulty, int mcqOptionCount) {
        return generateMixedQuestions(topic, totalCount, difficulty, mcqOptionCount, null);
    }

    public List<AIQuestionDraft> generateMixedQuestions(String topic, int totalCount, Difficulty difficulty, int mcqOptionCount, String customInstructions) {
        if (totalCount <= 0) {
            return new ArrayList<>();
        }
        int mcqCount;
        int tfCount;
        int saCount;
        if (totalCount == 1) {
            mcqCount = 1;
            tfCount = 0;
            saCount = 0;
        } else if (totalCount == 2) {
            mcqCount = 1;
            tfCount = 1;
            saCount = 0;
        } else {
            mcqCount = totalCount / 2;
            tfCount = (totalCount - mcqCount) / 2;
            saCount = totalCount - mcqCount - tfCount;
        }
        List<AIQuestionDraft> drafts = generateMixedQuestions(topic, mcqCount, tfCount, saCount, difficulty, mcqOptionCount, customInstructions);
        if (drafts.size() > totalCount) {
            return new ArrayList<>(drafts.subList(0, totalCount));
        }
        return drafts;
    }

    public List<AIQuestionDraft> generateMixedQuestions(String topic, int mcqCount, int tfCount, int saCount, Difficulty difficulty, int mcqOptionCount) {
        return generateMixedQuestions(topic, mcqCount, tfCount, saCount, difficulty, mcqOptionCount, null);
    }

    public List<AIQuestionDraft> generateMixedQuestions(String topic, int mcqCount, int tfCount, int saCount, Difficulty difficulty, int mcqOptionCount, String customInstructions) {
        List<AIQuestionDraft> allDrafts = new ArrayList<>();
        if (mcqCount > 0) {
            allDrafts.addAll(generateQuestions(topic, mcqCount, QuestionType.MCQ, difficulty, mcqOptionCount, customInstructions));
        }
        if (tfCount > 0) {
            allDrafts.addAll(generateQuestions(topic, tfCount, QuestionType.TRUE_FALSE, difficulty, mcqOptionCount, customInstructions));
        }
        if (saCount > 0) {
            allDrafts.addAll(generateQuestions(topic, saCount, QuestionType.SHORT_ANSWER, difficulty, mcqOptionCount, customInstructions));
        }
        return allDrafts;
    }

    public AIGradeResult gradeShortAnswer(String questionText, String modelContext, String studentAnswer) {
        String prompt = buildGradingPrompt(questionText, modelContext, studentAnswer);
        String rawJson = ollamaClient.generateJson(prompt);
        return parseGradingResult(rawJson);
    }

    private String buildGenerationPrompt(String topic, int count, QuestionType type, Difficulty difficulty, int mcqOptionCount, String customInstructions) {
        int opts = Math.max(2, Math.min(4, mcqOptionCount));
        String customBlock = "";
        if (customInstructions != null && !customInstructions.isBlank()) {
            customBlock = "TEACHER CUSTOM INSTRUCTIONS & CONSTRAINTS:\n" +
                          "\"" + customInstructions.trim() + "\"\n" +
                          "You MUST strictly follow and incorporate these instructions into the questions and answers.\n\n";
        }

        if (type == QuestionType.TRUE_FALSE) {
            return "You are an expert academic examiner. Generate EXACTLY " + count + " distinct " + difficulty.name() + " TRUE_FALSE questions on the topic: \"" + topic + "\".\n\n" +
                   customBlock +
                   "CRITICAL INSTRUCTIONS FOR TRUE_FALSE:\n" +
                   "1. You MUST generate EXACTLY " + count + " distinct questions in the 'questions' JSON array. Not 1 question, but all " + count + " questions.\n" +
                   "2. Every question MUST be a clear declarative factual statement that is either True or False.\n" +
                   "3. Do NOT ask multiple choice questions, questions starting with 'Which of the following', or questions with options embedded in the text.\n" +
                   "4. The 'options' array MUST contain exactly two options: 'True' and 'False'. Exactly one option must have correct=true.\n" +
                   "5. Keep question text concise (under 300 characters) and explanation concise (under 200 characters).\n" +
                   "Respond strictly in JSON format as an object with a 'questions' array:\n" +
                   "{\n" +
                   "  \"questions\": [\n" +
                   "    {\n" +
                   "      \"questionText\": \"In Java, the main method is the entry point of the application.\",\n" +
                   "      \"points\": 2.0,\n" +
                   "      \"explanation\": \"The main method is the required starting point for JVM execution.\",\n" +
                   "      \"options\": [\n" +
                   "        {\"optionText\": \"True\", \"correct\": true},\n" +
                   "        {\"optionText\": \"False\", \"correct\": false}\n" +
                   "      ]\n" +
                   "    }\n" +
                   "  ]\n" +
                   "}";
        } else if (type == QuestionType.SHORT_ANSWER) {
            return "You are an expert academic examiner. Generate EXACTLY " + count + " distinct " + difficulty.name() + " SHORT_ANSWER questions on the topic: \"" + topic + "\".\n\n" +
                   customBlock +
                   "CRITICAL INSTRUCTIONS FOR SHORT_ANSWER:\n" +
                   "1. You MUST generate EXACTLY " + count + " distinct questions in the 'questions' JSON array. Not 1 question, but all " + count + " questions.\n" +
                   "2. Every question MUST be an open-ended conceptual or analytical question requiring a concise written response.\n" +
                   "3. Do NOT provide multiple choice options or True/False questions.\n" +
                   "4. The 'options' array MUST be empty [].\n" +
                   "5. The 'explanation' field MUST provide a concise model answer rubric under 200 characters.\n" +
                   "6. Keep question text concise (under 300 characters).\n" +
                   "Respond strictly in JSON format as an object with a 'questions' array:\n" +
                   "{\n" +
                   "  \"questions\": [\n" +
                   "    {\n" +
                   "      \"questionText\": \"Explain the difference between method overloading and method overriding in Java.\",\n" +
                   "      \"points\": 5.0,\n" +
                   "      \"explanation\": \"Overloading occurs in the same class with same name and different parameters. Overriding occurs in a subclass with the same signature.\",\n" +
                   "      \"options\": []\n" +
                   "    }\n" +
                   "  ]\n" +
                   "}";
        } else {
            return "You are an expert academic examiner. Generate EXACTLY " + count + " distinct " + difficulty.name() + " Multiple Choice (MCQ) questions on the topic: \"" + topic + "\".\n\n" +
                   customBlock +
                   "CRITICAL INSTRUCTIONS FOR MCQ:\n" +
                   "1. You MUST generate EXACTLY " + count + " distinct questions in the 'questions' JSON array. Not 1 question, but all " + count + " questions.\n" +
                   "2. Every question MUST be a multiple choice question with exactly " + opts + " distinct options.\n" +
                   "3. Options MUST NOT be 'True' or 'False'. Provide realistic plausible distractors.\n" +
                   "4. Exactly one option must have correct=true.\n" +
                   "5. Keep question text concise (under 300 characters), option text under 100 characters, and explanation under 200 characters.\n" +
                   "Respond strictly in JSON format as an object with a 'questions' array:\n" +
                   "{\n" +
                   "  \"questions\": [\n" +
                   "    {\n" +
                   "      \"questionText\": \"Which keyword is used to prevent a class from being subclassed in Java?\",\n" +
                   "      \"points\": 2.0,\n" +
                   "      \"explanation\": \"The final keyword prevents class inheritance.\",\n" +
                   "      \"options\": [\n" +
                   "        {\"optionText\": \"static\", \"correct\": false},\n" +
                   "        {\"optionText\": \"final\", \"correct\": true},\n" +
                   "        {\"optionText\": \"abstract\", \"correct\": false},\n" +
                   "        {\"optionText\": \"sealed\", \"correct\": false}\n" +
                   "      ]\n" +
                   "    }\n" +
                   "  ]\n" +
                   "}";
        }
    }

    private String buildGradingPrompt(String questionText, String modelContext, String studentAnswer) {
        return "You are an automated grading assistant for exams. Evaluate the student's answer accurately.\n" +
               "Question: \"" + questionText + "\"\n" +
               "Model Answer / Context: \"" + (modelContext != null ? modelContext : "General domain knowledge") + "\"\n" +
               "Student's Answer: \"" + studentAnswer + "\"\n" +
               "Assign a score from 0 to 100 based on conceptual accuracy, clarity, and completeness.\n" +
               "Keep evaluation feedback concise and under 300 characters.\n" +
               "Respond strictly in JSON format:\n" +
               "{\n" +
               "  \"score\": 85,\n" +
               "  \"feedback\": \"Constructive evaluation comments.\"\n" +
               "}";
    }

    private String sanitizeJson(String raw) {
        if (raw == null) return "[]";
        String s = raw.trim();
        int codeStart = s.indexOf("```json");
        if (codeStart != -1) {
            s = s.substring(codeStart + 7);
        } else {
            codeStart = s.indexOf("```");
            if (codeStart != -1) {
                s = s.substring(codeStart + 3);
            }
        }
        int codeEnd = s.lastIndexOf("```");
        if (codeEnd != -1) {
            s = s.substring(0, codeEnd);
        }
        s = s.trim();
        int firstBrace = s.indexOf('{');
        int firstBracket = s.indexOf('[');
        int start = -1;
        if (firstBrace != -1 && firstBracket != -1) {
            start = Math.min(firstBrace, firstBracket);
        } else if (firstBrace != -1) {
            start = firstBrace;
        } else {
            start = firstBracket;
        }
        int lastBrace = s.lastIndexOf('}');
        int lastBracket = s.lastIndexOf(']');
        int end = Math.max(lastBrace, lastBracket);
        if (start != -1 && end != -1 && end >= start) {
            s = s.substring(start, end + 1);
        }
        return s.trim();
    }

    private List<AIQuestionDraft> parseGeneratedQuestions(String rawJson, QuestionType type, Difficulty difficulty, int mcqOptionCount) {
        List<AIQuestionDraft> drafts = new ArrayList<>();
        try {
            String cleanJson = sanitizeJson(rawJson);
            JsonNode root = objectMapper.readTree(cleanJson);
            List<JsonNode> questionNodes = new ArrayList<>();

            if (root.isArray()) {
                root.forEach(questionNodes::add);
            } else if (root.isObject()) {
                if (root.has("questions") && root.get("questions").isArray()) {
                    root.get("questions").forEach(questionNodes::add);
                } else if (root.has("items") && root.get("items").isArray()) {
                    root.get("items").forEach(questionNodes::add);
                } else if (root.has("results") && root.get("results").isArray()) {
                    root.get("results").forEach(questionNodes::add);
                } else if (root.has("questionText") || root.has("question") || root.has("text") || root.has("options")) {
                    questionNodes.add(root);
                } else {
                    root.elements().forEachRemaining(node -> {
                        if (node.isObject() && (node.has("questionText") || node.has("question") || node.has("text") || node.has("options"))) {
                            questionNodes.add(node);
                        }
                    });
                }
            }

            for (JsonNode node : questionNodes) {
                String text = node.path("questionText").asText(
                        node.path("question").asText(node.path("text").asText(""))
                ).trim();
                if (text.isBlank()) continue;
                if (text.length() > 500) {
                    text = text.substring(0, 497) + "...";
                }

                double points = Math.max(0.5, Math.min(100.0, node.path("points").asDouble(2.0)));
                String explanation = node.path("explanation").asText(node.path("rubric").asText("")).trim();
                if (explanation.length() > 300) {
                    explanation = explanation.substring(0, 297) + "...";
                }

                List<QuestionOption> options = new ArrayList<>();
                if (type == QuestionType.SHORT_ANSWER) {
                } else if (type == QuestionType.TRUE_FALSE) {
                    Boolean explicitTrueCorrect = null;
                    JsonNode optsNode = node.path("options").isMissingNode() ? node.path("choices") : node.path("options");
                    if (optsNode.isArray()) {
                        for (JsonNode optNode : optsNode) {
                            String optText;
                            boolean correct;
                            if (optNode.isTextual()) {
                                optText = optNode.asText().trim();
                                String ans = node.path("answer").asText(node.path("correctAnswer").asText(node.path("correct_answer").asText("")));
                                correct = optText.equalsIgnoreCase(ans);
                            } else {
                                optText = optNode.path("optionText").asText(
                                        optNode.path("text").asText(optNode.path("choice").asText(""))
                                ).trim();
                                correct = optNode.path("correct").asBoolean(optNode.path("is_correct").asBoolean(false));
                            }
                            if (correct) {
                                if ("false".equalsIgnoreCase(optText) || "f".equalsIgnoreCase(optText) || "no".equalsIgnoreCase(optText)) {
                                    explicitTrueCorrect = false;
                                } else if ("true".equalsIgnoreCase(optText) || "t".equalsIgnoreCase(optText) || "yes".equalsIgnoreCase(optText)) {
                                    explicitTrueCorrect = true;
                                }
                            }
                        }
                    }

                    if (explicitTrueCorrect == null) {
                        String ans = node.path("answer").asText(node.path("correctAnswer").asText(node.path("correct_answer").asText(""))).trim();
                        if (!ans.isBlank()) {
                            if ("false".equalsIgnoreCase(ans) || "f".equalsIgnoreCase(ans) || "no".equalsIgnoreCase(ans)) {
                                explicitTrueCorrect = false;
                            } else if ("true".equalsIgnoreCase(ans) || "t".equalsIgnoreCase(ans) || "yes".equalsIgnoreCase(ans)) {
                                explicitTrueCorrect = true;
                            }
                        }
                    }

                    if (explicitTrueCorrect == null && node.has("is_true")) {
                        explicitTrueCorrect = node.path("is_true").asBoolean(true);
                    }

                    boolean isTrue;
                    if (explicitTrueCorrect != null) {
                        isTrue = explicitTrueCorrect;
                    } else {
                        String explLower = explanation.toLowerCase();
                        if (explLower.contains("is false") || explLower.contains("statement is false") || explLower.contains("false.")) {
                            isTrue = false;
                        } else {
                            isTrue = true;
                        }
                    }

                    options.add(QuestionOption.builder().optionText("True").correct(isTrue).optionOrder(1).build());
                    options.add(QuestionOption.builder().optionText("False").correct(!isTrue).optionOrder(2).build());
                } else {
                    JsonNode optsNode = node.path("options").isMissingNode() ? node.path("choices") : node.path("options");
                    if (optsNode.isArray()) {
                        int order = 1;
                        boolean hasCorrect = false;
                        java.util.Set<String> seen = new java.util.HashSet<>();
                        String topAnswer = node.path("answer").asText(node.path("correctAnswer").asText(node.path("correct_answer").asText(""))).trim();

                        for (JsonNode optNode : optsNode) {
                            String optText;
                            boolean correct;
                            if (optNode.isTextual()) {
                                optText = optNode.asText().trim();
                                correct = !topAnswer.isBlank() && (optText.equalsIgnoreCase(topAnswer) || (topAnswer.length() == 1 && Character.isDigit(topAnswer.charAt(0)) && Integer.parseInt(topAnswer) == order - 1));
                            } else {
                                optText = optNode.path("optionText").asText(
                                        optNode.path("text").asText(optNode.path("choice").asText(""))
                                ).trim();
                                correct = optNode.path("correct").asBoolean(optNode.path("is_correct").asBoolean(false));
                                if (!correct && !topAnswer.isBlank() && optText.equalsIgnoreCase(topAnswer)) {
                                    correct = true;
                                }
                            }
                            if (optText.isBlank()) continue;
                            if (optText.length() > 200) {
                                optText = optText.substring(0, 197) + "...";
                            }
                            if (!seen.add(optText.toLowerCase())) {
                                continue;
                            }
                            if (correct) {
                                if (hasCorrect) {
                                    correct = false;
                                } else {
                                    hasCorrect = true;
                                }
                            }
                            options.add(QuestionOption.builder()
                                    .optionText(optText)
                                    .correct(correct)
                                    .optionOrder(order++)
                                    .build());
                        }
                        if (!hasCorrect && !options.isEmpty()) {
                            options.get(0).setCorrect(true);
                        }
                    }
                    if (options.size() < 2) {
                        continue;
                    }
                }

                drafts.add(AIQuestionDraft.builder()
                        .questionText(text)
                        .questionType(type)
                        .difficulty(difficulty)
                        .points(points)
                        .explanation(explanation)
                        .options(options)
                        .build());
            }
        } catch (Exception e) {
            throw new AIException("Failed to parse questions from AI response: " + e.getMessage(), e);
        }
        return drafts;
    }

    private AIGradeResult parseGradingResult(String rawJson) {
        try {
            String cleanJson = sanitizeJson(rawJson);
            JsonNode root = objectMapper.readTree(cleanJson);
            double rawScore = 75.0;
            if (root.has("score")) {
                rawScore = root.path("score").asDouble(75.0);
            } else if (root.has("grade")) {
                rawScore = root.path("grade").asDouble(75.0);
            } else if (root.has("points")) {
                rawScore = root.path("points").asDouble(75.0);
            } else if (root.has("percentage")) {
                rawScore = root.path("percentage").asDouble(75.0);
            }
            int score;
            if (rawScore > 0.0 && rawScore <= 1.0) {
                score = (int) Math.round(rawScore * 100.0);
            } else {
                score = (int) Math.round(rawScore);
            }
            score = Math.min(100, Math.max(0, score));

            String feedback = root.path("feedback").asText(
                    root.path("comments").asText(root.path("explanation").asText(root.path("evaluation").asText("Answer evaluated by AI.")))
            ).trim();
            if (feedback.length() > 500) {
                feedback = feedback.substring(0, 497) + "...";
            }
            return AIGradeResult.builder().score(score).feedback(feedback).build();
        } catch (Exception e) {
            String fallbackFeedback = rawJson != null ? rawJson.trim() : "";
            if (fallbackFeedback.length() > 200) {
                fallbackFeedback = fallbackFeedback.substring(0, 197) + "...";
            }
            return AIGradeResult.builder().score(75).feedback("AI evaluated answer: " + fallbackFeedback).build();
        }
    }
}