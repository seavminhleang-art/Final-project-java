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

    public static String get(String key, String defaultValue) {
        return props.getProperty(key, defaultValue);
    }

    public static String get(String key) {
        return props.getProperty(key);
    }

    public static int getInt(String key, int defaultValue) {
        String val = props.getProperty(key);
        if (val == null) return defaultValue;
        try {
            return Integer.parseInt(val.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}