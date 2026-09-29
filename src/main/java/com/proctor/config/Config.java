package com.proctor.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

public class Config {
    private static final Properties props = new Properties();

    static {
        loadProperties();
    }

    // Load configuration key-values from external file or classpath resource
    private static void loadProperties() {
        try {
            File externalFile = new File("config.properties");
            if (externalFile.exists()) {
                try (InputStream input = new FileInputStream(externalFile)) {
                    props.load(input);
                    return;
                }
            }
            try (InputStream input = Config.class.getClassLoader().getResourceAsStream("config.properties")) {
                if (input != null) {
                    props.load(input);
                }
            }
        } catch (Exception e) {
            System.err.println("Warning: Failed to load config.properties, using defaults.");
        }
    }

    // Retrieve string property with fallback default
    public static String get(String key, String defaultValue) {
        String sys = System.getProperty(key);
        if (sys != null && !sys.isBlank()) return sys;
        return props.getProperty(key, defaultValue);
    }

    // Retrieve string property without default
    public static String get(String key) {
        String sys = System.getProperty(key);
        if (sys != null && !sys.isBlank()) return sys;
        return props.getProperty(key);
    }

    // Retrieve integer property with fallback default
    public static int getInt(String key, int defaultValue) {
        String val = get(key, null);
        if (val == null || val.isBlank()) return defaultValue;
        try {
            return Integer.parseInt(val.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
