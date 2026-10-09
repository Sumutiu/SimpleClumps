package com.sumutiu.simpleclumps;

import com.sumutiu.simpleclumps.mixin.ExperienceOrbAccessor;
import com.sumutiu.simpleclumps.mixin.ItemEntityAccessor;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

import static com.sumutiu.simpleclumps.MessagesHelper.*;

/**
 * DropManager: clumps XP and stackable items, and periodically clears stray drops.
 * Merging follows vanilla's rules (see ItemEntity and ExperienceOrb), just over a bigger radius.
 * Everything here runs on the server thread.
 */
public class DropManager {

    private static final int MAX_PER_TICK = 200;

    // Vanilla values: items that can never be picked up, never despawn, or are about to despawn are not merged
    private static final int NEVER_PICK_UP = 32767;
    private static final int UNLIMITED_LIFETIME = -32768;
    private static final int LIFETIME = 6000;

    // An item that can't be picked up yet is checked again when its pickup delay is over.
    // This many times at most, in case the delay doesn't count down (a chunk that is loaded but doesn't tick).
    private static final int MAX_RETRIES = 5;

    // How often the "x64" labels are checked against the stack (a player or hopper can take part of it)
    private static final int LABEL_REFRESH_TICKS = 10;

    // Orb sizes used by vanilla, biggest first
    private static final int[] ORB_SIZES = {2477, 1237, 617, 307, 149, 73, 37, 17, 7, 3, 1};

    private static int radius = 5;
    private static long cleanIntervalTicks = 5 * 60 * 20; // 0 = cleanup turned off

    // New drops, merged at the end of the tick
    private static final Queue<QueuedEntity> pending = new ConcurrentLinkedQueue<>();
    // Items waiting for their pickup delay to end, the first one due first
    private static final Queue<QueuedEntity> waiting = new PriorityQueue<>(Comparator.comparingLong(QueuedEntity::readyAt));
    // Labeled items, and the stack size their label shows
    private static final Map<ItemEntity, Integer> labels = new IdentityHashMap<>();

    private static long currentTick = 0;
    private static long ticksUntilClean = cleanIntervalTicks;
    private static boolean countdownAnnounced30s = false;

    public static void init(int clumpRadius, int cleanupMinutes) {
        radius = clumpRadius;
        cleanIntervalTicks = cleanupMinutes * 60L * 20L;
        ticksUntilClean = cleanIntervalTicks;
        countdownAnnounced30s = false;
    }

    // Server stopped: forget everything (the entities belong to the old world)
    public static void clear() {
        pending.clear();
        waiting.clear();
        labels.clear();
        currentTick = 0;
    }

    public static void onEntityLoad(Entity entity, ServerLevel world) {
        if (entity instanceof ItemEntity || entity instanceof ExperienceOrb) {
            pending.add(new QueuedEntity(entity, world, 0, 0));
        }
    }

    public static void handleServerTick(MinecraftServer server) {
        currentTick++;
        processQueues();

        if (currentTick % LABEL_REFRESH_TICKS == 0) {
            refreshLabels();
        }

        tickCleanup(server);
    }

    private static void processQueues() {
        int processed = 0;

        // Items whose wait is over first, then the new drops
        while (processed < MAX_PER_TICK) {
            QueuedEntity q = waiting.peek();
            if (q == null || q.readyAt > currentTick) break;

            waiting.poll();
            if (process(q)) processed++;
        }

        while (processed < MAX_PER_TICK) {
            QueuedEntity q = pending.poll();
            if (q == null) break;

            if (process(q)) processed++;
        }
    }

    // Returns false if the entity is already gone (that costs nothing, so it doesn't count)
    private static boolean process(QueuedEntity q) {
        if (!q.entity.isAlive()) return false;

        try {
            if (q.entity instanceof ItemEntity item) {
                mergeNearbyItems(item, q);
            } else if (q.entity instanceof ExperienceOrb orb) {
                mergeNearbyOrbs(orb, q.world);
            }
        } catch (Exception ex) {
            Logger(2, String.format(ERROR_MERGING, ex));
        }
        return true;
    }

    // ----------------------------
    // Items
    // ----------------------------

