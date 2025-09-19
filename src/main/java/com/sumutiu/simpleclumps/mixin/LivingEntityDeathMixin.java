package com.sumutiu.simpleclumps.mixin;

import com.sumutiu.simpleclumps.MergedMob;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityDeathMixin {

    @Inject(method = "onDeath", at = @At("HEAD"), cancellable = true)
    private void onDeath(DamageSource damageSource, CallbackInfo ci) {
        LivingEntity thisEntity = (LivingEntity) (Object) this;
        if (thisEntity instanceof PlayerEntity) return;

        MergedMob mergedMob = (MergedMob) thisEntity;
        if (mergedMob.simpleclumps_getStackCount() > 1) {
            int newCount = mergedMob.simpleclumps_getStackCount() - 1;
            mergedMob.simpleclumps_setStackCount(newCount);
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
