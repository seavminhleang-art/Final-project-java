package com.proctor.controller;

import com.proctor.model.service.AuthService;
import com.proctor.model.enums.QuestionType;
import com.proctor.model.entity.ExamSession;
import com.proctor.model.service.ExamService;
import com.proctor.model.entity.Question;
import com.proctor.model.entity.QuestionOption;
import com.proctor.model.entity.Result;
import com.proctor.util.KeyUtil;
import com.proctor.view.ExamViews;
import com.williamcallahan.tui4j.compat.bubbletea.Command;
import com.williamcallahan.tui4j.compat.bubbletea.KeyPressMessage;
import com.williamcallahan.tui4j.compat.bubbletea.Message;
import com.williamcallahan.tui4j.compat.bubbletea.input.key.KeyType;

public class ExamTakerScreen implements Screen {
    private final ExamSession session;
    private final ExamService examService;
    private final AuthService authService;

    private int currentQuestionIndex = 0;
    private int focusedOptionIndex = 0;
    private final StringBuilder shortAnswerBuffer = new StringBuilder();
    private boolean confirmSubmitMode = false;
    private boolean confirmSubmitFocused = false;

    public record TickMessage() implements Message {}

    public ExamTakerScreen(ExamSession session, ExamService examService, AuthService authService) {
        this.session = session;
        this.examService = examService;
        this.authService = authService;
        loadCurrentQuestionState();
    }

    private void loadCurrentQuestionState() {
        if (session.getQuestions().isEmpty()) return;
        Question q = session.getQuestions().get(currentQuestionIndex);
        shortAnswerBuffer.setLength(0);

        if (q.getQuestionType() == QuestionType.SHORT_ANSWER) {
            String ans = session.getTextAnswers().get(q.getId());
            if (ans != null) shortAnswerBuffer.append(ans);
        } else {
            Integer selectedOpt = session.getSelectedOptions().get(q.getId());
            if (selectedOpt != null && q.getOptions() != null) {
                for (int i = 0; i < q.getOptions().size(); i++) {
                    if (q.getOptions().get(i).getId().equals(selectedOpt)) {
                        focusedOptionIndex = i;
                        break;
                    }
                }
            } else {
                focusedOptionIndex = 0;
            }
        }
    }

