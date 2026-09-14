package com.proctor;

import com.proctor.model.repository.UserRepository;
import com.proctor.model.service.AuthService;
import com.proctor.model.service.SeedService;
import com.proctor.config.DatabaseConnection;
import com.proctor.config.SchemaInitializer;
import com.proctor.controller.AppModel;
import com.proctor.controller.StartupScreen;
import com.williamcallahan.tui4j.compat.bubbletea.Program;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Main {
    public static void main(String[] args) {

        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "warn");
        Logger.getLogger("org.jline").setLevel(Level.OFF);
        PrintStream originalErr = System.err;
        System.setErr(new PrintStream(OutputStream.nullOutputStream()));

        try {
            DatabaseConnection.init();
            SchemaInitializer.initialize();

            UserRepository userRepository = new UserRepository();
            SeedService seedService = new SeedService(userRepository);
            seedService.seedDefaultAdmin();
            seedService.seedDefaultSubjects();

            AuthService authService = new AuthService(userRepository);

            StartupScreen startupScreen = new StartupScreen(authService);
            new Program(new AppModel(startupScreen)).withAltScreen().run();
        } catch (Throwable t) {
            System.setErr(originalErr);
            System.out.println("\n\u001B[1;31m[PROCTOR ERROR] Application error:\u001B[0m");
            System.out.println("\u001B[31m✖ " + (t.getMessage() != null ? t.getMessage() : t.getClass().getSimpleName()) + "\u001B[0m");
            Throwable cause = t.getCause();
            while (cause != null) {
                if (cause.getMessage() != null && !cause.getMessage().isBlank()) {
                    System.out.println("\u001B[33m  Caused by: " + cause.getMessage() + "\u001B[0m");
                }
                cause = cause.getCause();   
            }
            System.out.println();
            String fullErr = (t.getMessage() != null ? t.getMessage() : "") + (t.getCause() != null && t.getCause().getMessage() != null ? t.getCause().getMessage() : "");
            if (fullErr.contains("Connection") || fullErr.contains("Hikari") || fullErr.contains("database") || fullErr.contains("refused") || fullErr.contains("authentication")) {
                System.out.println("Troubleshooting PostgreSQL Database Connection:");
                System.out.println("  1. Is PostgreSQL running? Check: sudo systemctl status postgresql (or: sudo systemctl start postgresql)");
                System.out.println("  2. Does the database exist? Run: sudo -u postgres psql -c \"CREATE DATABASE proctor_db;\"");
                System.out.println("  3. Verify credentials in config.properties (db.url, db.username, db.password).\n");
            }
            System.exit(1);
        } finally {
            DatabaseConnection.close();
        }
    }
}