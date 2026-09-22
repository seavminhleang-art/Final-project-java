package com.proctor.controller;

import com.proctor.controller.LoginScreen.ForgotStep;
import com.proctor.exception.ValidationException;
import com.proctor.model.service.AuthService;
import com.proctor.model.service.EmailVerificationService;
import com.proctor.util.KeyUtil;
import com.proctor.util.MouseUtil;
import com.proctor.util.TuiHelper;
import com.williamcallahan.tui4j.compat.bubbletea.KeyPressMessage;
import com.williamcallahan.tui4j.compat.bubbletea.Message;
import com.williamcallahan.tui4j.compat.bubbletea.input.key.KeyType;

import java.util.List;

public class ForgotPasswordScreen implements Screen {

    private final AuthService authService;
    private final EmailVerificationService verificationService;

    private ForgotStep forgotStep = ForgotStep.IDENTIFIER;
    private String maskedEmail = "";
    private final StringBuilder forgotIdentifier = new StringBuilder();
    private final StringBuilder forgotCode = new StringBuilder();
    private final StringBuilder forgotNewPassword = new StringBuilder();
    private final StringBuilder forgotConfirmPassword = new StringBuilder();
    private int forgotFocusIndex = 0;
    private String forgotMessage = "";

    public ForgotPasswordScreen(AuthService authService) {
        this(authService, new EmailVerificationService());
    }

    public ForgotPasswordScreen(AuthService authService, EmailVerificationService verificationService) {
        this.authService = authService;
        this.verificationService = verificationService != null ? verificationService : new EmailVerificationService();
    }

    public ForgotStep getForgotStep() {
        return forgotStep;
    }

    public String getForgotMessage() {
        return forgotMessage;
    }

