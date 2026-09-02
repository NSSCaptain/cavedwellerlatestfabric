package com.thecaptain.cavedweller.entities.goals;

import com.thecaptain.cavedweller.entities.CaveDwellerEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

public class DwellerHideGoal extends Goal {
    private final CaveDwellerEntity caveDweller;
    public boolean needsToHide = false;

    public DwellerHideGoal(CaveDwellerEntity caveDweller) {
        this.caveDweller = caveDweller;

    }

    @Override
    public boolean canUse() {
        if (this.caveDweller.isPassenger() || this.caveDweller.isInvisible()) {
            return false;
        } else if (this.caveDweller.currentRoll != Roll.HIDE) {
            return false;
        } else {
            return this.caveDweller.getTarget() != null;
        }
    }

    @Override
    public boolean canContinueToUse() {
        if (this.caveDweller.currentRoll != Roll.HIDE) {
            return false;
        } else if (this.needsToHide) {
            return false;
        }else {
            return this.caveDweller.getTarget() != null;
        }
    }

    @Override
    public void tick() {
    LivingEntity target = this.caveDweller.getTarget();

        if (target != null) {
            this.caveDweller.getLookControl().setLookAt(target, 180.0F, 1.0F);
        }
        this.needsToHide = true;
        this.caveDweller.pleaseStopMoving = true;
        this.caveDweller.getNavigation().stop();
        this.caveDweller.setDeltaMovement(Vec3.ZERO);
        this.caveDweller.isHiding = true;
        this.caveDweller.getEntityData().set(CaveDwellerEntity.HIDING_ACCESSOR, true);
    }

    public void stop(){
        super.stop();
        this.caveDweller.setFadeState(-1, 0.05F, true);
    }
}
