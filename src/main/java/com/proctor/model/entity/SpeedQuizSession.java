package com.proctor.model.entity;

import com.proctor.model.enums.Difficulty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpeedQuizSession {
    private Attempt attempt;
    private Quiz quiz;

    @Builder.Default
    private List<Question> allQuestions = new ArrayList<>();

    @Builder.Default
    private Set<Integer> answeredQuestionIds = new HashSet<>();

    @Builder.Default
    private Difficulty currentTier = Difficulty.EASY;

    @Builder.Default
    private int currentStreak = 0;

    @Builder.Default
    private int maxStreak = 0;

    @Builder.Default
    private double totalScore = 0.0;

    private int secondsPerQuestion;
    private int questionSecondsRemaining;

    @Builder.Default
    private List<SpeedQuizAnswerRecord> answerRecords = new ArrayList<>();

    private Question currentQuestion;
    private boolean lastAnswerWasUpward;

    public void initSession(Quiz quiz, Attempt attempt, List<Question> questions) {
        this.quiz = quiz;
        this.attempt = attempt;
        this.allQuestions = questions != null ? new ArrayList<>(questions) : new ArrayList<>();
        this.answeredQuestionIds = new HashSet<>();
        this.currentTier = Difficulty.EASY;
        this.currentStreak = 0;
        this.maxStreak = 0;
        this.totalScore = 0.0;
        this.secondsPerQuestion = (quiz != null && quiz.getSpeedSecondsPerQuestion() != null && quiz.getSpeedSecondsPerQuestion() > 0)
                ? quiz.getSpeedSecondsPerQuestion() : 15;
        this.questionSecondsRemaining = this.secondsPerQuestion;
        this.answerRecords = new ArrayList<>();
        this.lastAnswerWasUpward = true;
        this.currentQuestion = pickNextQuestion();
    }

    public boolean isCompleted() {
        return allQuestions.isEmpty() || answeredQuestionIds.size() >= allQuestions.size();
    }

    public int getAnsweredCount() {
        return answeredQuestionIds.size();
    }

    public int getTotalQuestionsCount() {
        return allQuestions.size();
    }

    public Question pickNextQuestion() {
        if (isCompleted()) {
            return null;
        }

        // Try current target tier first
        Question chosen = findUnusedInTier(currentTier);
        if (chosen != null) {
            return chosen;
        }

        // Tier exhaustion fallback
        if (currentTier == Difficulty.HARD) {
            chosen = findUnusedInTier(Difficulty.MEDIUM);
            if (chosen == null) {
                chosen = findUnusedInTier(Difficulty.EASY);
            }
        } else if (currentTier == Difficulty.MEDIUM) {
            if (lastAnswerWasUpward) {
                chosen = findUnusedInTier(Difficulty.HARD);
                if (chosen == null) {
                    chosen = findUnusedInTier(Difficulty.EASY);
                }
            } else {
                chosen = findUnusedInTier(Difficulty.EASY);
                if (chosen == null) {
                    chosen = findUnusedInTier(Difficulty.HARD);
                }
            }
        } else { // EASY
            chosen = findUnusedInTier(Difficulty.MEDIUM);
            if (chosen == null) {
                chosen = findUnusedInTier(Difficulty.HARD);
            }
        }

        if (chosen != null) {
            return chosen;
        }

        // If not found in primary fallback paths, find ANY remaining question
        for (Question q : allQuestions) {
            if (!answeredQuestionIds.contains(q.getId())) {
                return q;
            }
        }

        return null;
    }

    private Question findUnusedInTier(Difficulty tier) {
        for (Question q : allQuestions) {
            if (q.getDifficulty() == tier && !answeredQuestionIds.contains(q.getId())) {
                return q;
            }
        }
        return null;
    }

    public SpeedQuizAnswerRecord recordAnswer(Question q, Integer selectedOptionId, boolean isTimeout) {
        if (q == null) return null;

        boolean isCorrect = false;
        if (!isTimeout && selectedOptionId != null && q.getOptions() != null) {
            for (QuestionOption opt : q.getOptions()) {
                if (opt.getId().equals(selectedOptionId) && opt.isCorrect()) {
                    isCorrect = true;
                    break;
                }
            }
        }

        double multiplier = switch (q.getDifficulty() != null ? q.getDifficulty() : Difficulty.MEDIUM) {
            case EASY -> 1.0;
            case MEDIUM -> 1.5;
            case HARD -> 2.0;
        };

        double basePoints = 0.0;
        double speedBonus = 0.0;
        double streakBonus = 0.0;
        double awarded = 0.0;

        if (isCorrect) {
            currentStreak++;
            if (currentStreak > maxStreak) {
                maxStreak = currentStreak;
            }
            basePoints = q.getPoints() * multiplier;
            if (secondsPerQuestion > 0) {
                speedBonus = Math.floor(((double) questionSecondsRemaining / (double) secondsPerQuestion) * 50.0);
            }
            if (currentStreak >= 3 && currentStreak % 3 == 0) {
                streakBonus = 25.0;
            }
            awarded = basePoints + speedBonus + streakBonus;
            totalScore += awarded;

            lastAnswerWasUpward = true;
            if (currentTier == Difficulty.EASY) {
                currentTier = Difficulty.MEDIUM;
            } else if (currentTier == Difficulty.MEDIUM) {
                currentTier = Difficulty.HARD;
            }
        } else {
            currentStreak = 0;
            lastAnswerWasUpward = false;
            if (currentTier == Difficulty.HARD) {
                currentTier = Difficulty.MEDIUM;
            } else if (currentTier == Difficulty.MEDIUM) {
                currentTier = Difficulty.EASY;
            }
        }

        SpeedQuizAnswerRecord record = SpeedQuizAnswerRecord.builder()
                .question(q)
                .tierShown(q.getDifficulty() != null ? q.getDifficulty() : Difficulty.MEDIUM)
                .selectedOptionId(selectedOptionId)
                .correct(isCorrect)
                .secondsRemaining(questionSecondsRemaining)
                .basePoints(basePoints)
                .speedBonus(speedBonus)
                .streakBonus(streakBonus)
                .totalPointsAwarded(awarded)
                .build();

        answerRecords.add(record);
        answeredQuestionIds.add(q.getId());

        // Prepare for next question
        questionSecondsRemaining = secondsPerQuestion;
        currentQuestion = pickNextQuestion();

        return record;
    }
}