    @Override
    public ScreenResult update(Message msg) {
        if (MouseUtil.isWheelUp(msg)) {
            if (forgotStep == ForgotStep.IDENTIFIER || forgotStep == ForgotStep.VERIFY_OTP) {
                forgotFocusIndex = (forgotFocusIndex == 0) ? 1 : 0;
            } else {
                if (forgotFocusIndex == 0) forgotFocusIndex = 2;
                else if (forgotFocusIndex == 1) forgotFocusIndex = 0;
                else forgotFocusIndex = 1;
            }
            return ScreenResult.stay(this);
        }

        if (MouseUtil.isWheelDown(msg)) {
            if (forgotStep == ForgotStep.IDENTIFIER || forgotStep == ForgotStep.VERIFY_OTP) {
                forgotFocusIndex = (forgotFocusIndex == 0) ? 1 : 0;
            } else {
                if (forgotFocusIndex == 0) forgotFocusIndex = 1;
                else if (forgotFocusIndex == 1) forgotFocusIndex = 2;
                else forgotFocusIndex = 0;
            }
            return ScreenResult.stay(this);
        }

        if (MouseUtil.isLeftClick(msg)) {
            int line = MouseUtil.getLineIndex(msg);
            int col = MouseUtil.getColInLine(msg);

            if (forgotStep == ForgotStep.IDENTIFIER) {
                if (line >= 13 && line <= 16) {
                    forgotFocusIndex = 0;
                    return ScreenResult.stay(this);
                } else if (line >= 18 && line <= 20) {
                    int btn = MouseUtil.getClickedButtonIndex(col, 106, "Send Verification Code", "Cancel");
                    if (btn == 0) {
                        return submitIdentifier();
                    } else if (btn == 1) {
                        return ScreenResult.navigate(new LoginScreen(authService, verificationService));
                    }
                }
            } else if (forgotStep == ForgotStep.VERIFY_OTP) {
                if (line >= 14 && line <= 17) {
                    forgotFocusIndex = 0;
                    return ScreenResult.stay(this);
                } else if (line >= 19 && line <= 21) {
                    int btn = MouseUtil.getClickedButtonIndex(col, 106, "Verify Code", "Resend Code", "Cancel");
                    if (btn == 0) {
                        return submitOtp();
                    } else if (btn == 1) {
                        return resendOtp();
                    } else if (btn == 2) {
                        return ScreenResult.navigate(new LoginScreen(authService, verificationService));
                    }
                }
            } else if (forgotStep == ForgotStep.NEW_PASSWORD) {
                if (line >= 13 && line <= 16) {
                    forgotFocusIndex = 0;
                    return ScreenResult.stay(this);
                } else if (line >= 18 && line <= 21) {
                    forgotFocusIndex = 1;
                    return ScreenResult.stay(this);
                } else if (line >= 23 && line <= 25) {
                    int btn = MouseUtil.getClickedButtonIndex(col, 106, "Reset Password", "Cancel");
                    if (btn == 0) {
                        return submitNewPassword();
                    } else if (btn == 1) {
                        return ScreenResult.navigate(new LoginScreen(authService, verificationService));
                    }
                }
            }
            return ScreenResult.stay(this);
        }

        if (msg instanceof KeyPressMessage k) {
            if (KeyUtil.isEsc(k)) {
                return ScreenResult.navigate(new LoginScreen(authService, verificationService));
            }

            if (forgotStep == ForgotStep.IDENTIFIER) {
                if (KeyUtil.isDown(k) || KeyUtil.isUp(k)) {
                    forgotFocusIndex = (forgotFocusIndex == 0) ? 1 : 0;
                    return ScreenResult.stay(this);
                }
                if (forgotFocusIndex >= 1 && (KeyUtil.isLeft(k) || KeyUtil.isRight(k))) {
                    forgotFocusIndex = (forgotFocusIndex == 1) ? 2 : 1;
                    return ScreenResult.stay(this);
                }
                if (KeyUtil.isEnter(k)) {
                    if (forgotFocusIndex == 0 || forgotFocusIndex == 1) {
                        return submitIdentifier();
                    } else {
                        return ScreenResult.navigate(new LoginScreen(authService, verificationService));
                    }
                }
                if (forgotFocusIndex == 0) {
                    if (KeyUtil.handleBackspace(forgotIdentifier, k)) {
                        forgotMessage = "";
                        return ScreenResult.stay(this);
                    }
                    char c = extractChar(k);
                    if (c != '\0' && !Character.isISOControl(c) && forgotIdentifier.length() < 254) {
                        forgotIdentifier.append(c);
                        forgotMessage = "";
                    }
                }
            } else if (forgotStep == ForgotStep.VERIFY_OTP) {
                if (KeyUtil.isDown(k) || KeyUtil.isUp(k)) {
                    forgotFocusIndex = (forgotFocusIndex == 0) ? 1 : 0;
                    return ScreenResult.stay(this);
                }
                if (forgotFocusIndex >= 1) {
                    if (KeyUtil.isLeft(k)) {
                        forgotFocusIndex = (forgotFocusIndex == 1) ? 3 : forgotFocusIndex - 1;
                        return ScreenResult.stay(this);
                    }
                    if (KeyUtil.isRight(k)) {
                        forgotFocusIndex = (forgotFocusIndex == 3) ? 1 : forgotFocusIndex + 1;
                        return ScreenResult.stay(this);
                    }
                }
                if (KeyUtil.isEnter(k)) {
                    if (forgotFocusIndex == 0 || forgotFocusIndex == 1) {
                        return submitOtp();
                    } else if (forgotFocusIndex == 2) {
                        return resendOtp();
                    } else {
                        return ScreenResult.navigate(new LoginScreen(authService, verificationService));
                    }
                }
                if (forgotFocusIndex == 0) {
                    if (KeyUtil.handleBackspace(forgotCode, k)) {
                        forgotMessage = "";
                        return ScreenResult.stay(this);
                    }
                    char c = extractChar(k);
                    if (c != '\0' && !Character.isISOControl(c) && forgotCode.length() < 10) {
                        forgotCode.append(c);
                        forgotMessage = "";
                    }
                }
            } else {
                if (KeyUtil.isDown(k)) {
                    forgotFocusIndex = (forgotFocusIndex + 1) % 3;
                    return ScreenResult.stay(this);
                }
                if (KeyUtil.isUp(k)) {
                    forgotFocusIndex = (forgotFocusIndex - 1 + 3) % 3;
                    return ScreenResult.stay(this);
                }
                if (forgotFocusIndex >= 2 && (KeyUtil.isLeft(k) || KeyUtil.isRight(k))) {
                    forgotFocusIndex = (forgotFocusIndex == 2) ? 3 : 2;
                    return ScreenResult.stay(this);
                }
                if (KeyUtil.isEnter(k)) {
                    if (forgotFocusIndex == 0) {
                        forgotFocusIndex = 1;
                        return ScreenResult.stay(this);
                    } else if (forgotFocusIndex == 1 || forgotFocusIndex == 2) {
                        return submitNewPassword();
                    } else {
                        return ScreenResult.navigate(new LoginScreen(authService, verificationService));
                    }
                }
                if (forgotFocusIndex == 0 || forgotFocusIndex == 1) {
                    StringBuilder active = (forgotFocusIndex == 0) ? forgotNewPassword : forgotConfirmPassword;
                    if (KeyUtil.handleBackspace(active, k)) {
                        forgotMessage = "";
                        return ScreenResult.stay(this);
                    }
                    char c = extractChar(k);
                    if (c != '\0' && !Character.isISOControl(c) && active.length() < 128) {
                        active.append(c);
                        forgotMessage = "";
                    }
                }
            }
        }

        return ScreenResult.stay(this);
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

    private ScreenResult submitIdentifier() {
        String target = forgotIdentifier.toString().trim();
        if (target.isBlank()) {
            forgotMessage = TuiHelper.red("✖ Please enter your email address or username.");
            return ScreenResult.stay(this);
        }
        try {
            maskedEmail = verificationService.sendPasswordResetCode(target);
            forgotStep = ForgotStep.VERIFY_OTP;
            forgotFocusIndex = 0;
            forgotCode.setLength(0);
            forgotNewPassword.setLength(0);
            forgotConfirmPassword.setLength(0);
            forgotMessage = "";
        } catch (ValidationException e) {
            forgotMessage = TuiHelper.red("✖ " + e.getMessage());
        } catch (Exception e) {
            forgotMessage = TuiHelper.red("✖ Failed to send verification code: " + e.getMessage());
        }
        return ScreenResult.stay(this);
    }

    private ScreenResult submitOtp() {
        String code = forgotCode.toString().trim();
        if (code.isBlank()) {
            forgotMessage = TuiHelper.red("✖ Please enter the 6-digit verification code.");
            return ScreenResult.stay(this);
        }
        try {
            verificationService.verifyPasswordResetCode(forgotIdentifier.toString().trim(), code);
            forgotStep = ForgotStep.NEW_PASSWORD;
            forgotFocusIndex = 0;
            forgotNewPassword.setLength(0);
            forgotConfirmPassword.setLength(0);
            forgotMessage = "";
        } catch (ValidationException e) {
            forgotMessage = TuiHelper.red("✖ " + e.getMessage());
        } catch (Exception e) {
            forgotMessage = TuiHelper.red("✖ Verification failed: " + e.getMessage());
        }
        return ScreenResult.stay(this);
    }

    private ScreenResult resendOtp() {
        try {
            maskedEmail = verificationService.sendPasswordResetCode(forgotIdentifier.toString().trim());
            forgotMessage = TuiHelper.green("✔ A fresh verification code was sent to " + maskedEmail);
        } catch (Exception e) {
            forgotMessage = TuiHelper.red("✖ Failed to resend code: " + e.getMessage());
        }
        return ScreenResult.stay(this);
    }

    private ScreenResult submitNewPassword() {
        String code = forgotCode.toString().trim();
        String newPass = forgotNewPassword.toString();
        String confirmPass = forgotConfirmPassword.toString();
        try {
            verificationService.verifyAndResetPassword(forgotIdentifier.toString().trim(), code, newPass, confirmPass);
            return ScreenResult.navigate(new LoginScreen(authService, TuiHelper.green("✔ Password reset successfully. Please log in with your new password.")));
        } catch (ValidationException e) {
            forgotMessage = TuiHelper.red("✖ " + e.getMessage());
        } catch (Exception e) {
            forgotMessage = TuiHelper.red("✖ Password reset failed: " + e.getMessage());
        }
        return ScreenResult.stay(this);
    }

    @Override
    public String view() {
        StringBuilder sb = new StringBuilder();
        sb.append(TuiHelper.header("PROCTOR"));
        sb.append("\n");

        if (forgotStep == ForgotStep.IDENTIFIER) {
            sb.append(TuiHelper.boxTitle("Password Recovery", "Step 1 of 3: Identify Account")).append("\n\n");
            sb.append("  ").append(TuiHelper.dim("Enter your registered email address or username to receive a 6-digit verification code.\n\n"));
            sb.append(TuiHelper.inputBox("Email or Username", forgotIdentifier.toString(), forgotFocusIndex == 0, 102, false, "e.g. user@gmail.com or username"));
            sb.append("\n");

            List<String> buttons = List.of("Send Verification Code", "Cancel");
            int btnIdx = (forgotFocusIndex >= 1) ? forgotFocusIndex - 1 : -1;
            sb.append(TuiHelper.buttonRow(buttons, btnIdx, 106)).append("\n\n");
        } else if (forgotStep == ForgotStep.VERIFY_OTP) {
            sb.append(TuiHelper.boxTitle("Password Recovery", "Step 2 of 3: Enter Verification Code")).append("\n\n");
            sb.append("  ").append(TuiHelper.bold("We sent a 6-digit verification code to: ")).append(TuiHelper.cyan(maskedEmail)).append("\n");
            sb.append("  ").append(TuiHelper.dim("Please check your email inbox and enter the 6-digit code below to proceed.\n\n"));

            sb.append(TuiHelper.inputBox("Verification Code", forgotCode.toString(), forgotFocusIndex == 0, 102, false, "Enter 6-digit code (e.g. 123456)"));
            sb.append("\n");

            List<String> buttons = List.of("Verify Code", "Resend Code", "Cancel");
            int btnIdx = (forgotFocusIndex >= 1) ? forgotFocusIndex - 1 : -1;
            sb.append(TuiHelper.buttonRow(buttons, btnIdx, 106)).append("\n\n");
        } else {
            sb.append(TuiHelper.boxTitle("Password Recovery", "Step 3 of 3: Set New Password")).append("\n\n");
            sb.append("  ").append(TuiHelper.dim("Enter your new account password below to reset your credentials.\n\n"));

            sb.append(TuiHelper.inputBox("New Password", forgotNewPassword.toString(), forgotFocusIndex == 0, 102, true, "Min 8 chars, mix of upper/lower/numbers"));
            sb.append("\n");
            sb.append(TuiHelper.inputBox("Confirm New Password", forgotConfirmPassword.toString(), forgotFocusIndex == 1, 102, true, "Repeat new password"));
            sb.append("\n");

            List<String> buttons = List.of("Reset Password", "Cancel");
            int btnIdx = (forgotFocusIndex >= 2) ? forgotFocusIndex - 2 : -1;
            sb.append(TuiHelper.buttonRow(buttons, btnIdx, 106)).append("\n\n");
        }

        if (!forgotMessage.isBlank()) {
            sb.append("  ").append(forgotMessage).append("\n\n");
        }
        sb.append(TuiHelper.dim("  [↑/↓] Switch Field  •  [←/→] Select Action  •  [Enter] Confirm  •  [Esc] Cancel\n"));
        return sb.toString();
    }
}
