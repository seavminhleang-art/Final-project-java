package com.proctor.controller;

import com.williamcallahan.tui4j.compat.bubbletea.Command;

public record ScreenResult(Screen nextScreen, Command command, boolean shouldQuit) {
    public static ScreenResult stay(Screen screen) {
        return new ScreenResult(screen, null, false);
    }

    public static ScreenResult stay(Screen screen, Command cmd) {
        return new ScreenResult(screen, cmd, false);
    }

    public static ScreenResult navigate(Screen newScreen) {
        if (newScreen == null) {
            return new ScreenResult(null, null, false);
        }
        return new ScreenResult(newScreen, newScreen.init(), false);
    }

    public static ScreenResult quit() {
        return new ScreenResult(null, null, true);
    }
}