package com.proctor.controller;

import com.proctor.exception.ValidationException;
import com.proctor.model.service.EmailVerificationService;
import com.proctor.util.KeyUtil;
import com.proctor.util.MouseUtil;
import com.proctor.util.TuiHelper;
import com.proctor.view.AuthViews;
import com.williamcallahan.tui4j.compat.bubbletea.Command;
import com.williamcallahan.tui4j.compat.bubbletea.KeyPressMessage;
import com.williamcallahan.tui4j.compat.bubbletea.Message;
import com.williamcallahan.tui4j.compat.bubbletea.input.key.KeyType;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

public class EmailVerificationScreen implements Screen {

    public record ResendCompletedMessage(boolean success, String error) implements Message {}
    public record ResendTickMessage(int generationId) implements Message {}

    private final String email;
    private final String fullName;
    private final String title;
    private final String actionButtonLabel;
    private final EmailVerificationService verificationService;
    private final Supplier<ScreenResult> onVerified;
    private final Screen returnScreen;

    private final StringBuilder codeBuffer = new StringBuilder();
    private int focusedSection = 0;
    private int focusedButton = 0;
    private String errorMessage = "";
    private String infoMessage = "";
    private boolean isResending = false;
    private int resendGenerationId = 0;
    private long resendStartTime = 0;
    private int spinnerTick = 0;
    private AtomicBoolean activeCancellation = null;

    public EmailVerificationScreen(String email,
                                   String fullName,
                                   String title,
                                   String actionButtonLabel,
                                   EmailVerificationService verificationService,
                                   Supplier<ScreenResult> onVerified,
                                   Screen returnScreen) {
        this.email = email;
        this.fullName = fullName;
        this.title = title;
        this.actionButtonLabel = actionButtonLabel;
        this.verificationService = verificationService;
        this.onVerified = onVerified;
        this.returnScreen = returnScreen;
    }

    public boolean isResending() {
        return isResending;
    }

    public int getSpinnerTick() {
        return spinnerTick;
    }

    @Override
    public Command init() {
        return null;
    }

    @Override
    public ScreenResult update(Message msg) {
        if (msg instanceof ResendTickMessage t) {
            if (isResending && t.generationId() == resendGenerationId) {
                spinnerTick++;
                return ScreenResult.stay(this, Command.tick(Duration.ofMillis(80), time -> new ResendTickMessage(resendGenerationId)));
            }
            return ScreenResult.stay(this);
        }

        if (msg instanceof ResendCompletedMessage res) {
            isResending = false;
            if (res.success()) {
                infoMessage = TuiHelper.green("✔ A fresh 6-digit verification code was sent to your email.");
                errorMessage = "";
            } else {
                errorMessage = TuiHelper.red("✖ " + res.error());
            }
            return ScreenResult.stay(this);
        }

        if (isResending) {
            if (msg instanceof KeyPressMessage k && KeyUtil.isEsc(k)) {
                if (activeCancellation != null) {
                    activeCancellation.set(true);
                }
                isResending = false;
                resendGenerationId++;
                errorMessage = TuiHelper.yellow("⚠ Resend cancelled.");
                return ScreenResult.stay(this);
            }
            if (MouseUtil.isLeftClick(msg)) {
                int line = MouseUtil.getLineIndex(msg);
                int btnLine = MouseUtil.findButtonRowLine(view());
                if (btnLine != -1 && line >= btnLine && line <= btnLine + 2) {
                    if (activeCancellation != null) {
                        activeCancellation.set(true);
                    }
                    isResending = false;
                    resendGenerationId++;
                    errorMessage = TuiHelper.yellow("⚠ Resend cancelled.");
                    return ScreenResult.stay(this);
                }
            }
            return ScreenResult.stay(this);
        }

        if (MouseUtil.isWheelUp(msg) || MouseUtil.isWheelDown(msg)) {
            focusedSection = (focusedSection == 0) ? 1 : 0;
            return ScreenResult.stay(this);
        }

        if (MouseUtil.isLeftClick(msg)) {
            if (isResending) {
                return ScreenResult.stay(this);
            }
            int line = MouseUtil.getLineIndex(msg);
            int col = MouseUtil.getColInLine(msg);
            if (line >= 16 && line <= 19) {
                focusedSection = 0;
                return ScreenResult.stay(this);
            } else if (line >= 21 && line <= 23) {
                int btn = MouseUtil.getClickedButtonIndex(col, 106, actionButtonLabel, "Resend Code", "Back");
                if (btn == 0) {
                    focusedSection = 1;
                    focusedButton = 0;
                    return submitVerification();
                } else if (btn == 1) {
                    focusedSection = 1;
                    focusedButton = 1;
                    return triggerResend();
                } else if (btn == 2) {
                    focusedSection = 1;
                    focusedButton = 2;
                    return ScreenResult.navigate(returnScreen);
                }
            }
            return ScreenResult.stay(this);
        }

        if (msg instanceof KeyPressMessage k) {
            if (isResending) {
                return ScreenResult.stay(this);
            }

            if (KeyUtil.isEsc(k)) {
                return ScreenResult.navigate(returnScreen);
            }

            if (KeyUtil.isDown(k)) {
                focusedSection = (focusedSection == 0) ? 1 : 0;
                return ScreenResult.stay(this);
            }

            if (KeyUtil.isUp(k)) {
                focusedSection = (focusedSection == 1) ? 0 : 1;
                return ScreenResult.stay(this);
            }

            if (focusedSection == 1) {
                if (KeyUtil.isLeft(k)) {
                    focusedButton = (focusedButton - 1 + 3) % 3;
                    return ScreenResult.stay(this);
                }
                if (KeyUtil.isRight(k)) {
                    focusedButton = (focusedButton + 1) % 3;
                    return ScreenResult.stay(this);
                }
            }

            if (KeyUtil.isEnter(k)) {
                if (focusedSection == 0) {
                    return submitVerification();
                } else {
                    if (focusedButton == 0) {
                        return submitVerification();
                    } else if (focusedButton == 1) {
                        return triggerResend();
                    } else {
                        return ScreenResult.navigate(returnScreen);
                    }
                }
            }

            if (focusedSection == 0) {
                if (KeyUtil.handleBackspace(codeBuffer, k)) {
                    errorMessage = "";
                    return ScreenResult.stay(this);
                }

                if (codeBuffer.length() < 10) {
                    char c = extractChar(k);
                    if (Character.isDigit(c) || Character.isLetter(c)) {
                        codeBuffer.append(c);
                        errorMessage = "";
                    }
                }
            }
        }

        return ScreenResult.stay(this);
    }