    // Merges in place: items move into the existing stacks and the emptied entities are removed.
    // Like vanilla: smaller stacks go into bigger ones, and a stack that gets items takes the younger age.
    private static void mergeNearbyItems(ItemEntity source, QueuedEntity q) {
        ItemStack sourceStack = source.getItem();
        if (sourceStack.isEmpty()) return;

        ItemEntityAccessor sourceAccess = access(source);
        int delay = sourceAccess.simpleclumps$getPickupDelay();
        int age = sourceAccess.simpleclumps$getAge();
        if (delay == NEVER_PICK_UP || age == UNLIMITED_LIFETIME || age >= LIFETIME) return;

        // Not while it can't be picked up (a fresh drop, an item thrown by a player): check again when it can
        if (delay > 0) {
            if (q.tries < MAX_RETRIES) {
                waiting.add(new QueuedEntity(source, q.world, currentTick + delay, q.tries + 1));
            }
            return;
        }

        UUID target = sourceAccess.simpleclumps$getTarget();
        List<ItemEntity> group = new ArrayList<>(q.world.getEntitiesOfClass(ItemEntity.class, boxAround(source),
                other -> canMerge(other, target) && ItemStack.isSameItemSameComponents(other.getItem(), sourceStack)));

        // Fullest stacks first
        group.sort(Comparator.comparingInt((ItemEntity e) -> e.getItem().getCount()).reversed());

        int maxStack = sourceStack.getMaxStackSize();
        int size = group.size();
        int[] counts = new int[size];
        int[] ages = new int[size];
        for (int i = 0; i < size; i++) {
            counts[i] = group.get(i).getItem().getCount();
            ages[i] = access(group.get(i)).simpleclumps$getAge();
        }
        int[] countsBefore = counts.clone();
        int[] agesBefore = ages.clone();

        int into = -1; // the stack that is being filled
        for (int from = 0; from < size; from++) {
            if (counts[from] >= maxStack) continue; // full stacks are left alone

            if (into < 0) {
                into = from;
                continue;
            }

            int moved = Math.min(maxStack - counts[into], counts[from]);
            counts[into] += moved;
            counts[from] -= moved;
            ages[into] = Math.min(ages[into], ages[from]);

            if (counts[into] >= maxStack) {
                // Full: what is left of this one is filled next
                into = counts[from] > 0 ? from : -1;
            }
        }

        for (int i = 0; i < size; i++) {
            ItemEntity entity = group.get(i);
            if (counts[i] <= 0) {
                entity.discard();
                continue;
            }
            if (counts[i] != countsBefore[i]) {
                entity.setItem(entity.getItem().copyWithCount(counts[i]));
            }
            if (ages[i] != agesBefore[i]) {
                access(entity).simpleclumps$setAge(ages[i]);
            }
            setLabel(entity);
        }
    }

    // Vanilla's rules: can be picked up now, doesn't live forever, isn't about to despawn,
    // and is for the same player (or for anyone)
    private static boolean canMerge(ItemEntity item, UUID target) {
        ItemEntityAccessor itemAccess = access(item);
        int age = itemAccess.simpleclumps$getAge();
        return item.isAlive()
                && !item.getItem().isEmpty()
                && itemAccess.simpleclumps$getPickupDelay() == 0
                && age != UNLIMITED_LIFETIME
                && age < LIFETIME
                && Objects.equals(itemAccess.simpleclumps$getTarget(), target);
    }

    private static void setLabel(ItemEntity entity) {
        applyLabel(entity);
        labels.put(entity, entity.getItem().getCount());
    }

    // Shows the stack size above the item, e.g. "x64 Oak Log"
    private static void applyLabel(ItemEntity entity) {
        ItemStack stack = entity.getItem();
        Component label = Component.literal("x" + stack.getCount())
                .withStyle(ChatFormatting.GREEN)
                .append(Component.literal(" " + stack.getHoverName().getString())
                        .withStyle(ChatFormatting.WHITE));

        entity.setCustomName(label);
        entity.setCustomNameVisible(true);
    }

