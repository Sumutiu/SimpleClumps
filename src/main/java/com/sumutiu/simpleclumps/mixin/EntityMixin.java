package com.sumutiu.simpleclumps.mixin;

import com.sumutiu.simpleclumps.MergedMob;
import net.minecraft.entity.Entity;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.nbt.NbtCompound;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class EntityMixin implements MergedMob {

    @Shadow
    protected DataTracker dataTracker;

    private static final TrackedData<Integer> STACK_COUNT = DataTracker.registerData(Entity.class, TrackedDataHandlerRegistry.INTEGER);

    @Inject(method = "initDataTracker", at = @At("TAIL"))
    private void initDataTracker(CallbackInfo ci) {
        this.dataTracker.set(STACK_COUNT, 1);
    }

    @Override
    public int simpleclumps_getStackCount() {
        return this.dataTracker.get(STACK_COUNT);
    }

    @Override
    public void simpleclumps_setStackCount(int count) {
        this.dataTracker.set(STACK_COUNT, count);
    }

    @Inject(method = "writeNbt", at = @At("HEAD"))
    private void writeNbt(NbtCompound nbt, CallbackInfo ci) {
        nbt.putInt("StackCount", simpleclumps_getStackCount());
    }

    @Inject(method = "readNbt", at = @At("HEAD"))
    private void readNbt(NbtCompound nbt, CallbackInfo ci) {
        if (nbt.contains("StackCount")) {
            simpleclumps_setStackCount(nbt.getInt("StackCount").orElse(1));
        }
    }
}
