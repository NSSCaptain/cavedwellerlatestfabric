package com.thecaptain.cavedweller.entities.goals;

import java.util.EnumSet;

import com.thecaptain.cavedweller.CaveDweller;
import com.thecaptain.cavedweller.entities.CaveDwellerEntity;
import com.thecaptain.cavedweller.util.Utils;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.pathfinder.Path;

public class DwellerStalkGoal extends Goal {
    private final CaveDwellerEntity caveDweller;
    private double movementSpeed = CaveDweller.CONFIG.MOVEMENT_SPEED();
    private double stalkSpeedMultiplier = 0.7;
    private int ticksTillFlip;
    private int flipClock = 0;
    private double pathedTargetX;
    private double pathedTargetY;
    private double pathedTargetZ;
    private int ticksUntilNextPathRecalculation;
    private int failedPathFindingPenalty = 0;
    private final float distanceThreshold;
    private Player target;
    private boolean followTargetEvenIfNotSeen;
    private boolean canPenalize = true;
    private int spottingRange = CaveDweller.CONFIG.SPOTTING_RANGE();

    public DwellerStalkGoal(CaveDwellerEntity caveDweller, float distanceThreshold, boolean followTargetEvenIfNotSeen) {
        this.caveDweller = caveDweller;
        this.distanceThreshold = distanceThreshold;
        this.followTargetEvenIfNotSeen = followTargetEvenIfNotSeen;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    private Player getTargetToStalk() {
        return this.caveDweller.level().getNearestPlayer(this.caveDweller.getX(), this.caveDweller.getY(), this.caveDweller.getZ(), CaveDweller.CONFIG.SPOTTING_RANGE(), Utils::isValidPlayer);
    }

    @Override
    public boolean canUse() {
        if (this.caveDweller.isRemoved() || this.caveDweller.isInvisible()) {
            return false;
        }

        if (this.caveDweller.getTarget() == null) {
            this.target = this.getTargetToStalk();
        } else if (this.caveDweller.getTarget() instanceof Player target) {
            this.target = target;
        }
        if (!Utils.isValidPlayer(this.target)) {
            return false;
        }

        if (this.caveDweller.isInStandoff()) {
            return false;
        }

        return this.caveDweller.stalking;
    }

    @Override
    public boolean canContinueToUse() {
        if (this.caveDweller.isRemoved() || this.caveDweller.isInvisible()) {
            return false;
        }
        if (!Utils.isValidPlayer(target)) {
            if (CaveDweller.CONFIG.DISAPPEAR()) {
                this.caveDweller.disappear();
            }
            return false;
        }

        return this.caveDweller.stalking;
    }

    public void switchToAggroIfPlayerInRange() {
        if (!Utils.isValidPlayer(this.target)) {
            return;
        }

        // Might be unused/redundant as a result of distanceThreshold in DwellerTargetTooCloseGoal
        boolean isCloseEnough = this.target.distanceTo(this.caveDweller) < this.distanceThreshold && this.caveDweller.inPlayerLineOfSight() && this.caveDweller.isPlayerLookingTowards(this.target);

        if (isCloseEnough) {
            this.caveDweller.currentRoll = Roll.CHASE;
            this.caveDweller.getEntityData().set(CaveDwellerEntity.AGGRO_ACCESSOR, true);
            this.stop();
        }
    }

    @Override
    public void start() {
        super.start();
        this.caveDweller.setNoGravity(false);
        this.caveDweller.noPhysics = false;

        this.caveDweller.pleaseStopMoving = false;
        this.caveDweller.getEntityData().set(CaveDwellerEntity.STALKING_ACCESSOR, true);
        this.caveDweller.refreshDimensions();
        this.flipClock = 0;
        int minTicksTillFlip = 400;
        int maxTicksTillFlip = 600;
        this.ticksTillFlip = minTicksTillFlip + this.caveDweller.getRandom().nextInt(maxTicksTillFlip - minTicksTillFlip);
    }

    @Override
    public void stop() {
        this.caveDweller.stalking = false;
        this.caveDweller.getEntityData().set(CaveDwellerEntity.STALKING_ACCESSOR, false);
        this.caveDweller.getNavigation().stop();
        this.target = null;
        super.stop();
    }


    @Override
    public void tick() {
        this.switchToAggroIfPlayerInRange();

        LivingEntity target = this.target;
        if (target != null) {

            Path path = this.caveDweller.getNavigation().getPath();
            if (path == null || path.isDone() || path.getEndNode() == null || path.getEndNode().asBlockPos().distSqr(target.blockPosition()) > 0.25D) {
                path = this.caveDweller.getNavigation().createPath(target, 1);
            }

            this.caveDweller.getNavigation().moveTo(path, this.stalkSpeedMultiplier);

            double distanceToTarget = this.getAttackReachSqr(target);
            this.ticksUntilNextPathRecalculation = Math.max(this.ticksUntilNextPathRecalculation - 1, 0);
            if ((this.followTargetEvenIfNotSeen
                    || this.caveDweller.getSensing().hasLineOfSight(target) && this.ticksUntilNextPathRecalculation <= 0 && (this.pathedTargetX == (double) 0.0F && this.pathedTargetY == (double) 0.0F && this.pathedTargetZ == (double) 0.0F
                    || target.distanceToSqr(this.pathedTargetX, this.pathedTargetY, this.pathedTargetZ) >= 1.0D
                    || this.caveDweller.getRandom().nextFloat() < 0.05F))) {
                this.pathedTargetX = target.getX();
                this.pathedTargetY = target.getY();
                this.pathedTargetZ = target.getZ();
                this.ticksUntilNextPathRecalculation = 2;
                if (this.canPenalize) {
                    this.ticksUntilNextPathRecalculation += this.failedPathFindingPenalty;
                    if (this.caveDweller.getNavigation().getPath() != null) {
                        net.minecraft.world.level.pathfinder.Node finalPathPoint = this.caveDweller.getNavigation().getPath().getEndNode();
                        if (finalPathPoint != null && target.distanceToSqr((double) finalPathPoint.x, (double) finalPathPoint.y, (double) finalPathPoint.z) < (double) 1.0F) {
                            this.failedPathFindingPenalty = 0;
                        } else {
                            this.failedPathFindingPenalty += 10;
                        }
                    } else {
                        this.failedPathFindingPenalty += 10;
                    }
                }

                this.caveDweller.getShortPath(target);
                // If there is a valid path to the player...
                if (this.caveDweller.shortPath != null) {
                    net.minecraft.world.level.pathfinder.Node finalShortPathPoint = this.caveDweller.shortPath.getEndNode();
                    // ...and its final destination node safely drops the entity within 2 blocks of the player (< 2.0D)...
                    if (finalShortPathPoint != null && target.distanceToSqr((double) finalShortPathPoint.x, (double) finalShortPathPoint.y, (double) finalShortPathPoint.z) < 2.0D) {
                        this.caveDweller.shortPathAvailable = true;
                    // if its final destination node does not safely drop the entity within 2 blocks of the player (< 2.0D)...
                    } else {
                        this.caveDweller.shortPathAvailable = false;
                    }
                // If there is not a valid path to the player...
                } else {
                    this.caveDweller.shortPathAvailable = false;
                }

                this.caveDweller.shouldUseShortPath = this.caveDweller.shortPathAvailable;

                // Reduce frequency of path recalculation if far away
                if (distanceToTarget > (double) 2034.0F) {
                    this.ticksUntilNextPathRecalculation += 10;
                } else if (distanceToTarget > (double) 512.0F) {
                    this.ticksUntilNextPathRecalculation += 5;
                }

                // If dweller shouldn't use a short path...
                if (!this.caveDweller.shouldUseShortPath) {
                    // ...and if Minecraft's pathfinding navigation system failed to initiate or find a valid path...
                    if (!this.caveDweller.getNavigation().moveTo(target, this.stalkSpeedMultiplier)) {
                        this.caveDweller.startedMovingChase = true;
                        this.ticksUntilNextPathRecalculation += 8;
                    }
                    // If dweller should use a short path...
                } else if (this.caveDweller.shouldUseShortPath) {
                    // ...and if dweller's navigation engine failed to execute or follow a pre-calculated short path...
                    if (!this.caveDweller.getNavigation().moveTo(this.caveDweller.shortPath, this.stalkSpeedMultiplier)) {
                        this.caveDweller.startedMovingChase = true;
                        this.ticksUntilNextPathRecalculation += 8;
                    }
                }

                this.ticksUntilNextPathRecalculation = this.adjustedTickDelay(this.ticksUntilNextPathRecalculation);
            }

            if (this.caveDweller.stalking) {
                ++this.flipClock;
                if (this.flipClock > this.ticksTillFlip) {
                    this.flipToAggro();
                }
            }
        }
    }


    private void flipToAggro() {
        if (this.caveDweller.getRandom().nextBoolean()) {
            this.caveDweller.stalking = false;
            this.caveDweller.currentRoll = Roll.CHASE;
            this.caveDweller.getEntityData().set(CaveDwellerEntity.AGGRO_ACCESSOR, true);
        }
    }

    protected double getAttackReachSqr(LivingEntity pAttackTarget) {
        return (double) (this.caveDweller.getBbWidth() * 4.0F * this.caveDweller.getBbWidth() * 4.0F + pAttackTarget.getBbWidth());
    }
}
