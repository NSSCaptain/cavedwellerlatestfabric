package com.gargin.cavenoise.entities.goals;

import com.gargin.cavenoise.entities.CaveDwellerEntity;
import com.gargin.cavenoise.util.Utils;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;

public class DwellerTargetTooCloseGoal extends NearestAttackableTargetGoal<Player> {
    private final CaveDwellerEntity caveDweller;
    private final float distanceThreshold;

    public DwellerTargetTooCloseGoal(CaveDwellerEntity caveDweller, float distanceThreshold) {
        super(caveDweller, Player.class, false);
        this.caveDweller = caveDweller;
        this.distanceThreshold = distanceThreshold;
    }

    @Override
    public boolean canUse() {
        if (!this.caveDweller.isInvisible()) {
            LivingEntity target = this.caveDweller.level().getNearestPlayer(this.caveDweller, (double) this.distanceThreshold);

            if (Utils.isValidPlayer(target)) {
                this.target = target;
                return true;
            }
        }

        return false;
    }

    @Override
    public void start() {
        this.caveDweller.setNoGravity(false);
        this.caveDweller.noPhysics = false;
        this.caveDweller.stalking = false;
        this.caveDweller.getEntityData().set(CaveDwellerEntity.AGGRO_ACCESSOR, true);
        this.caveDweller.currentRoll = Roll.CHASE;
        this.caveDweller.setTarget(this.target);
        super.start();
    }

    @Override
    public void stop() {
        super.stop();
    }

    @Override
    public boolean canContinueToUse() {
        return Utils.isValidPlayer(this.target);
    }

    @Override
    public void tick() {
        super.tick();
    }
}

