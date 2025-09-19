package com.sumutiu.simpleclumps;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.entity.Entity;

public class SimpleClumps implements ModInitializer {
	// radius (blocks) for clumping in each axis
	public static final int CLUMP_RADIUS = 10;

	// cleanup interval: 5 minutes (ticks)
	public static final int CLEAN_INTERVAL_TICKS = 5 * 60 * 20;

	@Override
	public void onInitialize() {
		DropManager.init(CLUMP_RADIUS, CLEAN_INTERVAL_TICKS);

		// when entities are loaded into a ServerWorld: check item/xp drops
		ServerEntityEvents.ENTITY_LOAD.register((Entity entity, ServerWorld world) -> {
			if (world == null) return;
			if (!world.isClient()) {
				DropManager.onEntityLoad(entity, world);
			}
		});

		// server tick: used for scheduled cleanup + countdown messages
		ServerTickEvents.END_SERVER_TICK.register(DropManager::handleServerTick);
	}
}
