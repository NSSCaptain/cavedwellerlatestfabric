package com.thecaptain.cavedweller.entities.goals;

import com.thecaptain.cavedweller.entities.CaveDwellerEntity;
import com.thecaptain.cavedweller.util.Utils;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.phys.Vec3;

public class DwellerStrollGoal extends WaterAvoidingRandomStrollGoal {
    private final CaveDwellerEntity caveDweller;

    public DwellerStrollGoal(CaveDwellerEntity caveDweller, double speedModifier) {
        super(caveDweller, speedModifier);
        this.caveDweller = caveDweller;
    }

    @Override
    public boolean canUse() {
        if (this.caveDweller.currentRoll != Roll.STROLL || this.caveDweller.stalking) {
            return false;
        } else if (!Utils.isValidPlayer(this.caveDweller.getTarget())) {
            return false;
        }

        return super.canUse();
    }

    @Override
    public boolean canContinueToUse() {
        if (this.caveDweller.currentRoll != Roll.STROLL || this.caveDweller.stalking) {
            return false;
        } else if (!Utils.isValidPlayer(this.caveDweller.getTarget())) {
            return false;
        }

        return super.canContinueToUse();
    }

    public void tick() {
        LivingEntity target = this.caveDweller.getTarget();
        if (target == null) {
            this.caveDweller.disappear();
            return;
        }

        this.caveDweller.pleaseStopMoving = true;
        this.caveDweller.getNavigation().stop();
        this.caveDweller.setDeltaMovement(Vec3.ZERO);

        this.caveDweller.getLookControl().setLookAt(target);
    }

    public void stop() {
        this.caveDweller.pleaseStopMoving = false;
    }
}

