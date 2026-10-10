package com.sumutiu.simpleclumps.neoforge;

import com.sumutiu.simpleclumps.SimpleClumps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * NeoForge entrypoint (see neoforge.mods.toml): passes the NeoForge events to the common code.
 * Only NeoForge creates it, by finding @Mod, so IntelliJ thinks it is never used.
 */
@SuppressWarnings("unused")
@Mod(SimpleClumps.MOD_ID)
public class SimpleClumpsNeoForge {

    public SimpleClumpsNeoForge(ModContainer container) {
        SimpleClumps.init(container.getModInfo().getVersion().toString());

        IEventBus events = NeoForge.EVENT_BUS;

        events.addListener(ServerStartedEvent.class, event -> SimpleClumps.onServerStarted(event.getServer()));

        // Lowest priority: runs after the other mods, and not at all when one of them stopped the entity from joining
        events.addListener(EventPriority.LOWEST, EntityJoinLevelEvent.class, event -> {
            if (event.getLevel() instanceof ServerLevel level) {
                SimpleClumps.onEntityLoad(event.getEntity(), level);
            }
        });

        events.addListener(ServerTickEvent.Post.class, event -> SimpleClumps.onServerTick(event.getServer()));

        // NeoForge has no "after break" event like Fabric: this one fires just before the block is removed.
        // Lowest priority: protection mods have already checked it, and a cancelled break never gets here.
        events.addListener(EventPriority.LOWEST, BreakBlockEvent.class, event -> {
            if (event.getLevel() instanceof ServerLevel level && event.getPlayer() instanceof ServerPlayer player) {
                SimpleClumps.onBlockBroken(level, player, event.getPos(), event.getState());
            }
        });

        events.addListener(PlayerEvent.PlayerLoggedInEvent.class, event -> {
            if (event.getEntity() instanceof ServerPlayer player) {
                SimpleClumps.onPlayerJoin(player);
            }
        });

        events.addListener(ServerStoppedEvent.class, _ -> SimpleClumps.onServerStopped());
    }
}
