package com.thecaptain.cavedweller.entities.goals;

import com.thecaptain.cavedweller.CaveDweller;
import com.thecaptain.cavedweller.block.BurntOutTorchBlock;
import com.thecaptain.cavedweller.entities.CaveDwellerEntity;
import com.thecaptain.cavedweller.registry.ModBlocks;
import com.thecaptain.cavedweller.util.Utils;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

public class DwellerStareGoal extends Goal {
    private final CaveDwellerEntity caveDweller;
    private boolean wereNotLooking;
    private int lookedAtCount;
    private int decisionCooldown = 0;
    private final List<List<BlockPos>> pendingWaveRows = new ArrayList<>();
    private int waveTickTimer = 0;
    private final int ticksBetweenWaves = 5;
    private boolean hasTriggeredBurnoutWave = false;
    private boolean canStartChaseSequence = false;

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
            if (CaveDweller.CONFIG.DISAPPEAR()) {
                this.caveDweller.disappear();
            }
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

        double baseChance = 0.15; // 15% chance to attack in general
        double maxChance = 0.80;  // 80% chance to attack a player with max armor points

        double calculatedChance = baseChance + ((double) armorValue / 20.0) * (maxChance - baseChance);
        calculatedChance = Math.min(maxChance, Math.max(baseChance, calculatedChance));

