package com.thecaptain.cavedweller.mixin;

import com.thecaptain.cavedweller.entities.CaveDwellerEntity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public class MixinLivingEntity {
    public MixinLivingEntity() {
    }

    @Inject(
            method = "travel",
            at = @At("HEAD"),
            cancellable = true
    )
    public void pleaseStopMoving(Vec3 travelVector, CallbackInfo callback) {
        LivingEntity livingEntity = (LivingEntity) (Object) this;
        if (livingEntity instanceof CaveDwellerEntity cavedweller) {
            if (cavedweller.pleaseStopMoving) {
                callback.cancel();
            }
        }
    }

    // Armor piercing
    @org.spongepowered.asm.mixin.injection.Inject(
            method = "getDamageAfterArmorAbsorb",
            at = @At("HEAD"),
            cancellable = true
    )
    private void bypassCaveDwellerArmorAbsorb(DamageSource source, float amount, org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<Float> cir) {
        if (source.getEntity() instanceof CaveDwellerEntity) {
            cir.setReturnValue(amount);
        }
    }


}
