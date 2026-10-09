package com.lavaflow.common.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class EnvLoader {

    private static final Logger log =
            LoggerFactory.getLogger(EnvLoader.class);

    private EnvLoader() {}

    public static void loadEnv() {

        Path envFile = Path.of(".env");

        if (!Files.exists(envFile)) {
            return;
        }

        try {
            for (String line : Files.readAllLines(envFile)) {

                line = line.trim();

                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }

                int idx = line.indexOf('=');

                if (idx <= 0) {
                    continue;
                }

                String key = line.substring(0, idx).trim();
                String value = line.substring(idx + 1).trim();

                if ((value.startsWith("\"") && value.endsWith("\""))
                        || (value.startsWith("'") && value.endsWith("'"))) {

                    value = value.substring(1, value.length() - 1);
                }

                if (System.getenv(key) == null &&
                        System.getProperty(key) == null) {

                    System.setProperty(key, value);
                }
            }

        } catch (IOException e) {
            log.error("Failed to load .env file", e);
        }
    }
}