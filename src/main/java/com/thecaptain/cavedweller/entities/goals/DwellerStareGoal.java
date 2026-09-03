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
        this.caveDweller.setNoGravity(false);
        this.caveDweller.noPhysics = false;
        this.caveDweller.stalking = false;
        this.caveDweller.setInStandoff(false);
        super.start();
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

    private boolean shouldStandOffBasedOnArmor(LivingEntity target) {
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

        this.caveDweller.pleaseStopMoving = true;
        this.caveDweller.getNavigation().stop();
        this.caveDweller.setDeltaMovement(Vec3.ZERO);

        if (this.caveDweller.isInStandoff()) {
            this.standoffTick();
        }

        boolean areCurrentlyLookingTowardsAndSeeing = this.caveDweller.targetIsLookingAtMe && target.hasLineOfSight(this.caveDweller);

        if (this.wereNotLooking && areCurrentlyLookingTowardsAndSeeing) {
            ++this.lookedAtCount;
        }

        // If decision cooldown is counting down, and dweller is not in a standoff
        if (this.decisionCooldown > 0 && !this.caveDweller.isInStandoff()) {
            // There is a .05% chance every tick to either check if it should stand-off or hide
            if (this.caveDweller.getRandom().nextFloat() < 0.0005F) {
                if (this.shouldStandOffBasedOnArmor(target)) {
                    this.caveDweller.setInStandoff(true);
                } else {
                    this.caveDweller.currentRoll = Roll.HIDE;
                }
            }    
        }

        // If dweller's been looked at 2 or more times, can't stand-off or stalk, and is not in a standoff...
        if (this.lookedAtCount >= 2 && !this.canStandoffOrStalk && !this.caveDweller.isInStandoff()) {
            // If the decision cooldown hits 0...
            if (this.decisionCooldown <= 0) {
                // If target is not looking and seeing...
                if (!areCurrentlyLookingTowardsAndSeeing) {
                    // There is a 40% chance to hide. If not, then set the decision cooldown to 40 ticks
                    if (this.caveDweller.getRandom().nextFloat() < 0.4F) {
                        this.caveDweller.currentRoll = Roll.HIDE;
                    } else {
                        this.decisionCooldown = 40;
                    }
                // If target IS looking and seeing...
                } else {
                    // There is a 60% chance to either stand-off, or start stalking if not able to attack based on armor
                    if (this.caveDweller.getRandom().nextFloat() < 0.6F) {
                        this.canStandoffOrStalk = true;

                        if (this.shouldStandOffBasedOnArmor(target)) {
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

        // Check every 60 ticks (3 seconds) whether to attack. Starts attack if you lose sight of it regardless of decision cooldown
        if (this.decisionCooldown <= 0 || !areCurrentlyLookingTowardsAndSeeing) {
            if (this.caveDweller.getRandom().nextBoolean()) {
                this.caveDweller.currentRoll = Roll.CHASE;
            }
        }
    }
}
