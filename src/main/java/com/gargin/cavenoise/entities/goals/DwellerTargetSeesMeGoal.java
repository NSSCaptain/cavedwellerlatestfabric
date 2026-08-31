package com.gargin.cavenoise.entities.goals;

import com.gargin.cavenoise.entities.CaveDwellerEntity;
import com.gargin.cavenoise.util.Utils;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

public class DwellerTargetSeesMeGoal extends NearestAttackableTargetGoal<Player> {
    private final CaveDwellerEntity caveDweller;

    public DwellerTargetSeesMeGoal(CaveDwellerEntity caveDweller) {
        super(caveDweller, Player.class, false);
        this.caveDweller = caveDweller;
    }

    @Override
    public boolean canUse() {
        if (this.caveDweller.isInvisible()) {
            return false;
        } else {
            this.target = Utils.getValidTarget(this.caveDweller);
            return !Utils.isValidPlayer(this.target) ? false : this.caveDweller.isPlayerLookingTowards(this.target) && this.target.hasLineOfSight(this.caveDweller);
        }
    }

    @Override
    public void start() {
        this.caveDweller.setNoGravity(false);
        this.caveDweller.noPhysics = false;
        this.caveDweller.setTarget(this.target);
        this.caveDweller.stalking = false;
        this.caveDweller.getEntityData().set(CaveDwellerEntity.SPOTTED_ACCESSOR, true);
        if (this.target != null && this.target.hasLineOfSight(this.caveDweller)) {
            this.caveDweller.currentRoll = Roll.STARE;
        } else {
            this.caveDweller.reRoll();
        }

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
