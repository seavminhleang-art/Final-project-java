package com.proctor.controller;

import com.proctor.model.entity.Question;
import com.proctor.model.entity.QuestionOption;
import com.proctor.model.entity.Result;
import com.proctor.model.entity.SpeedQuizAnswerRecord;
import com.proctor.model.entity.SpeedQuizSession;
import com.proctor.model.service.AuthService;
import com.proctor.model.service.ExamService;
import com.proctor.util.KeyUtil;
import com.proctor.view.SpeedQuizViews;
import com.williamcallahan.tui4j.compat.bubbletea.Command;
import com.williamcallahan.tui4j.compat.bubbletea.KeyPressMessage;
import com.williamcallahan.tui4j.compat.bubbletea.Message;

public class SpeedQuizTakerScreen implements Screen {

    public enum State {
        ANSWERING,
        REVEAL,
        DONE
    }

    private final SpeedQuizSession session;
    private final ExamService examService;
    private final AuthService authService;
    private final Screen returnScreen;

    private static final int TOAST_DURATION_SECONDS = 4;

    private State state = State.ANSWERING;
    private int focusedOptionIndex = 0;
    private SpeedQuizAnswerRecord lastAnswerRecord = null;
    private int revealSecondsRemaining = TOAST_DURATION_SECONDS;
    private boolean confirmForfeitMode = false;
    private boolean confirmForfeitFocused = false;
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

    public SpeedQuizTakerScreen(SpeedQuizSession session, ExamService examService, AuthService authService, Screen returnScreen) {
        this.session = session;
        this.examService = examService;
        this.authService = authService;
        this.returnScreen = returnScreen;
    }

    @Override
    public Command init() {
        tickGeneration = 1;
        int gen = tickGeneration;
        return () -> tick(gen);
    }

