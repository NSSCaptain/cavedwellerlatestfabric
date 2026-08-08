package com.gargin.cavenoise.mixin;

import com.gargin.cavenoise.entity.custom.CaveDwellerEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.pathfinder.Path;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Set;

@Mixin(PathNavigation.class)
public abstract class MixinPathNavigation {
    @Unique private boolean cave_dweller$wasSqueezing;
    @Inject(method = "createPath(Ljava/util/Set;IZIF)Lnet/minecraft/world/level/pathfinder/Path;", at = @At("HEAD"))
    public void isCrawling_true(final Set<BlockPos> targets, int regionOffset, boolean offsetUpward, int accuracy, float followRange, final CallbackInfoReturnable<Path> callback) {
        if (mob instanceof CaveDwellerEntity cavedweller) {
            cave_dweller$wasSqueezing = cavedweller.isCrawling();
            cavedweller.setCrawling = true;
        }
    }
    @Inject(method = "createPath(Ljava/util/Set;IZIF)Lnet/minecraft/world/level/pathfinder/Path;", at = @At("RETURN"))
    public void isCrawling_false(final Set<BlockPos> targets, int regionOffset, boolean offsetUpward, int accuracy, float followRange, final CallbackInfoReturnable<Path> callback) {
        if (mob instanceof CaveDwellerEntity cavedweller) {
            cavedweller.setCrawling(cave_dweller$wasSqueezing);
        }
    }

    @Shadow @Final protected Mob mob;
}
