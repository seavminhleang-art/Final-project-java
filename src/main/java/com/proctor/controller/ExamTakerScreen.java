package com.proctor.controller;

import com.proctor.model.service.AuthService;
import com.proctor.model.enums.QuestionType;
import com.proctor.model.entity.ExamSession;
import com.proctor.model.service.ExamService;
import com.proctor.model.entity.Question;
import com.proctor.model.entity.QuestionOption;
import com.proctor.model.entity.Result;
import com.proctor.util.KeyUtil;
import com.proctor.util.MouseUtil;
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
    private boolean isSubmitted = false;
    private int tickGeneration = 0;

    public record TickMessage(int generation) implements Message {
        public TickMessage() {
            this(-1);
        }
    }

    public static Message tick() {
        return tick(-1);
    }

    public static Message tick(int generation) {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException ignored) {}
        return new TickMessage(generation);
    }

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

    @Override
    public Command init() {
        if (!session.isTimed()) return null;
        tickGeneration = 1;
        int gen = tickGeneration;
        return () -> tick(gen);
    }

    private void saveCurrentAnswer() {
        if (session.getQuestions().isEmpty()) return;
        Question q = session.getQuestions().get(currentQuestionIndex);

        if (q.getQuestionType() == QuestionType.SHORT_ANSWER) {
            session.getTextAnswers().put(q.getId(), shortAnswerBuffer.toString());
            if (session.getAttempt() != null && session.getAttempt().getId() != null) {
                examService.recordAnswer(session.getAttempt().getId(), q.getId(), null, shortAnswerBuffer.toString());
            }
        } else {
            Integer selectedOptId = session.getSelectedOptions().get(q.getId());
            if (selectedOptId != null && session.getAttempt() != null && session.getAttempt().getId() != null) {
                examService.recordAnswer(session.getAttempt().getId(), q.getId(), selectedOptId, null);
            }
        }
    }

    @Override
    public ScreenResult update(Message msg) {
        if (isSubmitted) {
            return ScreenResult.stay(this);
        }

        if (msg instanceof TickMessage t) {
            if (t.generation() != -1 && t.generation() != this.tickGeneration) {
                return ScreenResult.stay(this);
            }
            if (session.isTimed()) {
                session.setRemainingSeconds(session.getRemainingSeconds() - 1);
                if (session.getRemainingSeconds() <= 0) {
                    isSubmitted = true;
                    saveCurrentAnswer();
                    Result result = examService.submitExam(session, true);
                    return ScreenResult.navigate(new ExamResultScreen(result, session, examService, authService));
                }
                tickGeneration++;
                int nextGen = tickGeneration;
                return ScreenResult.stay(this, () -> tick(nextGen));
            }
        }

        if (MouseUtil.isWheelUp(msg)) {
            if (!confirmSubmitMode && currentQuestionIndex > 0) {
                saveCurrentAnswer();
                currentQuestionIndex--;
                loadCurrentQuestionState();
            }
            return ScreenResult.stay(this);
        }

        if (MouseUtil.isWheelDown(msg)) {
            if (!confirmSubmitMode && currentQuestionIndex < session.getQuestions().size() - 1) {
                saveCurrentAnswer();
                currentQuestionIndex++;
                loadCurrentQuestionState();
            }
            return ScreenResult.stay(this);
        }

        if (MouseUtil.isLeftClick(msg)) {
            if (confirmSubmitMode) {
                int line = MouseUtil.getLineIndex(msg);
                int col = MouseUtil.getColInLine(msg);
                int btnLine = MouseUtil.findButtonRowLine(view());
                if (btnLine != -1 && line >= btnLine && line <= btnLine + 2) {
                    int btn = MouseUtil.getClickedButtonIndex(col, "Submit Quiz", "Return to Quiz");
                    if (btn == 0) {
                        isSubmitted = true;
                        saveCurrentAnswer();
                        Result result = examService.submitExam(session, false);
                        return ScreenResult.navigate(new ExamResultScreen(result, session, examService, authService));
                    } else if (btn == 1) {
                        confirmSubmitMode = false;
                        return ScreenResult.stay(this);
                    }
                } else if (btnLine != -1 && (line < btnLine - 4 || line > btnLine + 4)) {
                    confirmSubmitMode = false;
                    return ScreenResult.stay(this);
                }
                return ScreenResult.stay(this);
            }

            if (!session.getQuestions().isEmpty()) {
                Question q = session.getQuestions().get(currentQuestionIndex);
                if (q.getQuestionType() != QuestionType.SHORT_ANSWER && q.getOptions() != null) {
                    int line = MouseUtil.getLineIndex(msg);
                    int optIdx = MouseUtil.findOptionIndex(view(), line);
                    if (optIdx >= 0 && optIdx < q.getOptions().size()) {
                        focusedOptionIndex = optIdx;
                        QuestionOption opt = q.getOptions().get(optIdx);
                        session.getSelectedOptions().put(q.getId(), opt.getId());
                        saveCurrentAnswer();
                        return ScreenResult.stay(this);
                    }
                }
            }
            int line = MouseUtil.getLineIndex(msg);
            int col = MouseUtil.getColInLine(msg);
            String hintAction = MouseUtil.getClickedHintAction(view(), line, col);
            if ("Esc".equals(hintAction)) {
                confirmSubmitMode = true;
                confirmSubmitFocused = false;
                return ScreenResult.stay(this);
            }
            return ScreenResult.stay(this);
        }

        if (msg instanceof KeyPressMessage k) {
            if (confirmSubmitMode) {
                if (KeyUtil.isLeft(k) || KeyUtil.isRight(k)) {
                    confirmSubmitFocused = !confirmSubmitFocused;
                    return ScreenResult.stay(this);
                } else if (KeyUtil.isEnter(k)) {
                    if (confirmSubmitFocused) {
                        isSubmitted = true;
                        saveCurrentAnswer();
                        Result result = examService.submitExam(session, false);
                        return ScreenResult.navigate(new ExamResultScreen(result, session, examService, authService));
                    } else {
                        confirmSubmitMode = false;
                        return ScreenResult.stay(this);
                    }
                } else if (KeyUtil.isEsc(k)) {
                    confirmSubmitMode = false;
                    return ScreenResult.stay(this);
                }
                return ScreenResult.stay(this);
            }

            if (KeyUtil.isEsc(k)) {
                confirmSubmitMode = true;
                confirmSubmitFocused = false;
                return ScreenResult.stay(this);
            }

            Question q = session.getQuestions().get(currentQuestionIndex);
            if (q.getQuestionType() != QuestionType.SHORT_ANSWER) {
                if (KeyUtil.isLeft(k)) {
                    saveCurrentAnswer();
                    if (currentQuestionIndex > 0) {
                        currentQuestionIndex--;
                        loadCurrentQuestionState();
                    }
                    return ScreenResult.stay(this);
                }

                if (KeyUtil.isRight(k)) {
                    saveCurrentAnswer();
                    if (currentQuestionIndex < session.getQuestions().size() - 1) {
                        currentQuestionIndex++;
                        loadCurrentQuestionState();
                    }
                    return ScreenResult.stay(this);
                }

                if (q.getOptions() != null) {
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
                }
            } else {
                if (KeyUtil.handleBackspace(shortAnswerBuffer, k)) {
                    saveCurrentAnswer();
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

                if (KeyUtil.isUp(k)) {
                    saveCurrentAnswer();
                    if (currentQuestionIndex > 0) {
                        currentQuestionIndex--;
                        loadCurrentQuestionState();
                    }
                    return ScreenResult.stay(this);
                }

                if (KeyUtil.appendInput(shortAnswerBuffer, k)) {
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