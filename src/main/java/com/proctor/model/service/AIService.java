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
        return generateQuestions(topic, count, type, difficulty, 4);
    }

    public List<AIQuestionDraft> generateQuestions(String topic, int count, QuestionType type, Difficulty difficulty, int mcqOptionCount) {
        List<AIQuestionDraft> accumulated = new ArrayList<>();
        int attempts = 0;
        int maxAttempts = 3;
        while (accumulated.size() < count && attempts < maxAttempts) {
            attempts++;
            int needed = count - accumulated.size();
            String prompt = buildGenerationPrompt(topic, needed, type, difficulty, mcqOptionCount);
            String rawJson = ollamaClient.generateJson(prompt);
            List<AIQuestionDraft> batch = parseGeneratedQuestions(rawJson, type, difficulty, mcqOptionCount);
            if (batch.isEmpty()) {
                break;
            }
            accumulated.addAll(batch);
        }
        if (accumulated.size() > count) {
            return new ArrayList<>(accumulated.subList(0, count));
        }
        return accumulated;
    }

    public List<AIQuestionDraft> generateMixedQuestions(String topic, int totalCount, Difficulty difficulty, int mcqOptionCount) {
        int mcqCount = Math.max(1, totalCount / 2);
        int tfCount = Math.max(1, (totalCount - mcqCount) / 2);
        int saCount = Math.max(1, totalCount - mcqCount - tfCount);
        return generateMixedQuestions(topic, mcqCount, tfCount, saCount, difficulty, mcqOptionCount);
    }

    public List<AIQuestionDraft> generateMixedQuestions(String topic, int mcqCount, int tfCount, int saCount, Difficulty difficulty, int mcqOptionCount) {
        List<AIQuestionDraft> allDrafts = new ArrayList<>();
        if (mcqCount > 0) {
            allDrafts.addAll(generateQuestions(topic, mcqCount, QuestionType.MCQ, difficulty, mcqOptionCount));
        }
        if (tfCount > 0) {
            allDrafts.addAll(generateQuestions(topic, tfCount, QuestionType.TRUE_FALSE, difficulty, mcqOptionCount));
        }
        if (saCount > 0) {
            allDrafts.addAll(generateQuestions(topic, saCount, QuestionType.SHORT_ANSWER, difficulty, mcqOptionCount));
        }
        return allDrafts;
    }

    public AIGradeResult gradeShortAnswer(String questionText, String modelContext, String studentAnswer) {
        String prompt = buildGradingPrompt(questionText, modelContext, studentAnswer);
        String rawJson = ollamaClient.generateJson(prompt);
        return parseGradingResult(rawJson);
    }

    private String buildGenerationPrompt(String topic, int count, QuestionType type, Difficulty difficulty, int mcqOptionCount) {
        int opts = Math.max(2, Math.min(4, mcqOptionCount));
        if (type == QuestionType.TRUE_FALSE) {
            return "You are an expert academic examiner. Generate EXACTLY " + count + " distinct " + difficulty.name() + " TRUE_FALSE questions on the topic: \"" + topic + "\".\n" +
                   "CRITICAL INSTRUCTIONS FOR TRUE_FALSE:\n" +
                   "1. You MUST generate EXACTLY " + count + " distinct questions in the 'questions' JSON array. Not 1 question, but all " + count + " questions.\n" +
                   "2. Every question MUST be a clear declarative factual statement that is either True or False.\n" +
                   "3. Do NOT ask multiple choice questions, questions starting with 'Which of the following', or questions with options embedded in the text.\n" +
                   "4. The 'options' array MUST contain exactly two options: 'True' and 'False'. Exactly one option must have correct=true.\n" +
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
            return "You are an expert academic examiner. Generate EXACTLY " + count + " distinct " + difficulty.name() + " SHORT_ANSWER questions on the topic: \"" + topic + "\".\n" +
                   "CRITICAL INSTRUCTIONS FOR SHORT_ANSWER:\n" +
                   "1. You MUST generate EXACTLY " + count + " distinct questions in the 'questions' JSON array. Not 1 question, but all " + count + " questions.\n" +
                   "2. Every question MUST be an open-ended conceptual or analytical question requiring a concise written response.\n" +
                   "3. Do NOT provide multiple choice options or True/False questions.\n" +
                   "4. The 'options' array MUST be empty [].\n" +
                   "5. The 'explanation' field MUST provide a complete model answer/rubric.\n" +
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
            return "You are an expert academic examiner. Generate EXACTLY " + count + " distinct " + difficulty.name() + " Multiple Choice (MCQ) questions on the topic: \"" + topic + "\".\n" +
                   "CRITICAL INSTRUCTIONS FOR MCQ:\n" +
                   "1. You MUST generate EXACTLY " + count + " distinct questions in the 'questions' JSON array. Not 1 question, but all " + count + " questions.\n" +
                   "2. Every question MUST be a multiple choice question with exactly " + opts + " distinct options.\n" +
                   "3. Options MUST NOT be 'True' or 'False'. Provide realistic plausible distractors.\n" +
                   "4. Exactly one option must have correct=true.\n" +
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
               "Respond strictly in JSON format:\n" +
               "{\n" +
               "  \"score\": 85,\n" +
               "  \"feedback\": \"Constructive evaluation comments.\"\n" +
               "}";
    }

    private String sanitizeJson(String raw) {
        if (raw == null) return "[]";
        String s = raw.trim();
        if (s.startsWith("```json")) {
            s = s.substring(7);
        } else if (s.startsWith("```")) {
            s = s.substring(3);
        }
        if (s.endsWith("```")) {
            s = s.substring(0, s.length() - 3);
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
                );
                if (text.isBlank()) continue;

                double points = node.path("points").asDouble(2.0);
                String explanation = node.path("explanation").asText(node.path("rubric").asText(""));

                List<QuestionOption> options = new ArrayList<>();
                if (type == QuestionType.SHORT_ANSWER) {
                } else if (type == QuestionType.TRUE_FALSE) {
                    boolean isTrue = true;
                    JsonNode optsNode = node.path("options").isMissingNode() ? node.path("choices") : node.path("options");
                    if (optsNode.isArray()) {
                        for (JsonNode optNode : optsNode) {
                            String optText = optNode.path("optionText").asText(
                                    optNode.path("text").asText(optNode.path("choice").asText(""))
                            ).trim();
                            boolean correct = optNode.path("correct").asBoolean(optNode.path("is_correct").asBoolean(false));
                            if (correct) {
                                if ("false".equalsIgnoreCase(optText) || "f".equalsIgnoreCase(optText) || "no".equalsIgnoreCase(optText)) {
                                    isTrue = false;
                                } else {
                                    isTrue = true;
                                }
                            }
                        }
                    }
                    if (explanation.toLowerCase().contains("is false") || explanation.toLowerCase().contains("incorrect") || explanation.toLowerCase().contains("statement is false")) {
                        isTrue = false;
                    }
                    options.add(QuestionOption.builder().optionText("True").correct(isTrue).optionOrder(1).build());
                    options.add(QuestionOption.builder().optionText("False").correct(!isTrue).optionOrder(2).build());
                } else {
                    JsonNode optsNode = node.path("options").isMissingNode() ? node.path("choices") : node.path("options");
                    if (optsNode.isArray()) {
                        int order = 1;
                        boolean hasCorrect = false;
                        for (JsonNode optNode : optsNode) {
                            String optText = optNode.path("optionText").asText(
                                    optNode.path("text").asText(optNode.path("choice").asText(""))
                            ).trim();
                            if (optText.isBlank()) continue;
                            boolean correct = optNode.path("correct").asBoolean(optNode.path("is_correct").asBoolean(false));
                            if (correct) hasCorrect = true;
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
            int score = root.path("score").asInt(75);
            String feedback = root.path("feedback").asText("Answer evaluated by AI.");
            return AIGradeResult.builder().score(Math.min(100, Math.max(0, score))).feedback(feedback).build();
        } catch (Exception e) {
            return AIGradeResult.builder().score(75).feedback("AI evaluated answer: " + rawJson).build();
        }
    }
}