package com.sumutiu.simpleclumps;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static com.sumutiu.simpleclumps.MessagesHelper.*;

public class SimpleClumps implements ModInitializer {

	public static final Path CONFIG_FOLDER = Path.of("config", "SimpleClumps");
	public static final Path CONFIG_FILE = CONFIG_FOLDER.resolve("SimpleClumps.json");

	@Override
	public void onInitialize() {
		if (initPlugin()) {
			DropManager.init(SimpleClumpsConfig.getClumpRadius(), SimpleClumpsConfig.getCleanupMinutes() * 60 * 20);

			// when entities are loaded into a ServerLevel: check item/xp drops
			ServerEntityEvents.ENTITY_LOAD.register((Entity entity, ServerLevel world) -> {
				if (!world.isClientSide()) {
					DropManager.onEntityLoad(entity, world);
				}
			});

			// server tick: used for scheduled cleanup + countdown messages
			ServerTickEvents.END_SERVER_TICK.register(DropManager::handleServerTick);


			if (SimpleClumpsConfig.getEnableTreeCutter()) {
				PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, _) -> {
					if (!world.isClientSide()) {
						LogsCutter.init((ServerLevel) world, player, pos, state);
					}
				});
			}
		} else {
			Logger(2, MOD_INIT_FAILED);
		}
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
			if (!SimpleClumpsConfig.save()) {
				return false;
			}
		}
		return SimpleClumpsConfig.load();
	}
}