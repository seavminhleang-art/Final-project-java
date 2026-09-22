package com.proctor.controller;

import com.proctor.model.repository.UserRepository;
import com.proctor.model.service.AuthService;
import com.proctor.model.service.UserService;
import com.proctor.util.KeyUtil;
import com.proctor.util.MouseUtil;
import com.proctor.view.AuthViews;
import com.williamcallahan.tui4j.compat.bubbletea.KeyPressMessage;
import com.williamcallahan.tui4j.compat.bubbletea.Message;

public class RegisterRoleScreen implements Screen {
    private final AuthService authService;
    private int focusedButton = 0;

    public RegisterRoleScreen(AuthService authService) {
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
            int line = MouseUtil.getLineIndex(msg);
            int col = MouseUtil.getColInLine(msg);
            if (col >= 0 && col < 26) {
                UserService userService = new UserService(new UserRepository());
                if (line >= 11 && line <= 13) {
                    focusedButton = 0;
                    return ScreenResult.navigate(new StudentRegisterScreen(authService, userService));
                } else if (line >= 15 && line <= 17) {
                    focusedButton = 1;
                    return ScreenResult.navigate(new TeacherRegisterScreen(authService, userService));
                } else if (line >= 19 && line <= 21) {
                    focusedButton = 2;
                    return ScreenResult.navigate(new StartupScreen(authService));
                }
            }
            return ScreenResult.stay(this);
        }
        if (msg instanceof KeyPressMessage k) {
            if (KeyUtil.isEsc(k)) {
                return ScreenResult.navigate(new StartupScreen(authService));
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
                UserService userService = new UserService(new UserRepository());
                if (focusedButton == 0) {
                    return ScreenResult.navigate(new StudentRegisterScreen(authService, userService));
                } else if (focusedButton == 1) {
                    return ScreenResult.navigate(new TeacherRegisterScreen(authService, userService));
                } else if (focusedButton == 2) {
                    return ScreenResult.navigate(new StartupScreen(authService));
                }
            }
        }
        return ScreenResult.stay(this);
    }

    @Override
    public String view() {
        return AuthViews.renderRegisterRole(focusedButton);
    }
}
