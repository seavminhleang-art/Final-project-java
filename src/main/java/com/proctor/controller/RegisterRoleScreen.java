package com.proctor.controller;

import com.proctor.model.repository.UserRepository;
import com.proctor.model.service.AuthService;
import com.proctor.model.service.UserService;
import com.proctor.util.KeyUtil;
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
        if (msg instanceof KeyPressMessage k) {
            if (KeyUtil.isEsc(k)) {
                return ScreenResult.navigate(new StartupScreen(authService));
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
