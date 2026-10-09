package com.sumutiu.simpleclumps;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static com.sumutiu.simpleclumps.MessagesHelper.*;

public class SimpleClumps implements ModInitializer {

    public static Path CONFIG_FOLDER;
    public static Path CONFIG_FILE;

    public static volatile boolean SimpleClumpsInitialized = false;

    @Override
    public void onInitialize() {

        // -----------------------------
        // SERVER START (WORLD EXISTS)
        // -----------------------------
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {

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
        });

        // When entities are added to a world (new drops, and drops in chunks that load): queue item and XP drops
        ServerEntityEvents.ENTITY_LOAD.register((Entity entity, ServerLevel world) -> {
            if (SimpleClumpsInitialized) {
                DropManager.onEntityLoad(entity, world);
            }
        });

        // Server tick: merging, scheduled cleanup and countdown messages
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (SimpleClumpsInitialized) {
                DropManager.handleServerTick(server);
            }
        });

        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, _) -> {
            if (SimpleClumpsInitialized && world instanceof ServerLevel level && player instanceof ServerPlayer serverPlayer) {
                LogsCutter.init(level, serverPlayer, pos, state);
            }
        });

        ServerPlayConnectionEvents.JOIN.register((handler, _, _) -> {
            ServerPlayer player = handler.getPlayer();

            if (!SimpleClumpsInitialized) {
                player.connection.disconnect(
                        Component.literal(MOD_NOT_INITIALIZED)
                );
            }
        });

        ServerLifecycleEvents.SERVER_STOPPED.register(_ -> {
            SimpleClumpsInitialized = false;
            DropManager.clear();
        });
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
