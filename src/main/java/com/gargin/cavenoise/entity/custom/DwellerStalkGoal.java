package com.gargin.cavenoise.entity.custom;

import java.util.Random;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;

public class DwellerStalkGoal extends Goal {
    private final CaveDwellerEntity cavedweller;
    private double speedModifier;
    private int minTicksTillFlip = 400;
    private int maxTicksTillFlip = 600;
    private int ticksTillFlip;
    private int flipClock = 0;
    private Path path;
    private double pathedTargetX;
    private double pathedTargetY;
    private double pathedTargetZ;
    private int ticksUntilNextPathRecalculation;
    private int ticksUntilNextAttack;
    private int failedPathFindingPenalty = 0;
    private boolean followingTargetEvenIfNotSeen = true;
    private boolean canPenalize = true;
    private float distanceForAggro = 15.0F;
    private Player stalkingTarget;
    Random rand = new Random();

    public DwellerStalkGoal(CaveDwellerEntity pCaveDweller, double pSpeedModifier, float pDistanceForAggro) {
        this.distanceForAggro = pDistanceForAggro;
        this.cavedweller = pCaveDweller;
        this.speedModifier = pSpeedModifier;
    }

    private Player getTargetToStalk() {
        return this.cavedweller.level().getNearestPlayer(this.cavedweller, 200.0D);
    }

    @Override
    public boolean canUse() {
        if (this.cavedweller.isRemoved()) {
            return false;
        } else {
            if (this.cavedweller.getTarget() == null) {
                this.stalkingTarget = this.getTargetToStalk();
            } else {
                this.stalkingTarget = this.getTargetToStalk();
            }

            if (this.stalkingTarget == null || this.stalkingTarget.isSpectator() || this.stalkingTarget.isCreative()) {
                return false;
            } else {
                return this.cavedweller.rRollResult == 3 || this.cavedweller.forcedStalk;
            }
        }
    }

    @Override
    public boolean canContinueToUse() {
        if (this.cavedweller.isRemoved()) {
            return false;
        } else if (this.stalkingTarget == null) {
            return false;
        } else {
            return this.cavedweller.rRollResult == 3 || this.cavedweller.forcedStalk;
        }
    }

    public void switchToAggroIfPlayerInRange() {
        if (this.stalkingTarget.distanceToSqr(this.cavedweller) < this.distanceForAggro && this.cavedweller.inPlayerLineOfSight() && this.cavedweller.isPlayerLookingTowards()) {
            this.cavedweller.rRollResult = 0;
            this.cavedweller.forcedStalk = false;
        }
    }

    @Override
    public void start() {
        SynchedEntityData var10000 = this.cavedweller.getEntityData();
        CaveDwellerEntity var10001 = this.cavedweller;
        var10000.set(CaveDwellerEntity.STALKING_ACCESSOR, true);
        this.ticksTillFlip = this.minTicksTillFlip + this.rand.nextInt(this.maxTicksTillFlip - this.minTicksTillFlip);
        super.start();
    }

    @Override
    public void stop() {
        SynchedEntityData var10000 = this.cavedweller.getEntityData();
        CaveDwellerEntity var10001 = this.cavedweller;
        var10000.set(CaveDwellerEntity.STALKING_ACCESSOR, false);
        this.cavedweller.getNavigation().stop();
        super.stop();
    }
    @Override
    public void tick() {
        this.switchToAggroIfPlayerInRange();
        LivingEntity livingentity = this.stalkingTarget;
        if (livingentity != null) {
            this.cavedweller.getLookControl().setLookAt(livingentity, 30.0F, 30.0F);
            double d0 = this.cavedweller.distanceToSqr(livingentity);
            this.ticksUntilNextPathRecalculation = Math.max(this.ticksUntilNextPathRecalculation - 1, 0);
            if ((this.followingTargetEvenIfNotSeen || this.cavedweller.getSensing().hasLineOfSight(livingentity)) && this.ticksUntilNextPathRecalculation <= 0 && (this.pathedTargetX == (double)0.0F && this.pathedTargetY == (double)0.0F && this.pathedTargetZ == (double)0.0F || livingentity.distanceToSqr(this.pathedTargetX, this.pathedTargetY, this.pathedTargetZ) >= (double)1.0F || this.cavedweller.getRandom().nextFloat() < 0.05F)) {
                this.pathedTargetX = livingentity.getX();
                this.pathedTargetY = livingentity.getY();
                this.pathedTargetZ = livingentity.getZ();
                this.ticksUntilNextPathRecalculation = 4 + this.cavedweller.getRandom().nextInt(7);
                if (this.canPenalize) {
                    this.ticksUntilNextPathRecalculation += this.failedPathFindingPenalty;
                    if (this.cavedweller.getNavigation().getPath() != null) {
                        Node finalPathPoint = this.cavedweller.getNavigation().getPath().getEndNode();
                        if (finalPathPoint != null && livingentity.distanceToSqr((double)finalPathPoint.x, (double)finalPathPoint.y, (double)finalPathPoint.z) < (double)1.0F) {
                            this.failedPathFindingPenalty = 0;
                        } else {
                            this.failedPathFindingPenalty += 10;
                        }
                    } else {
                        this.failedPathFindingPenalty += 10;
                    }
                }

                if (d0 > (double)1024.0F) {
                    this.ticksUntilNextPathRecalculation += 10;
                } else if (d0 > (double)256.0F) {
                    this.ticksUntilNextPathRecalculation += 5;
                }

                if (!this.cavedweller.getNavigation().moveTo(livingentity, this.speedModifier)) {
                    this.ticksUntilNextPathRecalculation += 15;
                }
                this.ticksUntilNextPathRecalculation = this.adjustedTickDelay(this.ticksUntilNextPathRecalculation);
            }
        }

        if (this.cavedweller.rRollResult == 3) {
            ++this.flipClock;
            if (this.flipClock > this.ticksTillFlip) {
                this.flipToAggroOrFlee();
            }
        }

    }

    private void flipToAggroOrFlee() {
        if (this.rand.nextBoolean()) {
            this.cavedweller.rRollResult = 0;
        } else {
            this.cavedweller.rRollResult = 2;
        }

    }
}
