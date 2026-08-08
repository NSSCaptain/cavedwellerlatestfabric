package com.gargin.cavenoise.mixin;

import com.gargin.cavenoise.entity.custom.CaveDwellerEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class MixinLivingEntity {
    // Currently needed to prevent the mob from sliding to its previously set target location
    @Inject(method = "travel", at = @At("HEAD"), cancellable = true)
    public void pleaseStopMoving(final Vec3 travelVector, final CallbackInfo callback) {
        if ((Object) this instanceof CaveDwellerEntity cavedweller) {
            if (cavedweller.pleaseStopMoving) {
                callback.cancel();
            }
        }
    }
}