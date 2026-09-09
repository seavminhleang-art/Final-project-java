package com.proctor.controller;

import com.williamcallahan.tui4j.compat.bubbletea.Command;
import com.williamcallahan.tui4j.compat.bubbletea.Message;

public interface Screen {
    default Command init() {
        return null;
    }

    ScreenResult update(Message msg);

    String view();
}