        return this.caveDweller.getRandom().nextDouble() < calculatedChance;
    }

    @Override
    public void tick() {
        LivingEntity target = this.caveDweller.getTarget();


        this.caveDweller.pleaseStopMoving = true;
        this.caveDweller.getNavigation().stop();
        this.caveDweller.setDeltaMovement(Vec3.ZERO);

        if (this.decisionCooldown > 0) {
            --this.decisionCooldown;
        }

        if (this.caveDweller.isInStandoff()) {
           this.standoffTick();
        }

        boolean eitherAreCurrentlyLookingTowardsAndSeeing = this.caveDweller.targetIsLookingAtMe && target.hasLineOfSight(this.caveDweller);

        if (this.wereNotLooking && eitherAreCurrentlyLookingTowardsAndSeeing) {
            ++this.lookedAtCount;
        }

        if (!this.caveDweller.isInStandoff()) {
            if (this.lookedAtCount < 2 && this.decisionCooldown >= 0) {
                if (this.lookedAtCount < 1 && !eitherAreCurrentlyLookingTowardsAndSeeing) {
                    if (this.caveDweller.getRandom().nextFloat() < 0.05F) {
                        this.caveDweller.currentRoll = Roll.HIDE;
                    } else {
                        this.decisionCooldown = 20;
                    }
                }

                if (this.caveDweller.getRandom().nextFloat() < 0.005F) {
                    if (this.shouldStandOffBasedOnArmor(target)) {
                        this.caveDweller.setInStandoff(true);
                    } else {
                        this.caveDweller.currentRoll = Roll.HIDE;
                    }
                }
            } else if (this.lookedAtCount >= 2 && this.decisionCooldown <= 0) {
                if (!eitherAreCurrentlyLookingTowardsAndSeeing) {
                    if (this.caveDweller.getRandom().nextFloat() < 0.3F) {
                        this.caveDweller.currentRoll = Roll.HIDE;
                    } else {
                        if (this.shouldStandOffBasedOnArmor(target)) {
                            this.caveDweller.setInStandoff(true);
                        }
                    }
                } else {
                    if (this.caveDweller.getRandom().nextFloat() < 0.6F) {
                        if (this.shouldStandOffBasedOnArmor(target)) {
                            this.caveDweller.setInStandoff(true);
                        }
                    } else {
                        this.decisionCooldown = 20;
                    }
                }
            }
        }

        if (!this.pendingWaveRows.isEmpty()) {
            this.waveTickTimer--;
            if (this.waveTickTimer <= 0) {
                List<BlockPos> currentRow = this.pendingWaveRows.remove(0);
                this.extinguishTorchRow(currentRow);
                this.waveTickTimer = ticksBetweenWaves;
            }
        }

        this.caveDweller.getLookControl().setLookAt(target);
        this.wereNotLooking = !eitherAreCurrentlyLookingTowardsAndSeeing;
    }

    private void standoffTick() {
        LivingEntity target = this.caveDweller.getTarget();
        if (target == null) {
            return;
        }

        boolean eitherAreCurrentlyLookingTowardsAndSeeing = this.caveDweller.targetIsLookingAtMe && target.hasLineOfSight(this.caveDweller);

        // Check when to attack. Starts attack if you lose sight of it regardless of decision cooldown, unless already burning out torches
        if (this.canStartChaseSequence) {
            if (!this.hasTriggeredBurnoutWave) {
                this.prepareBurnoutWave(target);
                this.hasTriggeredBurnoutWave = true;
            }
            if (this.decisionCooldown <= 0) {
                this.caveDweller.currentRoll = Roll.CHASE;
            }
        } else if (!this.canStartChaseSequence && this.decisionCooldown <= 0) {
            if (this.caveDweller.getRandom().nextBoolean()) {
                this.canStartChaseSequence = true;
                this.decisionCooldown = 35;
            }
        } else if (!eitherAreCurrentlyLookingTowardsAndSeeing) {
            this.caveDweller.currentRoll = Roll.CHASE;
        }
    }

    private void prepareBurnoutWave(LivingEntity player) {
        this.pendingWaveRows.clear();

        ServerLevel level = (ServerLevel) this.caveDweller.level();
        BlockPos mobPos = this.caveDweller.blockPosition();
        BlockPos playerPos = player.blockPosition();

        Block burntOutTorchBlock = ModBlocks.getBurntOutTorch();
        Block burntOutWallTorchBlock = ModBlocks.getBurntOutWallTorch();

        int minX = Math.min(mobPos.getX(), playerPos.getX()) - 4;
        int maxX = Math.max(mobPos.getX(), playerPos.getX()) + 4;
        int minY = Math.min(mobPos.getY(), playerPos.getY()) - 4;
        int maxY = Math.max(mobPos.getY(), playerPos.getY()) + 4;
        int minZ = Math.min(mobPos.getZ(), playerPos.getZ()) - 4;
        int maxZ = Math.max(mobPos.getZ(), playerPos.getZ()) + 4;

        java.util.TreeMap<Integer, List<BlockPos>> sortedSlices = new java.util.TreeMap<>();

        for (BlockPos targetPos : BlockPos.betweenClosed(minX, minY, minZ, maxX, maxY, maxZ)) {
            BlockState state = level.getBlockState(targetPos);
            Block block = state.getBlock();

            if ((block instanceof TorchBlock || block instanceof WallTorchBlock)
                    && !(block instanceof RedstoneTorchBlock)
                    && !(block instanceof RedstoneWallTorchBlock)
                    && block != burntOutTorchBlock
                    && block != burntOutWallTorchBlock) {

                int blockDistance = Math.abs(targetPos.getX() - mobPos.getX())
                        + Math.abs(targetPos.getY() - mobPos.getY())
                        + Math.abs(targetPos.getZ() - mobPos.getZ());

                sortedSlices.computeIfAbsent(blockDistance, k -> new ArrayList<>()).add(targetPos.immutable());
            }
        }

        for (java.util.Map.Entry<Integer, List<BlockPos>> entry : sortedSlices.entrySet()) {
            this.pendingWaveRows.add(entry.getValue());
        }

        this.waveTickTimer = 0;
    }

    private void extinguishTorchRow(List<BlockPos> row) {
        ServerLevel level = (ServerLevel) this.caveDweller.level();
        Block burntOutTorchBlock = ModBlocks.getBurntOutTorch();
        Block burntOutWallTorchBlock = ModBlocks.getBurntOutWallTorch();

        for (BlockPos torchPos : row) {
            BlockState originalState = level.getBlockState(torchPos);
            Block block = originalState.getBlock();

            if ((block instanceof TorchBlock || block instanceof WallTorchBlock)
                    && block != burntOutTorchBlock && block != burntOutWallTorchBlock) {

                BlockState initialBurntState;
                if (originalState.getBlock() instanceof WallTorchBlock) {
                    initialBurntState = burntOutWallTorchBlock.defaultBlockState()
                            .setValue(WallTorchBlock.FACING, originalState.getValue(WallTorchBlock.FACING));
                } else {
                    initialBurntState = burntOutTorchBlock.defaultBlockState();
                }

                initialBurntState = initialBurntState.setValue(BurntOutTorchBlock.LIGHT, 14);
                level.setBlock(torchPos, initialBurntState, 3);

                level.sendParticles(
                        net.minecraft.core.particles.ParticleTypes.SMOKE,
                        torchPos.getX() + 0.5D, torchPos.getY() + 0.7D, torchPos.getZ() + 0.5D,
                        12, 0.05D, 0.1D, 0.05D, 0.02D
                );

                level.playSound(null, torchPos, net.minecraft.sounds.SoundEvents.REDSTONE_TORCH_BURNOUT, net.minecraft.sounds.SoundSource.BLOCKS, 1.0F, 1.0F);
            }
        }
    }
}
