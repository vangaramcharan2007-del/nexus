package nexus.integrations.common;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

/**
 * Central configuration manager for external integrations in NEXUS V26.
 *
 * Security & Reliability Principles:
 * - Never hardcode API keys or credentials.
 * - Hierarchical resolution: System Property > Environment Variable > Properties File > Default.
 * - Credential masking for safe diagnostic logging and UI display.
 */
public class IntegrationConfig {
    private static final Properties properties = new Properties();

    static {
        // Attempt to load optional local configuration file
        File propFile = new File("nexus.properties");
        if (propFile.exists()) {
            try (InputStream in = new FileInputStream(propFile)) {
                properties.load(in);
            } catch (Exception ignored) {}
        }
    }

    /**
     * Resolves a configuration value from system properties, environment, or properties file.
     */
    public static String get(String key, String defaultValue) {
        // 1. System property (e.g., -Dnexus.ntfy.topic=my-alerts)
        String val = System.getProperty(key);
        if (val != null && !val.trim().isEmpty()) {
            return val.trim();
        }

        // 2. Environment variable (e.g., NEXUS_NTFY_TOPIC)
        String envKey = key.toUpperCase().replace('.', '_');
        val = System.getenv(envKey);
        if (val != null && !val.trim().isEmpty()) {
            return val.trim();
        }

        // 3. Properties file
        val = properties.getProperty(key);
        if (val != null && !val.trim().isEmpty()) {
            return val.trim();
        }

        // 4. Default fallback
        return defaultValue;
    }

    public static int getInt(String key, int defaultValue) {
        String val = get(key, null);
        if (val != null) {
            try {
                return Integer.parseInt(val);
            } catch (NumberFormatException ignored) {}
        }
        return defaultValue;
    }

    public static boolean getBoolean(String key, boolean defaultValue) {
        String val = get(key, null);
        if (val != null) {
            return Boolean.parseBoolean(val);
        }
        return defaultValue;
    }

    /**
     * Masks sensitive tokens or credentials for logs and UI display.
     */
    public static String mask(String secret) {
        if (secret == null || secret.isEmpty()) {
            return "<none>";
        }
        if (secret.length() <= 4) {
            return "****";
        }
        return secret.substring(0, 2) + "****" + secret.substring(secret.length() - 2);
    }
}
