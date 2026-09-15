package com.proctor.model.enums;

import java.util.Arrays;
import java.util.Optional;

public enum PredefinedSubject {
    JAVASCRIPT("JS", "JavaScript", "JavaScript Programming & Web Scripting"),
    HTML("HTML", "HTML", "HyperText Markup Language & Web Structure"),
    CSS("CSS", "CSS", "Cascading Style Sheets & Responsive Styling"),
    SQL("SQL", "SQL", "Relational Database Design & Querying"),
    PYTHON("PYTHON", "Python", "Python Programming Language & Ecosystem"),
    LINUX("LINUX", "Linux", "Linux Commands, Shell & System Administration"),
    TYPESCRIPT("TS", "TypeScript", "Typed Superset of JavaScript"),
    JAVA("JAVA", "Java", "Java Object-Oriented Application Development"),
    CSHARP("C#", "C#", "C# Programming Language & .NET Framework"),
    CPP("C++", "C++", "C++ Systems & High-Performance Programming"),
    C("C", "C", "C Systems & Low-Level Programming"),
    PHP("PHP", "PHP", "PHP Server-Side Web Scripting"),
    GO("GO", "Go", "Go (Golang) Concurrent Systems Programming"),
    RUST("RUST", "Rust", "Rust Memory-Safe Systems Programming"),
    KOTLIN("KOTLIN", "Kotlin", "Kotlin Modern Multiplatform & JVM Development");

    private final String code;
    private final String displayName;
    private final String description;

    PredefinedSubject(String code, String displayName, String description) {
        this.code = code;
        this.displayName = displayName;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public static Optional<PredefinedSubject> findByCode(String code) {
        if (code == null) return Optional.empty();
        return Arrays.stream(values())
                .filter(s -> s.code.equalsIgnoreCase(code.trim()))
                .findFirst();
    }

    public static Optional<PredefinedSubject> findByName(String name) {
        if (name == null) return Optional.empty();
        return Arrays.stream(values())
                .filter(s -> s.displayName.equalsIgnoreCase(name.trim()))
                .findFirst();
    }
}