    private ScreenResult submitVerification() {
        String code = codeBuffer.toString().trim();
        if (code.isBlank()) {
            errorMessage = TuiHelper.red("✖ Please enter the 6-digit verification code.");
            focusedSection = 0;
            return ScreenResult.stay(this);
        }

        try {
            verificationService.verifyRegistrationCode(email, code);
            return onVerified.get();
        } catch (ValidationException e) {
            errorMessage = TuiHelper.red("✖ " + e.getMessage());
            return ScreenResult.stay(this);
        } catch (Exception e) {
            errorMessage = TuiHelper.red("✖ Verification failed: " + e.getMessage());
            return ScreenResult.stay(this);
        }
    }

    private ScreenResult triggerResend() {
        isResending = true;
        resendStartTime = System.currentTimeMillis();
        spinnerTick = 0;
        final int genId = ++resendGenerationId;
        final AtomicBoolean cancelled = new AtomicBoolean(false);
        this.activeCancellation = cancelled;
        errorMessage = "";
        infoMessage = "";

        Command sendCmd = () -> {
            try {
                verificationService.sendRegistrationCode(email, fullName);
                if (cancelled.get()) {
                    return new ResendCompletedMessage(false, "Cancelled");
                }
                return new ResendCompletedMessage(true, null);
            } catch (Exception e) {
                if (cancelled.get()) {
                    return new ResendCompletedMessage(false, "Cancelled");
                }
                return new ResendCompletedMessage(false, e.getMessage());
            }
        };

        Command tickCmd = Command.tick(Duration.ofMillis(80), time -> new ResendTickMessage(genId));
        return ScreenResult.stay(this, Command.batch(sendCmd, tickCmd));
    }

    private char extractChar(KeyPressMessage k) {
        if (k.type() == KeyType.KeyRunes && k.runes() != null && k.runes().length > 0) {
            return k.runes()[0];
        }
        if (k.key() != null && k.key().length() == 1) {
            return k.key().charAt(0);
        }
        return '\0';
    }

    @Override
    public String view() {
        if (isResending) {
            int elapsed = (int) ((System.currentTimeMillis() - resendStartTime) / 1000);
            return AuthViews.renderOtpLoading("EMAIL VERIFICATION", "Resending Verification Code", email, spinnerTick, elapsed);
        }

        StringBuilder sb = new StringBuilder();
        sb.append(TuiHelper.header("PROCTOR"));
        sb.append("\n");
        sb.append(TuiHelper.boxTitle(title, "Email Confirmation")).append("\n\n");

        sb.append("  ").append(TuiHelper.bold("A 6-digit verification code was sent to your email address:")).append("\n");
        sb.append("  ").append(TuiHelper.cyan(EmailVerificationService.maskEmail(email))).append("\n\n");
        sb.append("  ").append(TuiHelper.dim("Please check your email inbox and enter the code below to proceed.")).append("\n\n");

        boolean isCodeFocused = (focusedSection == 0);
        sb.append(TuiHelper.inputBox("Verification Code", codeBuffer.toString(), isCodeFocused, 102, false, "Enter 6-digit code (e.g. 123456)"));
        sb.append("\n");

        List<String> buttons = List.of(actionButtonLabel, "Resend Code", "Back");
        int focusedBtnIdx = (focusedSection == 1) ? focusedButton : -1;
        sb.append(TuiHelper.buttonRow(buttons, focusedBtnIdx, 106)).append("\n\n");

        if (!errorMessage.isBlank()) {
            sb.append("  ").append(errorMessage).append("\n\n");
        } else if (!infoMessage.isBlank()) {
            sb.append("  ").append(infoMessage).append("\n\n");
        }

        sb.append(TuiHelper.dim("  [↑/↓] Switch Section  •  [←/→] Select Action  •  [Enter] Confirm  •  [Esc] Back\n"));
        return sb.toString();
    }
}
