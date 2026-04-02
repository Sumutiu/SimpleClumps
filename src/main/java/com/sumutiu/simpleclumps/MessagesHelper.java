package com.sumutiu.simpleclumps;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MessagesHelper {

    // ----------------------------
    // Core / General
    // ----------------------------
    public static final String MOD_ASCII_BANNER = """
          _____ _                 _       _____ _                          \s
         / ____(_)               | |     / ____| |                         \s
        | (___  _ _ __ ___  _ __ | | ___| |    | |_   _ _ __ ___  _ __  ___\s
         \\___ \\| | '_ ` _ \\| '_ \\| |/ _ \\ |    | | | | | '_ ` _ \\| '_ \\/ __|
         ____) | | | | | | | |_) | |  __/ |____| | |_| | | | | | | |_) \\__ \\
        |_____/|_|_| |_| |_| .__/|_|\\___|\\_____|_|\\__,_|_| |_| |_| .__/|___/
                           | |                                   | |       \s
                           |_|                                   |_|       \s
        """;

    public static final String Mod_ID = "[SimpleClumps]";

    // ----------------------------
    // SimpleClumps - General
    // ----------------------------
    public static final String CLEANING_DROPS = "Cleaning stray drops in 30 seconds!";
    public static final String CLEANING_DROPS_SCHEDULE = "Cleaning stray drops in %d seconds...";
    public static final String CLEANING_DROPS_CONFIRM = "Cleaned stray drops. Removed %d entities.";
    public static final String ERROR_MERGING = "Error in merging the nearby entities / orbs. Error: %s.";
    public static final String MAIN_FOLDER_CREATED = "Main Config folder has been created.";
    public static final String MAIN_FOLDER_CREATION_FAILED = "Failed to create the main Config folder.";
    public static final String DEFAULT_CONFIG_LOADED = "Default Config created. Please edit the default values in the Config folder.";
    public static final String CONFIG_SAVE_FAILED = "Failed to save SimpleClumps config: %s";
    public static final String CONFIG_LOADED = "Successfully loaded configuration.";
    public static final String CONFIG_LOAD_FAILED = "Failed to load SimpleClumps config: %s";
    public static final String CONFIG_LOAD_FAILED_MALFORMED = "Failed to load configuration due to malformed JSON. Loading default settings.";
    public static final String MOD_INIT_FAILED = "Mod has failed to initialize. Error in creating the mod Config folder.";

    private static final Logger LOGGER = LoggerFactory.getLogger(Mod_ID);

    // ----------------------------
    // Server messaging
    // ----------------------------
    public static void ServerBroadcast(MinecraftServer server, String msg) {
        Component full = Component.literal(Mod_ID + ": ")
                .withStyle(style -> style.withColor(ChatFormatting.GREEN))
                .append(Component.literal(msg).withStyle(style -> style.withColor(ChatFormatting.WHITE)));
        server.getPlayerList().broadcastSystemMessage(full, false);
    }

    // ----------------------------
    // Logging
    // ----------------------------
    public static void Logger(int type, String message) {
        switch (type) {
            case 0 -> LOGGER.info(Mod_ID + ": {}", message);
            case 1 -> LOGGER.warn(Mod_ID + ": {}", message);
            case 2 -> LOGGER.error(Mod_ID + ": {}", message);
        }
    }

    // ----------------------------
    // Helper Methods
    // ----------------------------
    public static String getModVersion() {
        return FabricLoader.getInstance()
                .getModContainer("simpleclumps")
                .map(ModContainer::getMetadata)
                .map(meta -> meta.getVersion().getFriendlyString())
                .orElse("unknown");
    }

    public static void logAsciiBanner(String banner, String footer) {
        LOGGER.info(""); // Empty line before
        for (String line : banner.stripTrailing().split("\n")) {
            LOGGER.info(line);
        }
        LOGGER.info(""); // Empty line before
        LOGGER.info(footer);
        LOGGER.info(""); // Empty line after
    }
}
