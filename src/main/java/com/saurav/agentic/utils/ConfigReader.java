package com.saurav.agentic.utils;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * ConfigReader - Reads config.properties and config.local.properties
 *
 * Priority order:
 * 1. config.local.properties (highest - your secrets)
 * 2. config.properties (base config - safe for GitHub)
 *
 */
public class ConfigReader {

    private static final Properties properties = new Properties();
    private static ConfigReader instance;

    private static final String BASE_CONFIG = "config.properties";
    private static final String LOCAL_CONFIG = "config.local.properties";

    private ConfigReader() {
        loadConfig();
    }

    /**
     * Singleton - only one instance of ConfigReader
     */
    public static ConfigReader getInstance() {
        if (instance == null) {
            instance = new ConfigReader();
        }
        return instance;
    }

    /**
     * Load both config files
     * Local properties override base properties
     */
    private void loadConfig() {
        // Step 1: Load base config.properties
        try (InputStream baseStream = new FileInputStream(BASE_CONFIG)) {
            properties.load(baseStream);
            System.out.println("✅ Loaded: " + BASE_CONFIG);
        } catch (IOException e) {
            System.err.println("⚠️  config.properties not found at project root!");
        }

        // Step 2: Load config.local.properties (overrides base)
        try (InputStream localStream = new FileInputStream(LOCAL_CONFIG)) {
            Properties localProps = new Properties();
            localProps.load(localStream);
            properties.putAll(localProps); // local overrides base
            System.out.println("✅ Loaded: " + LOCAL_CONFIG + " (local overrides applied)");
        } catch (IOException e) {
            System.out.println("ℹ️  config.local.properties not found — using base config only");
        }
    }

    /**
     * Get a string property value
     */
    public String get(String key) {
        String value = properties.getProperty(key);
        if (value == null) {
            throw new RuntimeException("❌ Property not found: '" + key + "' in config files");
        }
        return value.trim();
    }

    /**
     * Get a string property with a default fallback
     */
    public String get(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue).trim();
    }

    /**
     * Get a boolean property
     */
    public boolean getBoolean(String key) {
        return Boolean.parseBoolean(get(key));
    }

    /**
     * Get a boolean property with a default fallback
     */
    public boolean getBoolean(String key, boolean defaultValue) {
        String val = properties.getProperty(key);
        if (val == null) return defaultValue;
        return Boolean.parseBoolean(val.trim());
    }

    /**
     * Get an integer property
     */
    public int getInt(String key) {
        return Integer.parseInt(get(key));
    }

    /**
     * Get Groq API Key — checks env variable first, then config file
     */
    public String getGroqApiKey() {
        // Check environment variable first (useful for CI/CD)
        String envKey = System.getenv("GROQ_API_KEY");
        if (envKey != null && !envKey.isEmpty()) {
            System.out.println("✅ Groq API Key loaded from environment variable");
            return envKey;
        }
        // Fall back to config file
        String configKey = get("groq.api.key");
        if (configKey.equals("YOUR_GROQ_API_KEY_HERE") || configKey.equals("your_actual_groq_api_key_here")) {
            throw new RuntimeException(
                "Groq API key not set!\n" +
                "Option 1: Set GROQ_API_KEY environment variable\n" +
                "Option 2: Add groq.api.key=your_key in config.local.properties"
            );
        }
        return configKey;
    }
}
