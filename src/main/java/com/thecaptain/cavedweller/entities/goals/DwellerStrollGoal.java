package com.thecaptain.cavedweller.entities.goals;

import com.thecaptain.cavedweller.entities.CaveDwellerEntity;
import com.thecaptain.cavedweller.util.Utils;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

public class DwellerStrollGoal extends Goal {
    private final CaveDwellerEntity caveDweller;

    public DwellerStrollGoal(CaveDwellerEntity caveDweller) {
        this.caveDweller = caveDweller;
    }

    @Override
    public boolean canUse() {
        return false;
    }

    @Override
    public boolean canContinueToUse() {
        return false;
    }

    public void tick() {
        if (Utils.isValidPlayer(this.caveDweller.getTarget())) {
            LivingEntity target = this.caveDweller.getTarget();
            this.caveDweller.getLookControl().setLookAt(target);
        }

        this.caveDweller.pleaseStopMoving = true;
        this.caveDweller.getNavigation().stop();
        this.caveDweller.setDeltaMovement(Vec3.ZERO);
    }

    public void stop() {
        this.caveDweller.pleaseStopMoving = false;
    }
}

