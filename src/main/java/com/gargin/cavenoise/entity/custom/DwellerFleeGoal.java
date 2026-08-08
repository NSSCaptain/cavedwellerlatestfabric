package com.gargin.cavenoise.entity.custom;

import java.util.Random;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

public class DwellerFleeGoal extends Goal {
    private final CaveDwellerEntity cavedweller;
    private final float ticksTillLeave;
    private final float ticksTillFlee;
    private float currentTicksTillLeave;
    private float currentTicksTillFlee;
    private boolean shouldLeave;
    private double fleeX;
    private double fleeY;
    private double fleeZ;
    private int ticksUntilNextPathRecalculation;
    private double speedModifier;

    public DwellerFleeGoal(CaveDwellerEntity pCaveDweller, float pTicksTillLeave, double pSpeedModifier) {
        this.cavedweller = pCaveDweller;
        this.ticksTillLeave = pTicksTillLeave;
        this.currentTicksTillLeave = pTicksTillLeave;
        this.ticksTillFlee = 10.0F;
        this.currentTicksTillFlee = this.ticksTillFlee;
        this.speedModifier = pSpeedModifier;
    }

    @Override
    public boolean canUse() {
        if (this.cavedweller.isRemoved()) {
            return false;
        } else if (this.cavedweller.rRollResult == 2 && !this.cavedweller.forcedStalk) {
            return this.cavedweller.getTarget() != null;
        } else {
            return false;
        }
    }

    @Override
    public boolean canContinueToUse() {
        if (this.cavedweller.rRollResult == 2 && !this.cavedweller.forcedStalk) {
            return this.cavedweller.getTarget() != null;
        } else {
            return false;
        }
    }

    @Override
    public void start() {
        this.getSpotToWalk();
        this.cavedweller.spottedByPlayer = false;
        this.shouldLeave = false;
        super.start();
    }

    @Override
    public void stop() {
    }

    public boolean isPlayerLookingTowards() {
        LivingEntity pendingTarget = this.cavedweller.getTarget();
        Minecraft minecraft = Minecraft.getInstance();
        boolean yawPlayerLookingTowards = false;
        float fov = (float) (Integer) minecraft.options.fov().get();
        float yFovMod = 0.65F;
        float fovMod = (35.0F / fov - 1.0F) * 0.4F + 1.0F;
        fov *= fovMod;
        Vec3 a = pendingTarget.position();
        Vec3 b = this.cavedweller.position();
        Vec2 dist = new Vec2((float) b.x - (float) a.x, (float) b.z - (float) a.z);
        dist = dist.normalized();
        double newAngle = Math.toDegrees(Math.atan2((double) dist.x, (double) dist.y));
        float lookX = (float) pendingTarget.getViewVector(1.0F).x;
        float lookZ = (float) pendingTarget.getViewVector(1.0F).z;
        double newLookAngle = Math.toDegrees(Math.atan2((double) lookX, (double) lookZ));
        double newNewAngle = this.loopAngle(newAngle - newLookAngle) + (double) fov;
        newNewAngle = this.loopAngle(newNewAngle);
        if (newNewAngle > 0.0D && newNewAngle < (double) (fov * 2.0F)) {
            yawPlayerLookingTowards = true;
        }

        boolean pitchPlayerLookingTowards = false;
        boolean shouldOnlyUsePitch = false;
        float yFov = fov * yFovMod;
        Vec2 yDist = new Vec2((float) Math.sqrt((b.x - a.x) * (b.x - a.x) + (b.z - a.z) * (b.z - a.z)), (float) (b.y - a.y));
        yDist = yDist.normalized();
        double yAngle = Math.toDegrees(Math.atan2((double) yDist.x, (double) yDist.y));
        float lookY = (float) pendingTarget.getViewVector(1.0F).y;
        Vec2 lookDist = new Vec2((float) Math.sqrt((double) (lookX * lookX + lookZ * lookZ)), lookY);
        lookDist = lookDist.normalized();
        double yLookAngle = Math.toDegrees(Math.atan2((double) lookDist.x, (double) lookDist.y));
        double newYAngle = this.loopAngle(yAngle - yLookAngle) + (double) yFov;
        newYAngle = this.loopAngle(newYAngle);
        if (newYAngle > (double) 0.0F && newYAngle < (double) (yFov * 2.0F)) {
            pitchPlayerLookingTowards = true;
        }
        if (!(yLookAngle < (double) (180.0F - yFov)) || !(yLookAngle > (double) yFov)) {
            shouldOnlyUsePitch = true;
        }
        return (yawPlayerLookingTowards || shouldOnlyUsePitch) && pitchPlayerLookingTowards;
    }

