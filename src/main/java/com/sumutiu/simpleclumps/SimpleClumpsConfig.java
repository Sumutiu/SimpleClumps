package com.sumutiu.simpleclumps;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.*;

import static com.sumutiu.simpleclumps.SimpleClumps.CONFIG_FILE;
import static com.sumutiu.simpleclumps.MessagesHelper.*;

public class SimpleClumpsConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static class ConfigData {
        public Integer SimpleClumps_CleanupMinutes = 5;
        public Integer SimpleClumps_ClumpRadius = 5;
        public Boolean SimpleClumps_EnableTreeCutter = true;
    }

    private static ConfigData config = new ConfigData();

    public static boolean save() {
        try (Writer writer = new FileWriter(CONFIG_FILE.toFile())) {
            GSON.toJson(config, writer);
            Logger(0, DEFAULT_CONFIG_LOADED);
            return true;
        } catch (IOException e) {
            Logger(2, String.format(CONFIG_SAVE_FAILED, e.getMessage()));
            return false;
        }
    }

    public static boolean load() {
        boolean updated = false;
        try (Reader reader = new FileReader(CONFIG_FILE.toFile())) {
            ConfigData loaded = GSON.fromJson(reader, ConfigData.class);
            if (loaded != null) {
                config = loaded;

                // Check for missing fields (null means they weren't present)
                if (config.SimpleClumps_CleanupMinutes == null) { config.SimpleClumps_CleanupMinutes = 5; updated = true; }
                if (config.SimpleClumps_ClumpRadius == null) { config.SimpleClumps_ClumpRadius = 5; updated = true; }
                if (config.SimpleClumps_EnableTreeCutter == null) { config.SimpleClumps_EnableTreeCutter = true; updated = true; }
                Logger(0, CONFIG_LOADED);
            } else {
                Logger(1, CONFIG_LOAD_FAILED_MALFORMED);
                config = new ConfigData();
                updated = true;
            }
        } catch (IOException e) {
            Logger(2, String.format(CONFIG_LOAD_FAILED, e.getMessage()));
            return false;
        }

        // Save updated file if defaults were added
        if (updated) save();

        return true;
    }

    public static int getCleanupMinutes() { return config.SimpleClumps_CleanupMinutes; }
    public static int getClumpRadius() { return config.SimpleClumps_ClumpRadius; }
    public static boolean getEnableTreeCutter() { return config.SimpleClumps_EnableTreeCutter; }
}