    @Override
    public ScreenResult update(Message msg) {
        if (isSubmitted) {
            return ScreenResult.stay(this);
        }

        if (session.isCompleted() && state != State.REVEAL) {
            return finishAndSubmit();
        }

        if (msg instanceof TickMessage t) {
            if (t.generation() != -1 && t.generation() != this.tickGeneration) {
                // Ignore stale tick from a previous state or question
                return ScreenResult.stay(this);
            }

            if (state == State.ANSWERING) {
                int remaining = session.getQuestionSecondsRemaining() - 1;
                session.setQuestionSecondsRemaining(remaining);
                if (remaining <= 0) {
                    Question q = session.getCurrentQuestion();
                    lastAnswerRecord = session.recordAnswer(q, null, true);
                    if (session.getAttempt() != null && q != null) {
                        examService.recordAnswer(session.getAttempt().getId(), q.getId(), null, null);
                    }
                    state = State.REVEAL;
                    revealSecondsRemaining = TOAST_DURATION_SECONDS;
                    focusedOptionIndex = 0;
                    tickGeneration++;
                    int nextGen = tickGeneration;
                    return ScreenResult.stay(this, () -> tick(nextGen));
                }
                int curGen = tickGeneration;
                return ScreenResult.stay(this, () -> tick(curGen));
            } else if (state == State.REVEAL) {
                revealSecondsRemaining--;
                if (revealSecondsRemaining <= 0) {
                    if (session.isCompleted() || session.getCurrentQuestion() == null) {
                        return finishAndSubmit();
                    } else {
                        state = State.ANSWERING;
                        lastAnswerRecord = null;
                        focusedOptionIndex = 0;
                        tickGeneration++;
                        int nextGen = tickGeneration;
                        return ScreenResult.stay(this, () -> tick(nextGen));
                    }
                }
                int curGen = tickGeneration;
                return ScreenResult.stay(this, () -> tick(curGen));
            }
        }

        if (msg instanceof KeyPressMessage k) {
            if (confirmForfeitMode) {
                if (KeyUtil.isLeft(k) || KeyUtil.isRight(k)) {
                    confirmForfeitFocused = !confirmForfeitFocused;
                    return ScreenResult.stay(this);
                } else if (KeyUtil.isEnter(k)) {
                    if (confirmForfeitFocused) {
                        return finishAndSubmit();
                    } else {
                        confirmForfeitMode = false;
                        return ScreenResult.stay(this);
                    }
                } else if ("y".equalsIgnoreCase(k.key())) {
                    return finishAndSubmit();
                } else if ("n".equalsIgnoreCase(k.key()) || KeyUtil.isEsc(k)) {
                    confirmForfeitMode = false;
                    return ScreenResult.stay(this);
                }
                return ScreenResult.stay(this);
            }

            if (state == State.REVEAL) {
                if (KeyUtil.isEsc(k)) {
                    confirmForfeitMode = true;
                    confirmForfeitFocused = false;
                }
                return ScreenResult.stay(this);
            }

            if (state == State.ANSWERING) {
                if (KeyUtil.isEsc(k)) {
                    confirmForfeitMode = true;
                    confirmForfeitFocused = false;
                    return ScreenResult.stay(this);
                }

                Question q = session.getCurrentQuestion();
                if (q == null || q.getOptions() == null || q.getOptions().isEmpty()) {
                    return finishAndSubmit();
                }

                int optCount = q.getOptions().size();

                if (KeyUtil.isUp(k)) {
                    focusedOptionIndex = (focusedOptionIndex - 1 + optCount) % optCount;
                    return ScreenResult.stay(this);
                } else if (KeyUtil.isDown(k)) {
                    focusedOptionIndex = (focusedOptionIndex + 1) % optCount;
                    return ScreenResult.stay(this);
                } else if (KeyUtil.isSpace(k) || KeyUtil.isEnter(k)) {
                    if (focusedOptionIndex >= 0 && focusedOptionIndex < optCount) {
                        QuestionOption opt = q.getOptions().get(focusedOptionIndex);
                        return lockInOption(q, opt.getId());
                    }
                    return ScreenResult.stay(this);
                }

                // Quick pick 1-4
                if (k.key() != null && k.key().length() == 1) {
                    char c = k.key().charAt(0);
                    if (c >= '1' && c <= '9') {
                        int idx = c - '1';
                        if (idx >= 0 && idx < optCount) {
                            return lockInOption(q, q.getOptions().get(idx).getId());
                        }
                    }
                    char lower = Character.toLowerCase(c);
                    if (lower >= 'a' && lower <= 'd') {
                        int idx = lower - 'a';
                        if (idx >= 0 && idx < optCount) {
                            return lockInOption(q, q.getOptions().get(idx).getId());
                        }
                    }
                    if (lower == 't') {
                        for (int i = 0; i < optCount; i++) {
                            if (q.getOptions().get(i).getOptionText().equalsIgnoreCase("true")) {
                                return lockInOption(q, q.getOptions().get(i).getId());
                            }
                        }
                    } else if (lower == 'f') {
                        for (int i = 0; i < optCount; i++) {
                            if (q.getOptions().get(i).getOptionText().equalsIgnoreCase("false")) {
                                return lockInOption(q, q.getOptions().get(i).getId());
                            }
                        }
                    }
                }
            }
        }

        return ScreenResult.stay(this);
    }

    private ScreenResult lockInOption(Question q, Integer optionId) {
        lastAnswerRecord = session.recordAnswer(q, optionId, false);
        if (session.getAttempt() != null) {
            examService.recordAnswer(session.getAttempt().getId(), q.getId(), optionId, null);
        }
        state = State.REVEAL;
        revealSecondsRemaining = TOAST_DURATION_SECONDS;
        focusedOptionIndex = 0;
        tickGeneration++;
        int nextGen = tickGeneration;
        return ScreenResult.stay(this, () -> tick(nextGen));
    }

    private ScreenResult finishAndSubmit() {
        if (isSubmitted) {
            return ScreenResult.stay(this);
        }
        isSubmitted = true;
        state = State.DONE;
        Result res = examService.submitSpeedQuiz(session);
        return ScreenResult.navigate(new SpeedQuizResultScreen(res, session, examService, authService, returnScreen));
    }

    @Override
    public String view() {
        SpeedQuizAnswerRecord toast = (state == State.REVEAL) ? lastAnswerRecord : null;
        return SpeedQuizViews.renderSpeedQuizTaker(session, focusedOptionIndex, confirmForfeitMode, confirmForfeitFocused, toast, revealSecondsRemaining);
    }
}
