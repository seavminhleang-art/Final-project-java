package com.proctor.controller;

import com.proctor.model.service.AuthService;
import com.proctor.util.KeyUtil;
import com.proctor.util.TuiHelper;
import com.proctor.view.AuthViews;
import com.williamcallahan.tui4j.compat.bubbletea.KeyPressMessage;
import com.williamcallahan.tui4j.compat.bubbletea.Message;

public class StartupScreen implements Screen {
    private final AuthService authService;
    private int focusedButton = 0; // 0 = Log In, 1 = Register, 2 = Exit
    private boolean showQuitModal = false;
    private boolean quitConfirmFocused = false;

    public StartupScreen(AuthService authService) {
        this.authService = authService;
    }

    @Override
    public ScreenResult update(Message msg) {
        if (msg instanceof KeyPressMessage k) {
            if (showQuitModal) {
                if (KeyUtil.isLeft(k) || KeyUtil.isRight(k)) {
                    quitConfirmFocused = !quitConfirmFocused;
                    return ScreenResult.stay(this);
                }
                if (KeyUtil.isEnter(k)) {
                    if (quitConfirmFocused) {
                        return ScreenResult.quit();
                    } else {
                        showQuitModal = false;
                        return ScreenResult.stay(this);
                    }
                }
                if (KeyUtil.isEsc(k)) {
                    showQuitModal = false;
                    return ScreenResult.stay(this);
                }
                return ScreenResult.stay(this);
            }

            if (KeyUtil.isEsc(k)) {
                showQuitModal = true;
                quitConfirmFocused = false;
                return ScreenResult.stay(this);
            }

            if (KeyUtil.isLeft(k)) {
                if (focusedButton == 1) {
                    focusedButton = 0;
                } else if (focusedButton == 0) {
                    focusedButton = 1;
                }
                return ScreenResult.stay(this);
            }

            if (KeyUtil.isRight(k)) {
                if (focusedButton == 0) {
                    focusedButton = 1;
                } else if (focusedButton == 1) {
                    focusedButton = 0;
                }
                return ScreenResult.stay(this);
            }

            if (KeyUtil.isDown(k)) {
                if (focusedButton == 0 || focusedButton == 1) {
                    focusedButton = 2;
                } else {
                    focusedButton = 0;
                }
                return ScreenResult.stay(this);
            }

            if (KeyUtil.isUp(k)) {
                if (focusedButton == 2) {
                    focusedButton = 0;
                } else {
                    focusedButton = 2;
                }
                return ScreenResult.stay(this);
            }

            if (KeyUtil.isEnter(k)) {
                if (focusedButton == 0) {
                    return ScreenResult.navigate(new LoginScreen(authService));
                } else if (focusedButton == 1) {
                    return ScreenResult.navigate(new RegisterRoleScreen(authService));
                } else if (focusedButton == 2) {
                    showQuitModal = true;
                    quitConfirmFocused = false;
                    return ScreenResult.stay(this);
                }
            }
        }
        return ScreenResult.stay(this);
    }

    @Override
    public String view() {
        if (showQuitModal) {
            return TuiHelper.confirmationModal(
                    "QUIT APPLICATION",
                    "Are you sure you want to quit Proctor?",
                    "",
                    "Quit",
                    "Cancel",
                    quitConfirmFocused
            );
        }
        return AuthViews.renderStartup(focusedButton);
    }
}
