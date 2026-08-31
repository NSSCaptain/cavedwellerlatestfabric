package com.gargin.cavenoise.entities.goals;

import com.gargin.cavenoise.entities.CaveDwellerEntity;
import net.minecraft.world.entity.ai.goal.Goal;

public class DwellerBreakInvisGoal extends Goal {
    private final CaveDwellerEntity caveDweller;

    public DwellerBreakInvisGoal(CaveDwellerEntity caveDweller) {
        this.caveDweller = caveDweller;
    }

    @Override
    public boolean canUse() {
        return this.caveDweller.isInvisible() && !this.caveDweller.targetIsLookingAtMe;
    }

    @Override
    public void start() {
        super.start();
        this.caveDweller.setNoGravity(false);
        this.caveDweller.noPhysics = false;
        this.caveDweller.setInvisible(false);
    }

}
