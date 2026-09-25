package com.proctor.controller;

import com.proctor.model.entity.User;
import com.proctor.model.service.AuthService;
import com.proctor.model.enums.Role;
import com.proctor.exception.AuthException;
import com.proctor.model.service.EmailVerificationService;
import com.proctor.util.KeyUtil;
import com.proctor.util.MouseUtil;
import com.proctor.view.AuthViews;
import com.williamcallahan.tui4j.compat.bubbletea.KeyPressMessage;
import com.williamcallahan.tui4j.compat.bubbletea.Message;
import com.williamcallahan.tui4j.compat.bubbletea.input.key.KeyType;

public class LoginScreen implements Screen {

    public enum ForgotStep { IDENTIFIER, VERIFY_OTP, NEW_PASSWORD }

    private final AuthService authService;
    private final EmailVerificationService verificationService;
    private final StringBuilder identifier = new StringBuilder();
    private final StringBuilder password = new StringBuilder();
    private int focusedField = 0;
    private String errorMessage = "";
    private String infoBanner = "";

    private ForgotPasswordScreen forgotScreen = null;

    public LoginScreen(AuthService authService) {
        this(authService, new EmailVerificationService());
    }

    public LoginScreen(AuthService authService, EmailVerificationService verificationService) {
        this.authService = authService;
        this.verificationService = verificationService != null ? verificationService : new EmailVerificationService();
    }

    public LoginScreen(AuthService authService, String infoBanner) {
        this(authService, new EmailVerificationService());
        this.infoBanner = infoBanner != null ? infoBanner : "";
    }

    public boolean isForgotPasswordMode() {
        return forgotScreen != null;
    }

    public ForgotStep getForgotStep() {
        return forgotScreen != null ? forgotScreen.getForgotStep() : ForgotStep.IDENTIFIER;
    }

    public String getForgotMessage() {
        return forgotScreen != null ? forgotScreen.getForgotMessage() : "";
    }

    public String getInfoBanner() {
        return infoBanner;
    }

    private int getFieldCount() {
        return 5;
    }

    @Override
    public ScreenResult update(Message msg) {
        if (forgotScreen != null) {
            ScreenResult res = forgotScreen.update(msg);
            if (res.nextScreen() != forgotScreen) {
                if (res.nextScreen() instanceof LoginScreen nextLogin) {
                    this.infoBanner = nextLogin.getInfoBanner();
                }
                forgotScreen = null;
                return ScreenResult.stay(this, res.command());
            }
            return ScreenResult.stay(this, res.command());
        }

        if (MouseUtil.isWheelUp(msg)) {
            focusedField = (focusedField - 1 + getFieldCount()) % getFieldCount();
            return ScreenResult.stay(this);
        }

        if (MouseUtil.isWheelDown(msg)) {
            focusedField = (focusedField + 1) % getFieldCount();
            return ScreenResult.stay(this);
        }

        if (MouseUtil.isLeftClick(msg)) {
            int line = MouseUtil.getLineIndex(msg);
            int col = MouseUtil.getColInLine(msg);

            if (line >= 11 && line <= 14) {
                focusedField = 0;
                return ScreenResult.stay(this);
            } else if (line >= 16 && line <= 19) {
                focusedField = 1;
                return ScreenResult.stay(this);
            }
            int btnLine = MouseUtil.findButtonRowLine(view());
            if (btnLine != -1 && line >= btnLine && line <= btnLine + 2) {
                int btn = MouseUtil.getClickedButtonIndex(col, "Log In", "Forgot Password", "Back");
                if (btn == 0) {
                    focusedField = 2;
                    return attemptLogin();
                } else if (btn == 1) {
                    focusedField = 3;
                    forgotScreen = new ForgotPasswordScreen(authService, verificationService);
                    return ScreenResult.stay(this);
                } else if (btn == 2) {
                    focusedField = 4;
                    return ScreenResult.navigate(new StartupScreen(authService));
                }
            }
            return ScreenResult.stay(this);
        }

        if (msg instanceof KeyPressMessage k) {
            if (KeyUtil.isEsc(k)) {
                return ScreenResult.navigate(new StartupScreen(authService));
            }

            if (KeyUtil.isDown(k)) {
                focusedField = (focusedField + 1) % getFieldCount();
                return ScreenResult.stay(this);
            }

            if (KeyUtil.isUp(k)) {
                focusedField = (focusedField - 1 + getFieldCount()) % getFieldCount();
                return ScreenResult.stay(this);
            }

            if (KeyUtil.isEnter(k)) {
                if (focusedField == 0) {
                    focusedField = 1;
                    return ScreenResult.stay(this);
                } else if (focusedField == 1 || focusedField == 2) {
                    return attemptLogin();
                } else if (focusedField == 3) {
                    forgotScreen = new ForgotPasswordScreen(authService, verificationService);
                    return ScreenResult.stay(this);
                } else if (focusedField == 4) {
                    return ScreenResult.navigate(new StartupScreen(authService));
                }
            }

            if (focusedField >= 2 && focusedField <= 4) {
                if (KeyUtil.isLeft(k)) {
                    focusedField = (focusedField == 2) ? 4 : focusedField - 1;
                    return ScreenResult.stay(this);
                } else if (KeyUtil.isRight(k)) {
                    focusedField = (focusedField == 4) ? 2 : focusedField + 1;
                    return ScreenResult.stay(this);
                }
            }

            if (focusedField == 0 || focusedField == 1) {
                StringBuilder active = (focusedField == 0) ? identifier : password;
                if (KeyUtil.isBackspace(k)) {
                    if (!active.isEmpty()) {
                        active.deleteCharAt(active.length() - 1);
                    }
                    return ScreenResult.stay(this);
                }

                int maxLen = (focusedField == 0) ? 254 : 128;
                if (k.type() == KeyType.KeyRunes && k.runes() != null) {
                    for (char c : k.runes()) {
                        if (!Character.isISOControl(c) && active.length() < maxLen) {
                            active.append(c);
                        }
                    }
                    errorMessage = "";
                } else if (k.key() != null && k.key().length() == 1 && !Character.isISOControl(k.key().charAt(0))) {
                    if (active.length() < maxLen) {
                        active.append(k.key());
                    }
                    errorMessage = "";
                }
            }
        }

        return ScreenResult.stay(this);
    }

    private ScreenResult attemptLogin() {
        try {
            User user = authService.login(identifier.toString(), password.toString());
            errorMessage = "";
            if (user.getRole() == Role.ADMIN) {
                return ScreenResult.navigate(new AdminDashboardScreen(authService));
            } else if (user.getRole() == Role.TEACHER) {
                return ScreenResult.navigate(new TeacherDashboardScreen(authService));
            } else {
                return ScreenResult.navigate(new StudentDashboardScreen(authService));
            }
        } catch (AuthException e) {
            errorMessage = e.getMessage();
            password.setLength(0);
            return ScreenResult.stay(this);
        } catch (Exception e) {
            errorMessage = "Login failed: " + e.getMessage();
            password.setLength(0);
            return ScreenResult.stay(this);
        }
    }

    @Override
    public String view() {
        if (forgotScreen != null) {
            return forgotScreen.view();
        }
        return AuthViews.renderLogin(identifier.toString(), password.toString(), focusedField, errorMessage, infoBanner);
    }
}