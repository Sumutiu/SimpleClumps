package com.sumutiu.simpleclumps.fabric;

import com.sumutiu.simpleclumps.SimpleClumps;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * Fabric entrypoint (see fabric.mod.json): passes the Fabric API events to the common code.
 * Only Fabric creates it, from the name in fabric.mod.json, so IntelliJ thinks it is never used.
 */
@SuppressWarnings("unused")
public class SimpleClumpsFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        SimpleClumps.init(FabricLoader.getInstance()
                .getModContainer(SimpleClumps.MOD_ID)
                .map(mod -> mod.getMetadata().getVersion().getFriendlyString())
                .orElse("unknown"));

        ServerLifecycleEvents.SERVER_STARTED.register(SimpleClumps::onServerStarted);

        ServerEntityEvents.ENTITY_LOAD.register(SimpleClumps::onEntityLoad);

        ServerTickEvents.END_SERVER_TICK.register(SimpleClumps::onServerTick);

        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, _) -> {
            if (world instanceof ServerLevel level && player instanceof ServerPlayer serverPlayer) {
                SimpleClumps.onBlockBroken(level, serverPlayer, pos, state);
            }
        });

        ServerPlayConnectionEvents.JOIN.register((handler, _, _) -> SimpleClumps.onPlayerJoin(handler.getPlayer()));

        ServerLifecycleEvents.SERVER_STOPPED.register(_ -> SimpleClumps.onServerStopped());
    }
}
