package com.sumutiu.simpleclumps;

import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Item;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * DropManager: clumps XP and stackable items, and periodically clears stray drops.
 */
public class DropManager {
    private static int radius = 10;
    private static int cleanIntervalTicks = 5 * 60 * 20;

    private static final Queue<QueuedEntity> pending = new ConcurrentLinkedQueue<>();
    private static final Set<Entity> processedThisTick = new HashSet<>();

    private static int ticksUntilClean = cleanIntervalTicks;
    private static boolean countdownAnnounced30s = false;

    public static void init(int r, int intervalTicks) {
        radius = r;
        cleanIntervalTicks = intervalTicks;
        ticksUntilClean = cleanIntervalTicks;
    }

    public static void onEntityLoad(Entity entity, ServerWorld world) {
        if (entity instanceof ItemEntity || entity instanceof ExperienceOrbEntity) {
            pending.add(new QueuedEntity(entity, world));
        }
    }

    public static void handleServerTick(MinecraftServer server) {
        processedThisTick.clear();
        int processed = 0;
        int maxPerTick = 200;
        while (processed < maxPerTick) {
            QueuedEntity q = pending.poll();
            if (q == null) break;

            if (processedThisTick.contains(q.entity)) {
                continue;
            }

            try {
                if (q.entity instanceof ItemEntity) {
                    mergeNearbyItems((ItemEntity) q.entity, q.world);
                } else if (q.entity instanceof ExperienceOrbEntity) {
                    mergeNearbyOrbs((ExperienceOrbEntity) q.entity, q.world);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            processed++;
        }

        ticksUntilClean--;

        if (!countdownAnnounced30s && ticksUntilClean <= 30 * 20 && ticksUntilClean > 5 * 20) {
            broadcast(server, "Cleaning stray drops in 30 seconds!");
            countdownAnnounced30s = true;
        }

        if (ticksUntilClean <= 5 * 20 && ticksUntilClean > 0) {
            if (ticksUntilClean % 20 == 0) {
                int sec = ticksUntilClean / 20;
                broadcast(server, "Cleaning stray drops in " + sec + "...");
            }
        }

        if (ticksUntilClean <= 0) {
            long removed = performCleanup(server);
            broadcast(server, "Cleaned stray drops. Removed " + removed + " entities.");
            ticksUntilClean = cleanIntervalTicks;
            countdownAnnounced30s = false;
        }
    }

    private static void broadcast(MinecraftServer server, String msg) {
        Text full = Text.literal("[SimpleProtect] ")
                .formatted(Formatting.GREEN)
                .append(Text.literal(msg).formatted(Formatting.WHITE));
        server.getPlayerManager().broadcast(full, false);
    }

    private static void mergeNearbyItems(ItemEntity source, ServerWorld world) {
        if (source == null || !source.isAlive()) return;

        Vec3d pos = source.getPos();
        Box box = new Box(pos.x - radius, pos.y - radius, pos.z - radius,
                pos.x + radius, pos.y + radius, pos.z + radius);

        List<ItemEntity> list = world.getEntitiesByClass(ItemEntity.class, box, ItemEntity::isAlive);
        if (list.isEmpty()) return;

        List<Group> groups = new ArrayList<>();

        for (ItemEntity ie : list) {
            if (processedThisTick.contains(ie)) {
                continue;
            }
            ItemStack st = ie.getStack();
            if (st.isEmpty()) continue;

            boolean added = false;
            for (Group g : groups) {
                if (ItemStack.areItemsAndComponentsEqual(g.prototype, st)) {
                    g.totalCount += st.getCount();
                    g.members.add(ie);
                    added = true;
                    break;
                }
            }
            if (!added) {
                Group g = new Group(st.copy(), st.getCount());
                g.members.add(ie);
                groups.add(g);
            }
        }

        for (Group g : groups) {
            if (g.members.isEmpty() || (g.members.size() == 1 && g.totalCount == 1)) {
                processedThisTick.addAll(g.members);
                continue;
            }

            int total = g.totalCount;
            if (total <= 0) continue;

            processedThisTick.addAll(g.members);
            Vec3d spawnPos = g.members.getFirst().getPos();

            // remove old entities
            for (ItemEntity e : g.members) {
                if (e != null && e.isAlive()) e.discard();
            }

            // respawn minimal stacks
            Item prototype = g.prototype.getItem();
            int max = prototype.getMaxCount();
            Vec3d originalVelocity = source.getVelocity();

            while (total > 0) {
                int size = Math.min(total, max);
                ItemStack newStack = g.prototype.copyWithCount(size);

                ItemEntity created = new ItemEntity(world, spawnPos.x, spawnPos.y, spawnPos.z, newStack);
                created.setToDefaultPickupDelay();
                created.setVelocity(originalVelocity);
                processedThisTick.add(created);

                String itemName = newStack.getName().getString();
                Text label = Text.literal("x" + size).formatted(Formatting.GREEN)
                                .append(Text.literal(" " + itemName).formatted(Formatting.WHITE));
                created.setCustomName(label);
                created.setCustomNameVisible(true);

                world.spawnEntity(created);
                total -= size;
            }
        }
    }

    private static void mergeNearbyOrbs(ExperienceOrbEntity source, ServerWorld world) {
        if (source == null || !source.isAlive()) return;

        Vec3d pos = source.getPos();
        Box box = new Box(pos.x - radius, pos.y - radius, pos.z - radius,
                pos.x + radius, pos.y + radius, pos.z + radius);

        List<ExperienceOrbEntity> list = world.getEntitiesByClass(ExperienceOrbEntity.class, box, ExperienceOrbEntity::isAlive);
        if (list.size() <= 1) return;

        int totalXp = 0;
        Vec3d spawnPos = source.getPos();

        for (ExperienceOrbEntity orb : list) {
            totalXp += orb.getValue(); // Mojang mapping
            orb.discard();
        }

        ExperienceOrbEntity.spawn(world, spawnPos, totalXp);
    }

    private static long performCleanup(MinecraftServer server) {
        long removed = 0;

        for (ServerWorld world : server.getWorlds()) {
            // Box covering the whole world (arbitrary large box)
            Box worldBox = new Box(-30000000, -64, -30000000, 30000000, 320, 30000000);

            // Clean ItemEntities
            for (ItemEntity e : world.getEntitiesByClass(ItemEntity.class, worldBox, entity -> true)) {
                e.discard();
                removed++;
            }

            // Clean ExperienceOrbEntities
            for (ExperienceOrbEntity orb : world.getEntitiesByClass(ExperienceOrbEntity.class, worldBox, entity -> true)) {
                orb.discard();
                removed++;
            }
        }

        return removed;
    }

    private static class Group {
        ItemStack prototype;
        int totalCount;
        List<ItemEntity> members = new ArrayList<>();

        Group(ItemStack prototype, int totalCount) {
            this.prototype = prototype;
            this.totalCount = totalCount;
        }
    }

    private static class QueuedEntity {
        final Entity entity;
        final ServerWorld world;
        QueuedEntity(Entity e, ServerWorld w) { this.entity = e; this.world = w; }
    }
}