    // Updates labels whose stack changed since (part of it was picked up), and forgets removed items
    private static void refreshLabels() {
        Iterator<Map.Entry<ItemEntity, Integer>> it = labels.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<ItemEntity, Integer> entry = it.next();
            ItemEntity entity = entry.getKey();
            if (!entity.isAlive()) {
                it.remove();
                continue;
            }

            int count = entity.getItem().getCount();
            if (count > 0 && count != entry.getValue()) {
                applyLabel(entity);
                entry.setValue(count);
            }
        }
    }

    // ----------------------------
    // XP orbs
    // ----------------------------
    private static void mergeNearbyOrbs(ExperienceOrb source, ServerLevel world) {
        List<ExperienceOrb> list = world.getEntitiesOfClass(ExperienceOrb.class, boxAround(source), ExperienceOrb::isAlive);
        if (list.size() <= 1) return;

        // Each orb is worth its value x count (vanilla stacks orbs of the same value)
        long totalXp = 0;
        int minAge = Integer.MAX_VALUE;
        for (ExperienceOrb orb : list) {
            ExperienceOrbAccessor orbAccess = access(orb);
            totalXp += (long) orb.getValue() * orbAccess.simpleclumps$getCount();
            minAge = Math.min(minAge, orbAccess.simpleclumps$getAge());
        }

        // One orb per size, stacked like vanilla does: e.g. 5 x 2477 is a single entity
        List<int[]> newOrbs = new ArrayList<>(); // {value, count}
        long left = totalXp;
        for (int size : ORB_SIZES) {
            long count = left / size;
            if (count > Integer.MAX_VALUE) return; // far too much XP to count: leave it alone
            if (count > 0) {
                newOrbs.add(new int[] {size, (int) count});
                left -= count * size;
            }
        }

        // Only when it leaves fewer orbs. The new orbs are checked again when they spawn,
        // and without this they would be removed and spawned again and again.
        if (newOrbs.size() >= list.size()) return;

        Vec3 spawnPos = source.position();
        Vec3 velocity = source.getDeltaMovement();

        for (ExperienceOrb orb : list) {
            orb.discard();
        }

        for (int[] newOrb : newOrbs) {
            ExperienceOrb orb = new ExperienceOrb(world, spawnPos.x, spawnPos.y, spawnPos.z, newOrb[0]);
            ExperienceOrbAccessor orbAccess = access(orb);
            orbAccess.simpleclumps$setCount(newOrb[1]);
            orbAccess.simpleclumps$setAge(minAge); // like vanilla: the merged orb takes the younger age
            orb.setDeltaMovement(velocity);

            world.addFreshEntity(orb);
        }
    }

    // The mixin adds these interfaces to every ItemEntity and ExperienceOrb when the game loads.
    // (IntelliJ doesn't know that, and warns when it sees the cast right after "new ExperienceOrb")
    private static ItemEntityAccessor access(ItemEntity item) {
        return (ItemEntityAccessor) item;
    }

    private static ExperienceOrbAccessor access(ExperienceOrb orb) {
        return (ExperienceOrbAccessor) orb;
    }

    private static AABB boxAround(Entity entity) {
        Vec3 pos = entity.position();
        return new AABB(
                pos.x - radius, pos.y - radius, pos.z - radius,
                pos.x + radius, pos.y + radius, pos.z + radius
        );
    }

    // ----------------------------
    // Scheduled cleanup
    // ----------------------------
    private static void tickCleanup(MinecraftServer server) {
        if (cleanIntervalTicks <= 0) return; // turned off in the config

        ticksUntilClean--;

        if (!countdownAnnounced30s && ticksUntilClean <= 30 * 20 && ticksUntilClean > 5 * 20) {
            ServerBroadcast(server, CLEANING_DROPS);
            countdownAnnounced30s = true;
        }

        if (ticksUntilClean <= 5 * 20 && ticksUntilClean > 0) {
            if (ticksUntilClean % 20 == 0) {
                long sec = ticksUntilClean / 20;
                ServerBroadcast(server, String.format(CLEANING_DROPS_SCHEDULE, sec));
            }
        }

        if (ticksUntilClean <= 0) {
            long removed = performCleanup(server);
            ServerBroadcast(server, String.format(CLEANING_DROPS_CONFIRM, removed));

            ticksUntilClean = cleanIntervalTicks;
            countdownAnnounced30s = false;
        }
    }

    private static long performCleanup(MinecraftServer server) {
        long removed = 0;

        for (ServerLevel world : server.getAllLevels()) {
            // Every loaded drop in the dimension, at any height
            for (ItemEntity e : world.getEntities(EntityTypeTest.forClass(ItemEntity.class), ItemEntity::isAlive)) {
                e.discard();
                removed++;
            }

            for (ExperienceOrb orb : world.getEntities(EntityTypeTest.forClass(ExperienceOrb.class), ExperienceOrb::isAlive)) {
                orb.discard();
                removed++;
            }
        }

        // Everything that was queued or labeled has just been removed
        pending.clear();
        waiting.clear();
        labels.clear();

        return removed;
    }

    private record QueuedEntity(Entity entity, ServerLevel world, long readyAt, int tries) {}
}
