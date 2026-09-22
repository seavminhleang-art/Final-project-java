package com.proctor.controller;

import com.proctor.model.service.AuthService;
import com.proctor.util.KeyUtil;
import com.proctor.util.MouseUtil;
import com.proctor.util.TuiHelper;
import com.proctor.view.AuthViews;
import com.williamcallahan.tui4j.compat.bubbletea.KeyPressMessage;
import com.williamcallahan.tui4j.compat.bubbletea.Message;

public class StartupScreen implements Screen {
    private final AuthService authService;
    private int focusedButton = 0;
    private boolean showQuitModal = false;
    private boolean quitConfirmFocused = false;

    public StartupScreen(AuthService authService) {
        this.authService = authService;
    }

    @Override
    public ScreenResult update(Message msg) {
        if (MouseUtil.isWheelUp(msg)) {
            focusedButton = (focusedButton - 1 + 3) % 3;
            return ScreenResult.stay(this);
        }

        if (MouseUtil.isWheelDown(msg)) {
            focusedButton = (focusedButton + 1) % 3;
            return ScreenResult.stay(this);
        }

        if (MouseUtil.isLeftClick(msg)) {
            if (showQuitModal) {
                int line = MouseUtil.getLineIndex(msg);
                int col = MouseUtil.getColInLine(msg);
                int btnLine = MouseUtil.findButtonRowLine(view());
                if (btnLine != -1 && line >= btnLine && line <= btnLine + 2) {
                    int btn = MouseUtil.getClickedButtonIndex(col, "Quit Application", "Return to App");
                    if (btn == 0) {
                        return ScreenResult.quit();
                    } else if (btn == 1) {
                        showQuitModal = false;
                        return ScreenResult.stay(this);
                    }
                } else if (btnLine != -1 && (line < btnLine - 6 || line > btnLine + 4)) {
                    showQuitModal = false;
                    return ScreenResult.stay(this);
                }
                return ScreenResult.stay(this);
            }

            int line = MouseUtil.getLineIndex(msg);
            int col = MouseUtil.getColInLine(msg);
            if (col >= 0 && col < 26) {
                if (line >= 11 && line <= 13) {
                    focusedButton = 0;
                    return ScreenResult.navigate(new LoginScreen(authService));
                } else if (line >= 15 && line <= 17) {
                    focusedButton = 1;
                    return ScreenResult.navigate(new RegisterRoleScreen(authService));
                } else if (line >= 19 && line <= 21) {
                    focusedButton = 2;
                    showQuitModal = true;
                    quitConfirmFocused = false;
                    return ScreenResult.stay(this);
                }
            }
            return ScreenResult.stay(this);
        }
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

            if (KeyUtil.isUp(k)) {
                focusedButton = (focusedButton - 1 + 3) % 3;
                return ScreenResult.stay(this);
            }

            if (KeyUtil.isDown(k)) {
                focusedButton = (focusedButton + 1) % 3;
                return ScreenResult.stay(this);
            }

            if (KeyUtil.isLeft(k)) {
                focusedButton = (focusedButton - 1 + 3) % 3;
                return ScreenResult.stay(this);
            }

            if (KeyUtil.isRight(k)) {
                focusedButton = (focusedButton + 1) % 3;
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
            return TuiHelper.quitConfirmationModal(quitConfirmFocused);
        }
        return AuthViews.renderStartup(focusedButton);
    }
}
