package com.proctor.controller;

import com.proctor.util.TuiHelper;
import com.williamcallahan.tui4j.compat.bubbletea.*;

public class AppModel implements Model {
    private Screen currentScreen;

    public AppModel(Screen initialScreen) {
        this.currentScreen = initialScreen;
    }

    @Override
    public Command init() {
        return currentScreen != null ? currentScreen.init() : null;
    }

    @Override
    public UpdateResult<? extends Model> update(Message msg) {
        if (msg instanceof WindowSizeMessage w) {
            TuiHelper.setTerminalDimensions(w.width(), w.height());
        }

        if (msg instanceof KeyPressMessage k && k.key().equals("ctrl+c")) {
            return new UpdateResult<>(this, QuitMessage::new);
        }

        if (currentScreen != null) {
            ScreenResult result = currentScreen.update(msg);
            if (result.shouldQuit()) {
                return new UpdateResult<>(this, QuitMessage::new);
            }
            if (result.nextScreen() != null) {
                this.currentScreen = result.nextScreen();
            }
            return new UpdateResult<>(this, result.command());
        }

        return new UpdateResult<>(this, null);
    }

    @Override
    public String view() {
        return currentScreen != null ? TuiHelper.centerLayout(currentScreen.view()) : "";
    }
}