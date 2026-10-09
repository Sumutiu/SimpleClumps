package com.sumutiu.simpleclumps.mixin;

import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.UUID;

/**
 * Private fields of ItemEntity, so items can be merged with vanilla's rules.
 * Use it by casting: ((ItemEntityAccessor) itemEntity)
 */
@Mixin(ItemEntity.class)
public interface ItemEntityAccessor {

    // Ticks since the item was dropped (despawns at 6000; -32768 = never despawns)
    @Accessor("age")
    int simpleclumps$getAge();

    @Accessor("age")
    void simpleclumps$setAge(int age);

    // Ticks until it can be picked up (32767 = never)
    @Accessor("pickupDelay")
    int simpleclumps$getPickupDelay();

    // The only player who can pick it up, or null for anyone (e.g. /give with a full inventory)
    @Accessor("target")
    UUID simpleclumps$getTarget();
}