    public static Message tick() {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException ignored) {}
        return new TickMessage();
    }

    @Override
    public Command init() {
        return session.isTimed() ? ExamTakerScreen::tick : null;
    }

    private void saveCurrentAnswer() {
        if (session.getQuestions().isEmpty()) return;
        Question q = session.getQuestions().get(currentQuestionIndex);

        if (q.getQuestionType() == QuestionType.SHORT_ANSWER) {
            session.getTextAnswers().put(q.getId(), shortAnswerBuffer.toString());
            examService.recordAnswer(session.getAttempt().getId(), q.getId(), null, shortAnswerBuffer.toString());
        } else {
            Integer selectedOptId = session.getSelectedOptions().get(q.getId());
            if (selectedOptId != null) {
                examService.recordAnswer(session.getAttempt().getId(), q.getId(), selectedOptId, null);
            }
        }
    }

    @Override
    public ScreenResult update(Message msg) {

        if (msg instanceof TickMessage) {
            if (session.isTimed()) {
                session.setRemainingSeconds(session.getRemainingSeconds() - 1);
                if (session.getRemainingSeconds() <= 0) {
                    saveCurrentAnswer();
                    Result result = examService.submitExam(session, true);
                    return ScreenResult.navigate(new ExamResultScreen(result, session, examService, authService));
                }
                return ScreenResult.stay(this, ExamTakerScreen::tick);
            }
        }

        if (msg instanceof KeyPressMessage k) {
            if (confirmSubmitMode) {
                if (KeyUtil.isLeft(k) || KeyUtil.isRight(k) || KeyUtil.isTab(k) || "shift+tab".equalsIgnoreCase(k.key())) {
                    confirmSubmitFocused = !confirmSubmitFocused;
                    return ScreenResult.stay(this, session.isTimed() ? ExamTakerScreen::tick : null);
                } else if (KeyUtil.isEnter(k)) {
                    if (confirmSubmitFocused) {
                        saveCurrentAnswer();
                        Result result = examService.submitExam(session, false);
                        return ScreenResult.navigate(new ExamResultScreen(result, session, examService, authService));
                    } else {
                        confirmSubmitMode = false;
                        return ScreenResult.stay(this, session.isTimed() ? ExamTakerScreen::tick : null);
                    }
                } else if ("y".equalsIgnoreCase(k.key())) {
                    saveCurrentAnswer();
                    Result result = examService.submitExam(session, false);
                    return ScreenResult.navigate(new ExamResultScreen(result, session, examService, authService));
                } else if ("n".equalsIgnoreCase(k.key()) || KeyUtil.isEsc(k)) {
                    confirmSubmitMode = false;
                    return ScreenResult.stay(this, session.isTimed() ? ExamTakerScreen::tick : null);
                }
                return ScreenResult.stay(this);
            }

            if (KeyUtil.isEsc(k)) {
                confirmSubmitMode = true;
                confirmSubmitFocused = false;
                return ScreenResult.stay(this);
            }

            if (KeyUtil.isLeft(k) || "p".equalsIgnoreCase(k.key())) {
                saveCurrentAnswer();
                if (currentQuestionIndex > 0) {
                    currentQuestionIndex--;
                    loadCurrentQuestionState();
                }
                return ScreenResult.stay(this);
            }

            if (KeyUtil.isRight(k) || "n".equalsIgnoreCase(k.key())) {
                saveCurrentAnswer();
                if (currentQuestionIndex < session.getQuestions().size() - 1) {
                    currentQuestionIndex++;
                    loadCurrentQuestionState();
                }
                return ScreenResult.stay(this);
            }

            Question q = session.getQuestions().get(currentQuestionIndex);
            if (q.getQuestionType() != QuestionType.SHORT_ANSWER && q.getOptions() != null) {
                if (KeyUtil.isUp(k)) {
                    focusedOptionIndex = (focusedOptionIndex - 1 + q.getOptions().size()) % q.getOptions().size();
                    return ScreenResult.stay(this);
                } else if (KeyUtil.isDown(k)) {
                    focusedOptionIndex = (focusedOptionIndex + 1) % q.getOptions().size();
                    return ScreenResult.stay(this);
                } else if (KeyUtil.isSpace(k)) {
                    if (focusedOptionIndex >= 0 && focusedOptionIndex < q.getOptions().size()) {
                        QuestionOption opt = q.getOptions().get(focusedOptionIndex);
                        session.getSelectedOptions().put(q.getId(), opt.getId());
                        saveCurrentAnswer();
                    }
                    return ScreenResult.stay(this);
                } else if (KeyUtil.isEnter(k)) {
                    if (focusedOptionIndex >= 0 && focusedOptionIndex < q.getOptions().size()) {
                        QuestionOption opt = q.getOptions().get(focusedOptionIndex);
                        session.getSelectedOptions().put(q.getId(), opt.getId());
                        saveCurrentAnswer();
                    }
                    if (currentQuestionIndex < session.getQuestions().size() - 1) {
                        currentQuestionIndex++;
                        loadCurrentQuestionState();
                    } else {
                        confirmSubmitMode = true;
                        confirmSubmitFocused = false;
                    }
                    return ScreenResult.stay(this);
                }
            } else if (q.getQuestionType() == QuestionType.SHORT_ANSWER) {
                if (KeyUtil.isBackspace(k)) {
                    if (!shortAnswerBuffer.isEmpty()) {
                        shortAnswerBuffer.deleteCharAt(shortAnswerBuffer.length() - 1);
                        saveCurrentAnswer();
                    }
                    return ScreenResult.stay(this);
                }

                if (KeyUtil.isEnter(k)) {
                    saveCurrentAnswer();
                    if (currentQuestionIndex < session.getQuestions().size() - 1) {
                        currentQuestionIndex++;
                        loadCurrentQuestionState();
                    } else {
                        confirmSubmitMode = true;
                        confirmSubmitFocused = false;
                    }
                    return ScreenResult.stay(this);
                }

                if (k.type() == KeyType.KeyRunes && k.runes() != null) {
                    for (char c : k.runes()) {
                        if (!Character.isISOControl(c)) shortAnswerBuffer.append(c);
                    }
                    saveCurrentAnswer();
                    return ScreenResult.stay(this);
                } else if (k.key() != null && k.key().length() == 1 && !Character.isISOControl(k.key().charAt(0))) {
                    shortAnswerBuffer.append(k.key());
                    saveCurrentAnswer();
                    return ScreenResult.stay(this);
                }
            }
        }

        return ScreenResult.stay(this);
    }

    @Override
    public String view() {
        return ExamViews.renderExamTaker(session, currentQuestionIndex, focusedOptionIndex, shortAnswerBuffer.toString(), confirmSubmitMode, confirmSubmitFocused);
    }
}