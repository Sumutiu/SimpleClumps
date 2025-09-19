package com.sumutiu.simpleclumps;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Box;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MobMergeManager {

    private static final int CLUMP_RADIUS = 5;
    private static final Set<EntityType<?>> MERGEABLE_HOSTILE_MOBS = new HashSet<>();
    private static final Set<EntityType<?>> MERGEABLE_ANIMALS = new HashSet<>();

    public static void init() {
        // Hostile Mobs
        MERGEABLE_HOSTILE_MOBS.add(EntityType.ZOMBIE);
        MERGEABLE_HOSTILE_MOBS.add(EntityType.SKELETON);
        MERGEABLE_HOSTILE_MOBS.add(EntityType.SPIDER);
        MERGEABLE_HOSTILE_MOBS.add(EntityType.CREEPER);
        MERGEABLE_HOSTILE_MOBS.add(EntityType.SLIME);
        MERGEABLE_HOSTILE_MOBS.add(EntityType.ENDERMAN);
        MERGEABLE_HOSTILE_MOBS.add(EntityType.WITCH);
        MERGEABLE_HOSTILE_MOBS.add(EntityType.PILLAGER);

        // Animals
        MERGEABLE_ANIMALS.add(EntityType.COW);
        MERGEABLE_ANIMALS.add(EntityType.PIG);
        MERGEABLE_ANIMALS.add(EntityType.SHEEP);
        MERGEABLE_ANIMALS.add(EntityType.CHICKEN);
        MERGEABLE_ANIMALS.add(EntityType.RABBIT);
        MERGEABLE_ANIMALS.add(EntityType.SQUID);
        MERGEABLE_ANIMALS.add(EntityType.COD);
        MERGEABLE_ANIMALS.add(EntityType.SALMON);
        MERGEABLE_ANIMALS.add(EntityType.TROPICAL_FISH);
        MERGEABLE_ANIMALS.add(EntityType.TURTLE);
    }

    public static void onEntityLoad(Entity entity, ServerWorld world) {
        if (!(entity instanceof LivingEntity)) return;

        LivingEntity source = (LivingEntity) entity;
        EntityType<?> type = source.getType();

        if (!MERGEABLE_HOSTILE_MOBS.contains(type) && !MERGEABLE_ANIMALS.contains(type)) {
            return;
        }

        MergedMob sourceMob = (MergedMob) source;
        if (sourceMob.getStackCount() > 1 || source.hasCustomName()) {
            return;
        }

        Box box = new Box(source.getPos().x - CLUMP_RADIUS, source.getPos().y - CLUMP_RADIUS, source.getPos().z - CLUMP_RADIUS,
                source.getPos().x + CLUMP_RADIUS, source.getPos().y + CLUMP_RADIUS, source.getPos().z + CLUMP_RADIUS);

        List<LivingEntity> list = world.getEntitiesByClass(LivingEntity.class, box, e -> e.getType() == type && e.isAlive() && e != source);

        if (list.isEmpty()) return;

        int currentStack = sourceMob.getStackCount();

        for (LivingEntity nearbyEntity : list) {
            MergedMob nearbyMob = (MergedMob) nearbyEntity;
            currentStack += nearbyMob.getStackCount();
            nearbyEntity.discard();
        }

        if (currentStack > 1) {
            sourceMob.setStackCount(currentStack);
            String mobName = source.getType().getName().getString();
            Text label = Text.literal("x" + currentStack).formatted(Formatting.GREEN)
                    .append(Text.literal(" " + mobName).formatted(Formatting.WHITE));
            source.setCustomName(label);
            source.setCustomNameVisible(true);
        }
    }
}
