package com.gargin.cavenoise.entity.custom;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

public class DwellerStareGoal extends Goal {
    private final CaveDwellerEntity cavedweller;
    private final float ticksTillLeave;
    private float currentTicksTillLeave;
    private boolean shouldLeave;

    public DwellerStareGoal(CaveDwellerEntity pCaveDweller, float pTicksTillLeave) {
        this.cavedweller = pCaveDweller;
        this.ticksTillLeave = pTicksTillLeave;
        this.currentTicksTillLeave = pTicksTillLeave;
    }

    @Override
    public boolean canUse() {
        if (this.cavedweller.isRemoved()) {
            return false;
        } else if (this.cavedweller.getTarget() == null) {
            return false;
        } else {
            return this.cavedweller.rRollResult == 1 && !this.cavedweller.forcedStalk;
        }
    }

    @Override
    public boolean canContinueToUse() {
        if (this.cavedweller.getTarget() == null) {
            return false;
        } else {
            return this.cavedweller.rRollResult == 1 && !this.cavedweller.forcedStalk;
        }
    }

    @Override
    public void start() {
        this.shouldLeave = false;
        super.start();
    }

    @Override
    public void stop() {
    }

    public void tickStareClock() {
        --this.currentTicksTillLeave;
        if (this.currentTicksTillLeave <= 0.0F) {
            this.shouldLeave = true;
        }
    }


    public boolean isPlayerLookingTowards() {
        LivingEntity pendingTarget = this.cavedweller.getTarget();
        Minecraft minecraft = Minecraft.getInstance();
        boolean yawPlayerLookingTowards = false;
        float fov = (float)minecraft.options.fov().get();
        float yFovMod = 0.65F;
        float fovMod = (35.0F / fov - 1.0F) * 0.4F + 1.0F;
        fov *= fovMod;
        Vec3 a = pendingTarget.position();
        Vec3 b = this.cavedweller.position();
        Vec2 dist = new Vec2((float)b.x - (float)a.x, (float)b.z - (float)a.z);
        dist = dist.normalized();
        double newAngle = Math.toDegrees(Math.atan2(dist.x, dist.y));
        float lookX = (float)pendingTarget.getViewVector(1.0F).x;
        float lookZ = (float)pendingTarget.getViewVector(1.0F).z;
        double newLookAngle = Math.toDegrees(Math.atan2(lookX, lookZ));
        double newNewAngle = this.loopAngle(newAngle - newLookAngle) + (double)fov;
        newNewAngle = this.loopAngle(newNewAngle);
        if (newNewAngle > 0.0D && newNewAngle < (double)(fov * 2.0F)) {
            yawPlayerLookingTowards = true;
        }

        boolean pitchPlayerLookingTowards = false;
        boolean shouldOnlyUsePitch = false;
        float yFov = fov * yFovMod;
        Vec2 yDist = new Vec2((float)Math.sqrt((b.x - a.x) * (b.x - a.x) + (b.z - a.z) * (b.z - a.z)), (float)(b.y - a.y));
        yDist = yDist.normalized();
        double yAngle = Math.toDegrees(Math.atan2(yDist.x, yDist.y));
        float lookY = (float)pendingTarget.getViewVector(1.0F).y;
        Vec2 lookDist = new Vec2((float)Math.sqrt((lookX * lookX + lookZ * lookZ)), lookY);
        lookDist = lookDist.normalized();
        double yLookAngle = Math.toDegrees(Math.atan2(lookDist.x, lookDist.y));
        double newYAngle = this.loopAngle(yAngle - yLookAngle) + (double)yFov;
        newYAngle = this.loopAngle(newYAngle);
        if (newYAngle > 0.0D && newYAngle < (double)(yFov * 2.0F)) {
            pitchPlayerLookingTowards = true;
        }

        if (!(yLookAngle < (double)(180.0F - yFov)) || !(yLookAngle > (double)yFov)) {
            shouldOnlyUsePitch = true;
        }

        return (yawPlayerLookingTowards || shouldOnlyUsePitch) && pitchPlayerLookingTowards;
    }

    public boolean inPlayerLineOfSight() {
        LivingEntity pendingTarget = this.cavedweller.getTarget();
        return pendingTarget != null ? pendingTarget.hasLineOfSight(this.cavedweller) : false;
    }

    public double loopAngle(double angle) {
        if (angle > (double)360.0F) {
            double var4;
            return var4 = angle - (double)360.0F;
        } else {
            double var3;
            return angle < (double)0.0F ? (var3 = angle + (double)360.0F) : angle;
        }
    }

    @Override
    public void tick() {
        this.tickStareClock();
        if (this.shouldLeave && (!this.isPlayerLookingTowards() || !this.inPlayerLineOfSight())) {
            this.cavedweller.playDisappearSound();
            this.cavedweller.discard();
        }

        this.cavedweller.getLookControl().setLookAt(this.cavedweller.getTarget(), 180.0F, 1.0F);
    }
}
