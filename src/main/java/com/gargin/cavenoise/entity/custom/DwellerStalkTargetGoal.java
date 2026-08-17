package com.gargin.cavenoise.entity.custom;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;

// Uses vanilla minecraft code.
public class DwellerStalkTargetGoal extends NearestAttackableTargetGoal<Player> {
    public DwellerStalkTargetGoal(Mob pMob, Class<Player> pTargetType, boolean pMustSee) {
        super(pMob, pTargetType, pMustSee);
    }
}
