package com.sumutiu.simpleclumps;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
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

    private static final Logger LOGGER = LoggerFactory.getLogger(Mod_ID);

    // ----------------------------
    // Server messaging
    // ----------------------------
    public static void ServerBroadcast(MinecraftServer server, String msg) {
        Text full = Text.literal(Mod_ID + ": ")
                .formatted(Formatting.GREEN)
                .append(Text.literal(msg).formatted(Formatting.WHITE));
        server.getPlayerManager().broadcast(full, false);
    }

    // ----------------------------
    // Logging
    // ----------------------------
    public static void Logger(int type, String message) {
        switch (type) {
            case 0 -> LOGGER.info(message);
            case 1 -> LOGGER.warn(message);
            case 2 -> LOGGER.error(message);
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
