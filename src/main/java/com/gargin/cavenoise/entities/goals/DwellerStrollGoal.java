package com.gargin.cavenoise.entities.goals;

import com.gargin.cavenoise.entities.CaveDwellerEntity;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;

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
        }
        return super.canUse();
    }

    @Override
    public boolean canContinueToUse() {
        if (this.caveDweller.currentRoll != Roll.STROLL || this.caveDweller.stalking) {
            return false;
        }
        return super.canContinueToUse();
    }
}

