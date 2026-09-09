package com.proctor;

import com.proctor.model.repository.UserRepository;
import com.proctor.model.service.AuthService;
import com.proctor.model.service.SeedService;
import com.proctor.config.DatabaseConnection;
import com.proctor.config.SchemaInitializer;
import com.proctor.controller.AppModel;
import com.proctor.controller.LoginScreen;
import com.williamcallahan.tui4j.compat.bubbletea.Program;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Main {
    public static void main(String[] args) {

        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "warn");
        Logger.getLogger("org.jline").setLevel(Level.OFF);
        System.setErr(new PrintStream(OutputStream.nullOutputStream()));

        DatabaseConnection.init();
        SchemaInitializer.initialize();

        UserRepository userRepository = new UserRepository();
        SeedService seedService = new SeedService(userRepository);
        seedService.seedDefaultAdmin();

        AuthService authService = new AuthService(userRepository);

        LoginScreen loginScreen = new LoginScreen(authService);
        new Program(new AppModel(loginScreen)).withAltScreen().run();

        DatabaseConnection.close();
    }
}