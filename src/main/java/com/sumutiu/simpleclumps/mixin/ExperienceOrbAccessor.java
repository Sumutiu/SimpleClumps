package com.sumutiu.simpleclumps.mixin;

import net.minecraft.world.entity.ExperienceOrb;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Private fields of ExperienceOrb. Vanilla stacks orbs of the same value into one entity:
 * the orb is worth getValue() x count.
 * Use it by casting: ((ExperienceOrbAccessor) orb)
 */
@Mixin(ExperienceOrb.class)
public interface ExperienceOrbAccessor {

    @Accessor("count")
    int simpleclumps$getCount();

    @Accessor("count")
    void simpleclumps$setCount(int count);

    // Ticks since the orb spawned (despawns at 6000)
    @Accessor("age")
    int simpleclumps$getAge();

    @Accessor("age")
    void simpleclumps$setAge(int age);
}