    public boolean inPlayerLineOfSight() {
        if (this.cavedweller.getTarget() != null)
            return this.cavedweller.getTarget().hasLineOfSight((Entity)this.cavedweller);
        return false;
    }

    public double loopAngle(double angle) {
        if (angle > (double) 360.0F) {
            double var4;
            return var4 = angle - (double) 360.0F;
        } else {
            double var3;
            return angle < (double) 0.0F ? (var3 = angle + (double) 360.0F) : angle;
        }
    }

    private boolean getSpotToWalk() {
        Random rand = new Random();
        double randX = rand.nextDouble() - (double) 0.5F;
        double randY = (double) (rand.nextInt(64) - 32);
        double randZ = rand.nextDouble() - (double) 0.5F;
        if (randX > (double) 0.0F) {
            this.fleeX = this.cavedweller.getX() + (double) 1.0F * (double) 64.0F;
        } else {
            this.fleeX = this.cavedweller.getX() - (double) 1.0F * (double) 64.0F;
        }

        this.fleeY = this.cavedweller.getY() + randY;
        if (randZ > (double) 0.0F) {
            this.fleeZ = this.cavedweller.getZ() + (double) 1.0F * (double) 64.0F;
        } else {
            this.fleeZ = this.cavedweller.getZ() - (double) 1.0F * (double) 64.0F;
        }

        BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos(this.fleeX, this.fleeY, this.fleeZ);

        while (blockpos$mutableblockpos.getY() > this.cavedweller.level().getMinBuildHeight() && !this.cavedweller.level().getBlockState(blockpos$mutableblockpos).isCollisionShapeFullBlock(this.cavedweller.level(), blockpos$mutableblockpos)) {
            blockpos$mutableblockpos.move(Direction.DOWN);
        }

        BlockState blockstate = this.cavedweller.level().getBlockState(blockpos$mutableblockpos);
        boolean flag = blockstate.isCollisionShapeFullBlock(this.cavedweller.level(), blockpos$mutableblockpos);
        boolean flag1 = blockstate.getFluidState().is(net.minecraft.tags.FluidTags.LAVA);
        return flag && !flag1;
    }


    public void tickStareClock() {
        --this.currentTicksTillLeave;
        if (this.currentTicksTillLeave < 0.0F) {
            this.shouldLeave = true;
        }

    }

    void tickFleeClock() {
        --this.currentTicksTillFlee;
    }

    public void fleeTick() {
        this.cavedweller.playFleeSound();
        this.ticksUntilNextPathRecalculation = Math.max(this.ticksUntilNextPathRecalculation - 1, 0);
        if (this.ticksUntilNextPathRecalculation <= 0) {
            this.ticksUntilNextPathRecalculation = 2;
            if (!this.cavedweller.getNavigation().moveTo(this.fleeX, this.fleeY, this.fleeZ, this.speedModifier)) {
                this.ticksUntilNextPathRecalculation += 2;
            }
            this.ticksUntilNextPathRecalculation = this.adjustedTickDelay(this.ticksUntilNextPathRecalculation);
        }
    }

    @Override
    public void tick() {
        if (this.shouldLeave && (!this.isPlayerLookingTowards() || !this.inPlayerLineOfSight())) {
            this.cavedweller.discard();
        }

        this.tickFleeClock();
        this.tickStareClock();
        if ((float) this.currentTicksTillFlee <= 0.0F) {
            this.fleeTick();
            this.cavedweller.isFleeing = true;
            SynchedEntityData var10000 = this.cavedweller.getEntityData();
            CaveDwellerEntity var10001 = this.cavedweller;
            var10000.set(CaveDwellerEntity.FLEEING_ACCESSOR, true);
        } else {
            this.cavedweller.getLookControl().setLookAt(this.cavedweller.getTarget(), 180.0F, 1.0F);
        }
    }
}

