package com.gargin.cavenoise.entity.custom;

import org.jetbrains.annotations.Nullable;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;

public class DwellerTargetTooCloseGoal extends NearestAttackableTargetGoal<Player> {
    private Player pendingTarget;
    private CaveDwellerEntity cavedweller;
    private float distanceThreshold;

    public DwellerTargetTooCloseGoal(CaveDwellerEntity pCaveDweller, float pDistanceThreshold) {
        super(pCaveDweller, Player.class, false);
        this.cavedweller = pCaveDweller;
        this.distanceThreshold = pDistanceThreshold;
    }

    public void setPendingTarget(@Nullable Player pendingTarget) {
        this.pendingTarget = pendingTarget;
    }

    public boolean inPlayerLineOfSight() {
        return this.pendingTarget != null ? this.pendingTarget.hasLineOfSight(this.cavedweller) : false;
    }

    @Override
    public boolean canUse() {
        if (this.cavedweller.isRemoved()) {
            return false;
        } else {
            this.setPendingTarget(this.cavedweller.level().getNearestPlayer(this.cavedweller, (double)this.distanceThreshold));
            if (this.pendingTarget == null) {
                return false;
            } else {
                return this.pendingTarget.isSpectator() ? false : this.inPlayerLineOfSight();
            }
        }
    }

    @Override
    public void start() {
        SynchedEntityData var10000 = this.cavedweller.getEntityData();
        CaveDwellerEntity var10001 = this.cavedweller;
        var10000.set(CaveDwellerEntity.AGGRO_ACCESSOR, true);
        this.cavedweller.isAggro = true;
        this.cavedweller.rRollResult = 0;
        this.target = this.pendingTarget;
        this.cavedweller.setTarget(this.pendingTarget);
        super.start();
    }

    @Override
    public void stop() {
        this.pendingTarget = null;
        super.stop();
    }

    @Override
    public void tick() {
        super.tick();
    }
}
