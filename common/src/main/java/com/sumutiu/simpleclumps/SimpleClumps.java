package com.sumutiu.simpleclumps;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static com.sumutiu.simpleclumps.MessagesHelper.*;

/**
 * The mod itself, shared by Fabric and NeoForge.
 * Each loader's entrypoint (SimpleClumpsFabric, SimpleClumpsNeoForge) calls init() once
 * and forwards its events to the on...() methods below.
 */
public class SimpleClumps {

    public static final String MOD_ID = "simpleclumps";

    public static Path CONFIG_FOLDER;
    public static Path CONFIG_FILE;

    public static volatile boolean SimpleClumpsInitialized = false;

    private static String modVersion = "unknown";

    // Called by the loader entrypoint when the mod loads
    public static void init(String version) {
        modVersion = version;
    }

    public static String getModVersion() {
        return modVersion;
    }

    // -----------------------------
    // SERVER START (WORLD EXISTS)
    // -----------------------------
    public static void onServerStarted(MinecraftServer server) {

        long seed = server.getWorldGenSettings().options().seed();

        CONFIG_FOLDER = Path.of("config", "SimpleClumps_Seed_" + Long.toUnsignedString(seed));
        CONFIG_FILE = CONFIG_FOLDER.resolve("SimpleClumps.json");

        if (initPlugin()) {
            DropManager.init(SimpleClumpsConfig.getClumpRadius(), SimpleClumpsConfig.getCleanupMinutes());
            if (SimpleClumpsConfig.getCleanupMinutes() == 0) {
                Logger(0, CLEANUP_DISABLED);
            }

            SimpleClumpsInitialized = true;
        } else {
            Logger(2, MOD_INIT_FAILED);
        }
    }

    // When entities are added to a world (new drops, and drops in chunks that load): queue item and XP drops
    public static void onEntityLoad(Entity entity, ServerLevel world) {
        if (SimpleClumpsInitialized) {
            DropManager.onEntityLoad(entity, world);
        }
    }

    // Server tick: merging, scheduled cleanup and countdown messages
    public static void onServerTick(MinecraftServer server) {
        if (SimpleClumpsInitialized) {
            DropManager.handleServerTick(server);
        }
    }

    // A player broke a block: fells the whole tree when it is a log
    public static void onBlockBroken(ServerLevel world, ServerPlayer player, BlockPos pos, BlockState state) {
        if (SimpleClumpsInitialized) {
            LogsCutter.init(world, player, pos, state);
        }
    }

    public static void onPlayerJoin(ServerPlayer player) {
        if (!SimpleClumpsInitialized) {
            player.connection.disconnect(
                    Component.literal(MOD_NOT_INITIALIZED)
            );
        }
    }

    public static void onServerStopped() {
        SimpleClumpsInitialized = false;
        DropManager.clear();
    }

    private static boolean initPlugin() {
        logAsciiBanner(MOD_ASCII_BANNER, Mod_ID + ": V" + getModVersion() + " - Because your server deserves smooth performance!");

        try {
            if (Files.notExists(CONFIG_FOLDER)) {
                Files.createDirectories(CONFIG_FOLDER);
                Logger(0, MAIN_FOLDER_CREATED);
            }
        } catch (IOException e) {
            Logger(2, MAIN_FOLDER_CREATION_FAILED);
            return false;
        }

        if (Files.notExists(CONFIG_FILE)) {
            if (!SimpleClumpsConfig.createDefault()) {
                return false;
            }
            Logger(0, DEFAULT_CONFIG_LOADED);
        }
        SimpleClumpsConfig.load();
        return true;
    }
}
