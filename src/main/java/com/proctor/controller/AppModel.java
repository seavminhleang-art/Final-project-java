package com.proctor.controller;

import com.proctor.util.TuiHelper;
import com.williamcallahan.tui4j.compat.bubbletea.*;
import com.williamcallahan.tui4j.compat.bubbletea.input.MouseAction;
import com.williamcallahan.tui4j.compat.bubbletea.input.MouseMessage;
import com.williamcallahan.tui4j.input.MouseTarget;
import com.williamcallahan.tui4j.input.MouseTargetProvider;

import java.util.List;

public class AppModel implements Model, MouseTargetProvider {
    private Screen currentScreen;
    private String runtimeError = null;

    public AppModel(Screen initialScreen) {
        this.currentScreen = initialScreen;
    }

    @Override
    public List<MouseTarget> mouseTargets() {
        return TuiHelper.getHitMap().getMouseTargets();
    }

    @Override
    public Command init() {
        try {
            return currentScreen != null ? currentScreen.init() : null;
        } catch (Throwable t) {
            runtimeError = t.getMessage() != null ? t.getMessage() : t.getClass().getSimpleName();
            return null;
        }
    }

    @Override
    public UpdateResult<? extends Model> update(Message msg) {
        if (msg instanceof WindowSizeMessage w) {
            TuiHelper.setTerminalDimensions(w.width(), w.height());
        }

        if (msg instanceof MouseMessage m && m.getAction() == MouseAction.MouseActionMotion) {
            return new UpdateResult<>(this, null);
        }

        if (msg instanceof KeyPressMessage k && k.key().equals("ctrl+c")) {
            return new UpdateResult<>(this, QuitMessage::new);
        }

        if (currentScreen != null) {
            try {
                ScreenResult result = currentScreen.update(msg);
                if (result == null) {
                    return new UpdateResult<>(this, null);
                }
                if (result.shouldQuit()) {
                    return new UpdateResult<>(this, QuitMessage::new);
                }
                if (result.nextScreen() != null) {
                    this.currentScreen = result.nextScreen();
                    this.runtimeError = null;
                }
                return new UpdateResult<>(this, result.command());
            } catch (Throwable t) {
                this.runtimeError = t.getMessage() != null ? t.getMessage() : t.getClass().getSimpleName();
                return new UpdateResult<>(this, null);
            }
        }

        return new UpdateResult<>(this, null);
    }

    @Override
    public String view() {
        if (currentScreen == null) {
            return "";
        }
        try {
            String rendered = currentScreen.view();
            if (runtimeError != null) {
                rendered = TuiHelper.red("✖ System Error: " + runtimeError) + "\n\n" + rendered;
            }
            return TuiHelper.centerLayout(rendered);
        } catch (Throwable t) {
            return TuiHelper.centerLayout(
                    TuiHelper.header("APPLICATION ERROR") + "\n"
                    + TuiHelper.boxTitle("An unexpected error occurred") + "\n\n"
                    + TuiHelper.red("✖ " + (t.getMessage() != null ? t.getMessage() : t.getClass().getName())) + "\n\n"
                    + TuiHelper.dim("Press [Esc] to return or [Ctrl+C] to quit.")
            );
        }
    }
}