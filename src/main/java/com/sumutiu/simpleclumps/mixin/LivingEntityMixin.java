package com.sumutiu.simpleclumps.mixin;

import com.sumutiu.simpleclumps.MergedMob;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.entity.player.PlayerEntity;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends net.minecraft.entity.Entity implements MergedMob {

    private static final TrackedData<Integer> STACK_COUNT = DataTracker.registerData(LivingEntity.class, TrackedDataHandlerRegistry.INTEGER);

    public LivingEntityMixin(EntityType<?> type, World world) {
        super(type, world);
    }

    @Inject(method = "initDataTracker", at = @At("TAIL"))
    private void initDataTracker(CallbackInfo ci) {
        ((DataTracker) (Object) this.dataTracker).set(STACK_COUNT, 1);
    }

    @Override
    public int getStackCount() {
        return this.dataTracker.get(STACK_COUNT);
    }

    @Override
    public void setStackCount(int count) {
        this.dataTracker.set(STACK_COUNT, count);
    }

    @Inject(method = "writeNbt", at = @At("HEAD"))
    private void writeNbt(NbtCompound nbt, CallbackInfo ci) {
        nbt.putInt("StackCount", getStackCount());
    }

    @Inject(method = "readNbt", at = @At("HEAD"))
    private void readNbt(NbtCompound nbt, CallbackInfo ci) {
        if (nbt.contains("StackCount")) {
            setStackCount(nbt.getInt("StackCount").orElse(1));
        }
    }

    @Inject(method = "onDeath", at = @At("HEAD"), cancellable = true)
    private void onDeath(DamageSource damageSource, CallbackInfo ci) {
        LivingEntity thisEntity = (LivingEntity) (Object) this;
        if (thisEntity instanceof PlayerEntity) return;

        MergedMob mergedMob = (MergedMob) thisEntity;
        if (mergedMob.getStackCount() > 1) {
            int newCount = mergedMob.getStackCount() - 1;
            mergedMob.setStackCount(newCount);
            thisEntity.setHealth(thisEntity.getMaxHealth());

            if (newCount > 1) {
                String mobName = thisEntity.getType().getName().getString();
                Text label = Text.literal("x" + newCount).formatted(Formatting.GREEN)
                        .append(Text.literal(" " + mobName).formatted(Formatting.WHITE));
                thisEntity.setCustomName(label);
                thisEntity.setCustomNameVisible(true);
            } else {
                thisEntity.setCustomName(null);
                thisEntity.setCustomNameVisible(false);
            }

            ci.cancel();
        }
    }
}
