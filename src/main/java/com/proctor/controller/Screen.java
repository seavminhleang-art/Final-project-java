package com.proctor.controller;

import com.williamcallahan.tui4j.compat.bubbletea.Command;
import com.williamcallahan.tui4j.compat.bubbletea.Message;

// TUI screen lifecycle interface (init, update event loop, and view rendering)
public interface Screen {
    default Command init() {
        return null;
    }

    ScreenResult update(Message msg);

    String view();
}
