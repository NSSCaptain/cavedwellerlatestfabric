package com.thecaptain.cavedweller.entities.goals;

import com.thecaptain.cavedweller.entities.CaveDwellerEntity;
import com.thecaptain.cavedweller.util.Utils;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

public class DwellerStareGoal extends Goal {
    private final CaveDwellerEntity caveDweller;
    private boolean wereNotLooking;
    private int lookedAtCount;
    private int decisionCooldown = 0;
    private boolean canStandoffOrStalk = false;

    public DwellerStareGoal(CaveDwellerEntity caveDweller) {
        this.caveDweller = caveDweller;
    }

    @Override
    public boolean canUse() {
        if (this.caveDweller.isInvisible()) {
            return false;
        } else if (!Utils.isValidPlayer(this.caveDweller.getTarget())) {
            return false;
        } else {
            return this.caveDweller.currentRoll == Roll.STARE;
        }
    }

    public void start() {
        super.start();
        this.caveDweller.setNoGravity(false);
        this.caveDweller.noPhysics = false;
        this.caveDweller.setInStandoff(false);
    }

    @Override
    public boolean canContinueToUse() {
        if (!Utils.isValidPlayer(this.caveDweller.getTarget())) {
            return false;
        } else {
            return this.caveDweller.currentRoll == Roll.STARE;
        }
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void stop() {
        super.stop();
        this.lookedAtCount = 2;
        this.decisionCooldown = 0;
        this.wereNotLooking = false;
        this.caveDweller.pleaseStopMoving = false;
        this.caveDweller.getEntityData().set(CaveDwellerEntity.SPOTTED_ACCESSOR, false);
    }

    private boolean shouldAttackBasedOnArmor(LivingEntity target) {
        if (target == null) {
            return false;
        }

        int armorValue = target.getArmorValue();

        double baseChance = 0.05; // 5% chance to attack in general
        double maxChance = 0.80;  // 80% chance to attack a player with max armor points

        double calculatedChance = baseChance + ((double) armorValue / 20.0) * (maxChance - baseChance);
        calculatedChance = Math.min(maxChance, Math.max(baseChance, calculatedChance));

        return this.caveDweller.getRandom().nextDouble() < calculatedChance;
    }

    @Override
    public void tick() {
        if (this.decisionCooldown > 0) {
            --this.decisionCooldown;
        }
        
        LivingEntity target = this.caveDweller.getTarget();
        if (target == null) {
            this.caveDweller.disappear();
            return;
        }

        if (this.caveDweller.isInStandoff()) {
            this.standoffTick();
        }

        boolean areCurrentlyLookingTowardsAndSeeing = this.caveDweller.targetIsLookingAtMe && target.hasLineOfSight(this.caveDweller);
        if (this.wereNotLooking && areCurrentlyLookingTowardsAndSeeing) {
            ++this.lookedAtCount;
        }

        if (this.caveDweller.getRandom().nextFloat() < 0.001F && !(this.decisionCooldown <= 0) && !this.caveDweller.isInStandoff()) {
            if (this.caveDweller.getRandom().nextBoolean()) {
                this.canStandoffOrStalk = true;
                this.caveDweller.stalking = true;
                this.caveDweller.currentRoll = Roll.STALK;
            } else {
                this.caveDweller.currentRoll = Roll.HIDE;
            }
        }

        if (this.lookedAtCount > 2 && !this.canStandoffOrStalk && !this.caveDweller.isInStandoff()) {
            if (this.decisionCooldown <= 0) {
                if (!areCurrentlyLookingTowardsAndSeeing) {
                    if (this.caveDweller.getRandom().nextFloat() < 0.4F) {
                        this.caveDweller.currentRoll = Roll.HIDE;
                    } else {
                        this.decisionCooldown = 40;
                    }
                } else {
                    if (this.caveDweller.getRandom().nextFloat() < 0.6F) {
                        this.canStandoffOrStalk = true;

                        if (this.shouldAttackBasedOnArmor(target)) {
                            this.caveDweller.pleaseStopMoving = true;
                            this.caveDweller.getNavigation().stop();
                            this.caveDweller.setDeltaMovement(Vec3.ZERO);
                            this.caveDweller.setInStandoff(true);
                        } else {
                            this.caveDweller.stalking = true;
                            this.caveDweller.currentRoll = Roll.STALK;
                        }
                    } else {
                        this.decisionCooldown = 60;
                    }
                }
            }
        }
        
        if (areCurrentlyLookingTowardsAndSeeing && !this.canStandoffOrStalk) {
            this.caveDweller.pleaseStopMoving = true;
            this.caveDweller.getNavigation().stop();
            this.caveDweller.setDeltaMovement(Vec3.ZERO);
        }

        if (!areCurrentlyLookingTowardsAndSeeing && !this.caveDweller.isInStandoff()) {
            this.caveDweller.pleaseStopMoving = false;
            this.caveDweller.getNavigation().moveTo(target, 0.7);
            if (this.caveDweller.isMoving()) {
                this.caveDweller.getEntityData().set(CaveDwellerEntity.SQUEEZING_ACCESSOR, true);
                this.caveDweller.refreshDimensions();
            }
        } else {
            if (!this.caveDweller.isMoving()) {
                this.caveDweller.getEntityData().set(CaveDwellerEntity.SQUEEZING_ACCESSOR, false);
                this.caveDweller.refreshDimensions();
            }
        }

        if (this.decisionCooldown <= 0) {
            this.decisionCooldown = 60;
        }

        this.caveDweller.getLookControl().setLookAt(target);
        this.wereNotLooking = !areCurrentlyLookingTowardsAndSeeing;
    }

    private void standoffTick() {
        LivingEntity target = this.caveDweller.getTarget();
        if (target == null) {
            return;
        }

        boolean areCurrentlyLookingTowardsAndSeeing = this.caveDweller.targetIsLookingAtMe && target.hasLineOfSight(this.caveDweller);

        if (this.decisionCooldown <= 0 || !areCurrentlyLookingTowardsAndSeeing) {
            if (this.caveDweller.getRandom().nextBoolean()) {
                this.caveDweller.getEntityData().set(CaveDwellerEntity.AGGRO_ACCESSOR, true);
                this.caveDweller.currentRoll = Roll.CHASE;
            }
        }
    }
}
