package com.sumutiu.simpleclumps;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static com.sumutiu.simpleclumps.SimpleClumps.CONFIG_FILE;
import static com.sumutiu.simpleclumps.MessagesHelper.*;

public class SimpleClumpsConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static class ConfigData {
        public Integer SimpleClumps_CleanupMinutes = 5;
        public Integer SimpleClumps_ClumpRadius = 5;
        public Boolean SimpleClumps_EnableTreeCutter = true;
    }

    private static final ConfigData config_Default = new ConfigData();
    private static ConfigData config = new ConfigData();

    // Writes a new config file with the default values
    public static boolean createDefault() {
        config = new ConfigData();
        return save();
    }

    public static boolean save() {
        // Serialize to a String first: Gson wraps write errors in an unchecked JsonIOException
        try {
            Files.writeString(CONFIG_FILE, GSON.toJson(config), StandardCharsets.UTF_8);
            Logger(0, String.format(CONFIG_SAVED, CONFIG_FILE));
            return true;
        } catch (IOException e) {
            Logger(2, String.format(CONFIG_SAVE_FAILED, e.getMessage()));
            return false;
        }
    }

    // Never fails: if the file can't be read, the defaults are used for this run
    public static void load() {
        boolean updated = false;
        try {
            String text = Files.readString(CONFIG_FILE, StandardCharsets.UTF_8);
            ConfigData loaded = GSON.fromJson(text, ConfigData.class);
            if (loaded != null) {
                config = loaded;

                // Check for fields set to null
                if (config.SimpleClumps_CleanupMinutes == null) {
                    config.SimpleClumps_CleanupMinutes = config_Default.SimpleClumps_CleanupMinutes;
                    updated = true;
                }
                if (config.SimpleClumps_ClumpRadius == null) {
                    config.SimpleClumps_ClumpRadius = config_Default.SimpleClumps_ClumpRadius;
                    updated = true;
                }
                if (config.SimpleClumps_EnableTreeCutter == null) {
                    config.SimpleClumps_EnableTreeCutter = config_Default.SimpleClumps_EnableTreeCutter;
                    updated = true;
                }

                // Check for fields missing from the file (Gson keeps the default value for those,
                // so they are never null): write them to the file
                JsonObject json = JsonParser.parseString(text).getAsJsonObject();
                for (String key : GSON.toJsonTree(config).getAsJsonObject().keySet()) {
                    if (!json.has(key)) {
                        updated = true;
                    }
                }
                Logger(0, CONFIG_LOADED);
            } else {
                Logger(1, CONFIG_LOAD_FAILED_MALFORMED);
                config = new ConfigData();
                updated = true;
            }
        } catch (IOException | JsonParseException e) {
            // Use the defaults for this run, but never overwrite a file the admin may be editing
            Logger(2, String.format(CONFIG_LOAD_FAILED, e.getMessage()));
            config = new ConfigData();
            return;
        }

        // Save updated file if defaults were added
        if (updated) save();

        validate();
    }

    // Out-of-range values are replaced in memory only (with a warning), the file is left as it is
    private static void validate() {
        config.SimpleClumps_CleanupMinutes = atLeast("SimpleClumps_CleanupMinutes", config.SimpleClumps_CleanupMinutes, 0);
        config.SimpleClumps_ClumpRadius = atLeast("SimpleClumps_ClumpRadius", config.SimpleClumps_ClumpRadius, 1);
    }

    private static int atLeast(String key, int value, int minimum) {
        if (value >= minimum) return value;
        Logger(1, String.format(CONFIG_INVALID_VALUE, key, value, minimum));
        return minimum;
    }

    // 0 = the scheduled cleanup is turned off
    public static int getCleanupMinutes() {
        return config.SimpleClumps_CleanupMinutes;
    }

    public static int getClumpRadius() {
        return config.SimpleClumps_ClumpRadius;
    }

    public static boolean getEnableTreeCutter() {
        return config.SimpleClumps_EnableTreeCutter;
    }
}
