package service;

import model.entity.Attempt;
import model.entity.AttemptAnswer;
import model.entity.Question;

import java.util.List;

public interface ExamService {
    /** Begins a timed attempt for the given student against the given quiz. */
    Attempt startAttempt(Long quizId, Long studentId);

    /** Ordered questions for an in-progress attempt. */
    List<Question> questionsFor(Long attemptId);

    /** Records/updates the student's selected option for one question (navigation-friendly, can be called repeatedly). */
    void saveAnswer(Long attemptId, Long questionId, Character selectedOption);

    /** Currently saved answers for an attempt, keyed conceptually by question id. */
    List<AttemptAnswer> answersFor(Long attemptId);

    /** Grades and finalizes the attempt (manual submit). */
    Attempt submit(Long attemptId);

    /** Grades and finalizes the attempt because the timer expired. */
    Attempt autoSubmit(Long attemptId);

    Attempt get(Long attemptId);

    List<Attempt> historyFor(Long studentId);

    List<Attempt> attemptsForQuiz(Long quizId);

    /** How many finished (submitted/auto-submitted) attempts this student has used on this quiz. */
    int attemptsUsed(Long quizId, Long studentId);
}