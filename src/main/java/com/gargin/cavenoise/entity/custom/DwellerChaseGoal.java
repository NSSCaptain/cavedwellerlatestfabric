package com.gargin.cavenoise.entity.custom;

import java.util.EnumSet;
import java.util.Random;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

public class DwellerChaseGoal extends Goal {
    protected final PathfinderMob mob;
    private final CaveDwellerEntity cavedweller;
    private final double speedModifier;
    private double crawlModifier = 0.6;
    private double speedInLavaPerTick = 0.6;
    private final boolean followingTargetEvenIfNotSeen;
    private Path path;
    private double pathedTargetX;
    private double pathedTargetY;
    private double pathedTargetZ;
    private int ticksUntilNextPathRecalculation;
    private int ticksUntilNextAttack;
    private final int attackInterval = 20;
    private long lastCanUseCheck;
    private static final long COOLDOWN_BETWEEN_CAN_USE_CHECKS = 20L;
    private int failedPathFindingPenalty = 0;
    private boolean canPenalize = false;
    private float ticksTillChase;
    private float currentTicksTillChase;
    private boolean shouldUseShortPath = false;
    private boolean shouldUseClimbPath = false;
    private boolean squeezing = false;
    private boolean climbing = false;
    private Path shortPath;
    private Path climbPath;
    private Vec3 vecNodePos;
    private Vec3 vecMobPos;
    private int ticksToSqueeze;
    private int currentTicksToSqueeze;
    private int ticksTillLeave;
    private int currentTicksTillLeave;
    Vec3 xPathStartVec;
    Vec3 zPathStartVec;
    Vec3 xPathTargetVec;
    Vec3 zPathTargetVec;
    Vec3 vecTargetPos;
    Vec3 nodePositionCooldownPos;
    BlockPos nodePos;
    Logger logger = LogManager.getLogManager().getLogger("cavenoise");
    private BlockPos climbPos;
    private boolean climbPathAvailable;
    private boolean shortPathAvailable;
    private boolean normalPathAvailable;
    private float climbRelativeY = 0.0F;
    private float climbTicks = 0.0F;
    private float climbSpeed = 4.0F;
    private int maxClimb = 50;
    private int climbInt = 0;
    private Vec3 climbStartVec;
    Vec3 newClimbAroundPos = new Vec3((double)0.0F, (double)0.0F, (double)0.0F);
    Random rand = new Random();
    BlockPos currentBlock = new BlockPos(0, 0, 0);
    BlockPos oldBlock = new BlockPos(0, 0, 0);
    int torchDestructionRadius = 1;
    BlockPos checkBlockForTorch;
    boolean isStartSqueezingOrSqueezingTickRunning = false;

    public DwellerChaseGoal(PathfinderMob pMob, CaveDwellerEntity pCaveDweller, double pSpeedModifier, boolean pFollowingTargetEvenIfNotSeen, float pTicksTillChase) {
        this.mob = pMob;
        this.speedModifier = pSpeedModifier;
        this.followingTargetEvenIfNotSeen = pFollowingTargetEvenIfNotSeen;
        this.cavedweller = pCaveDweller;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        this.ticksTillChase = pTicksTillChase;
        this.currentTicksTillChase = pTicksTillChase;
        this.vecNodePos = null;
        this.ticksToSqueeze = 15;
        this.nodePos = null;
        this.ticksTillLeave = 600;
        this.currentTicksTillLeave = this.ticksTillLeave;
    }

    @Override
    public boolean canUse() {
        if (this.cavedweller.isRemoved() || this.cavedweller.isDeadOrDying()) {
            return false;
        } else if (this.cavedweller.rRollResult == 0 && !this.cavedweller.forcedStalk) {
            long i = this.mob.level().getGameTime();
            if (i - this.lastCanUseCheck < 20L) {
                return false;
            } else {
                this.lastCanUseCheck = i;
                LivingEntity livingentity = this.mob.getTarget();
                if (livingentity == null) {
                    return false;
                } else if (!livingentity.isAttackable()) {
                    return false;
                } else if (this.canPenalize) {
                    if (--this.ticksUntilNextPathRecalculation <= 0) {
                        this.path = this.mob.getNavigation().createPath(livingentity, 0);
                        this.ticksUntilNextPathRecalculation = 2;
                        return this.path != null;
                    } else {
                        return true;
                    }
                } else {
                    this.path = this.mob.getNavigation().createPath(livingentity, 0);
                    if (this.path != null) {
                        return true;
                    } else {
                        return this.getAttackReachSqr(livingentity) >= this.mob.distanceToSqr(livingentity.getX(), livingentity.getY(), livingentity.getZ());
                    }
                }
            }
        } else {
            return false;
        }
    }

    @Override
    public boolean canContinueToUse() {
        LivingEntity livingentity = this.mob.getTarget();
        if (livingentity == null) {
            return false;
        } else if (!livingentity.isAttackable()) {
            this.cavedweller.discard();
            return false;
        } else if (!this.followingTargetEvenIfNotSeen) {
            return !this.mob.getNavigation().isDone();
        } else if (!this.mob.isWithinRestriction(livingentity.blockPosition())) {
            return false;
        } else {
            return !(livingentity instanceof Player player) || !player.isInvisible() && !player.isCreative() && !player.isSpectator();
        }
    }

    @Override
    public void start() {
        this.ticksUntilNextPathRecalculation = 0;
        this.ticksUntilNextAttack = 0;
        super.start();
    }

    @Override
    public void stop() {
        LivingEntity livingentity = this.mob.getTarget();
        if (!EntitySelector.NO_CREATIVE_OR_SPECTATOR.test(livingentity)) {
            this.mob.setTarget(null);
        }

        this.cavedweller.squeezeCrawling = false;
        SynchedEntityData var10000 = this.cavedweller.getEntityData();
        CaveDwellerEntity var10001 = this.cavedweller;
        var10000.set(CaveDwellerEntity.AGGRO_ACCESSOR, false);
        this.cavedweller.isAggro = false;
        this.cavedweller.refreshDimensions();
        this.currentTicksTillChase = this.ticksTillChase;
        this.mob.setAggressive(false);
        this.mob.getNavigation().stop();
        this.cavedweller.setNoGravity(false);
        this.cavedweller.noPhysics = false;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    public void tickAggroClock() {
        --this.currentTicksTillChase;
        if (this.currentTicksTillChase <= 0.0F) {
            SynchedEntityData var10000 = this.cavedweller.getEntityData();
            CaveDwellerEntity var10001 = this.cavedweller;
            var10000.set(CaveDwellerEntity.AGGRO_ACCESSOR, true);
        }

        this.cavedweller.isAggro = true;
        this.cavedweller.refreshDimensions();
    }

    public Path getShortPath(LivingEntity livingentity) {
        return this.shortPath = this.cavedweller.createShortPath(livingentity);
    }

    public Path getClimbPath(LivingEntity livingentity) {
        return this.climbPath = this.cavedweller.createClimbPath(livingentity);
    }

    public static double lerp(double a, double b, double f) {
        return (b - a) * f + a;
    }

    public void startSqueezing() {
        // I don't know why this was in the original. It seems to just break the squeezing action
        /*
        this.vecNodePos = null;
        this.vecMobPos = null;
        this.xPathStartVec = null;
        this.zPathStartVec = null;
        this.xPathTargetVec = null;
        this.zPathTargetVec = null;
        this.vecTargetPos = null;
        this.currentTicksToSqueeze = 0;
        */
        isStartSqueezingOrSqueezingTickRunning = true;
        this.squeezing = true;
        System.out.println("[DwellerChaseGoal] startSqueezing(): set this.squeezing to true");
        SynchedEntityData var10000 = this.cavedweller.getEntityData();
        CaveDwellerEntity var10001 = this.cavedweller;
        var10000.set(CaveDwellerEntity.SQUEEZING_ACCESSOR, true);
        // Same here
        /*
        this.nodePos = null;
        */
    }

    public void squeezingTick() {
        boolean isStartSqueezingOrSqueezingTickRunning = true;
        System.out.println("[DwellerChaseGoal] squeezingTick(): squeezingTick() called!");
        this.cavedweller.setNoGravity(true);
        this.cavedweller.noPhysics = true;
        if (this.nodePos == null && this.mob.getNavigation().getPath() != null) {
            this.nodePos = this.mob.getNavigation().getPath().getNextNodePos();
        }
        this.vecMobPos = vecNodePos;
        this.vecTargetPos = this.vecMobPos;

        this.mob.getNavigation().stop();
        if (this.nodePos == null) {
            System.out.println("[DwellerChaseGoal] squeezingTick(): nodePos == null; called stopSqueezing()");
            this.stopSqueezing();
        } else {
            if (this.vecNodePos == null) {
                this.vecNodePos = new Vec3((double)this.nodePos.getX(), (double)this.nodePos.getY(), (double)this.nodePos.getZ());
            }

            this.nodePositionCooldownPos = this.vecNodePos;
            Vec3 vecOldMobPos = this.cavedweller.getViewVector(1.0F);
            if (this.xPathStartVec == null) {
                if (vecOldMobPos.x < this.vecNodePos.x) {
                    this.xPathStartVec = new Vec3(this.vecNodePos.x - 1.0F, this.vecNodePos.y, this.vecNodePos.z + 0.5F);
                    this.xPathTargetVec = new Vec3(this.vecNodePos.x + 1.0F, this.vecNodePos.y, this.vecNodePos.z + 0.5F);
                } else {
                    this.xPathStartVec = new Vec3(this.vecNodePos.x + 1.0F, this.vecNodePos.y, this.vecNodePos.z + 0.5F);
                    this.xPathTargetVec = new Vec3(this.vecNodePos.x - 1.0F, this.vecNodePos.y, this.vecNodePos.z + 0.5F);
                }
            }

            if (this.zPathStartVec == null) {
                if (vecOldMobPos.z < this.vecNodePos.z) {
                    this.zPathStartVec = new Vec3(this.vecNodePos.x + 0.5F, this.vecNodePos.y, this.vecNodePos.z - 1.0F);
                    this.zPathTargetVec = new Vec3(this.vecNodePos.x + 0.5F, this.vecNodePos.y, this.vecNodePos.z + 1.0F);
                } else {
                    this.zPathStartVec = new Vec3(this.vecNodePos.x + 0.5F, this.vecNodePos.y, this.vecNodePos.z + 1.0F);
                    this.zPathTargetVec = new Vec3(this.vecNodePos.x + 0.5F, this.vecNodePos.y, this.vecNodePos.z - 1.0F);
                }
            }

            BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos(this.xPathTargetVec.x, this.xPathTargetVec.y, this.xPathTargetVec.z);
            BlockState blockstate = this.cavedweller.level().getBlockState(blockpos$mutableblockpos);
            boolean xBlocked = blockstate.isCollisionShapeFullBlock(this.cavedweller.level(), blockpos$mutableblockpos);
            blockpos$mutableblockpos = new BlockPos.MutableBlockPos(this.zPathTargetVec.x, this.zPathTargetVec.y, this.zPathTargetVec.z);
            blockstate = this.cavedweller.level().getBlockState(blockpos$mutableblockpos);
            boolean zBlocked = blockstate.isCollisionShapeFullBlock(this.cavedweller.level(), blockpos$mutableblockpos);
            if (xBlocked) {
                this.vecMobPos = this.zPathStartVec;
                this.vecTargetPos = this.zPathTargetVec;
            }

            if (zBlocked) {
                this.vecMobPos = this.xPathStartVec;
                this.vecTargetPos = this.xPathTargetVec;
            }

            if (this.vecTargetPos != null && this.vecMobPos != null) {
                // Inc. squeeze timer
                ++this.currentTicksToSqueeze;
                float tickF = (float)this.currentTicksToSqueeze / (float)this.ticksToSqueeze;
                // Define the interpolation for the dweller's movement
                Vec3 vecCurrentMobPos = new Vec3(lerp(this.vecMobPos.x, this.vecTargetPos.x, (double)tickF), this.vecMobPos.y, lerp(this.vecMobPos.z, this.vecTargetPos.z, (double)tickF));
                // Define the dweller's rotation
                Vec3 rotAxis = new Vec3(this.vecTargetPos.x - this.vecMobPos.x, 0.0F, this.vecTargetPos.z - this.vecMobPos.z); rotAxis = rotAxis.normalize();
                // Rotate the body
                double rotAngle = Math.toDegrees(Math.atan2(-rotAxis.x, rotAxis.z));
                this.cavedweller.setYBodyRot((float)rotAngle);
                // Move to destination pos.
                this.cavedweller.moveTo(vecCurrentMobPos.x, vecCurrentMobPos.y, vecCurrentMobPos.z, (float)rotAngle, (float)rotAngle);

                // If tickF reaches 1.0 (or higher), the squeeze is done
                if (tickF >= 1.0F) {
                    // Double check dweller reached position(?)
                    this.cavedweller.setPos(this.vecTargetPos.x, this.vecTargetPos.y, this.vecTargetPos.z);
                    // Stop squeezing
                    System.out.println("[DwellerChaseGoal] squeezingTick(): tickF is >= 1.0 (" + tickF + "); called stopSqueezing()");
                    this.stopSqueezing();
                    currentTicksToSqueeze = 1;
                    ticksToSqueeze = 1;
                }

            } else {
                if (this.vecTargetPos == null) {
                    System.out.println("[DwellerChaseGoal] squeezingTick(): this.vecTargetPos == null; called stopSqueezing()");
                    this.stopSqueezing();
                } else if (this.vecMobPos == null) {
                    System.out.println("[DwellerChaseGoal] squeezingTick(): this.vecMobPos == null; called stopSqueezing()");
                    this.stopSqueezing();
                }
            }
        }
    }

    public void stopSqueezing() {
        isStartSqueezingOrSqueezingTickRunning = false;
        System.out.println("[DwellerChaseGoal] stopSqueezing(): set this.squeezing to false");
        this.squeezing = false;
        SynchedEntityData var10000 = this.cavedweller.getEntityData();
        CaveDwellerEntity var10001 = this.cavedweller;
        var10000.set(CaveDwellerEntity.SQUEEZING_ACCESSOR, false);
        this.cavedweller.setNoGravity(false);
        this.cavedweller.noPhysics = false;
    }

    public void startClimbing(BlockPos pClimbPos) {
        this.climbStartVec = this.cavedweller.position();
        this.climbRelativeY = 0.0F;
        this.climbTicks = 0.0F;
        this.climbInt = 0;
        this.climbPos = pClimbPos;
        this.climbing = true;
        SynchedEntityData var10000 = this.cavedweller.getEntityData();
        CaveDwellerEntity var10001 = this.cavedweller;
        var10000.set(CaveDwellerEntity.CLIMBING_ACCESSOR, true);
        System.out.println("started climbing with pos: " + this.climbPos);
    }

    public void stopClimbing() {
        this.climbing = false;
        SynchedEntityData var10000 = this.cavedweller.getEntityData();
        CaveDwellerEntity var10001 = this.cavedweller;
        var10000.set(CaveDwellerEntity.CLIMBING_ACCESSOR, false);
        this.cavedweller.setNoGravity(false);
        this.cavedweller.noPhysics = false;
        System.out.println("stopped climbing");
    }

    public boolean checkIfShouldSqueeze(Path pathToCheck) {
        // If there is no path, don't squeeze
        if (pathToCheck == null) {
            return false;
        }
        // If the path is finished, don't squeeze
        if (pathToCheck.isDone()) {
            return false;
        }
        BlockPos blockpos = pathToCheck.getNextNodePos();
        // Prevents the AI from spamming checks on the same stuck position
        if (this.nodePositionCooldownPos != null
                && blockpos.getX() == (int) this.nodePositionCooldownPos.x()
                && blockpos.getY() == (int) this.nodePositionCooldownPos.y()
                && blockpos.getZ() == (int) this.nodePositionCooldownPos.z()) {
            return false;
        }
        // Target the block exactly 1 block above the floor path (head height)
        BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos(
                blockpos.getX(),
                blockpos.getY() + 1,
                blockpos.getZ()
        );
        BlockState blockstate = this.cavedweller.level().getBlockState(blockpos$mutableblockpos);
        // FIX: Added '!' so flag is TRUE if the block has collision (is solid)
        boolean flag = !blockstate.getCollisionShape(this.cavedweller.level(), blockpos$mutableblockpos).isEmpty();
        System.out.println("[DwellerChaseGoal] checkIfShouldSqueeze(): returned flag (" + flag + ")");
        // result may not be accurate to whether dweller can squeeze or not
        return flag;
    }

    public BlockPos checkIfShouldClimbAndReturnPos(Path pathToCheck) {
        if (pathToCheck == null) {
            return null;
        } else {
            BlockPos blockpos = null;
            if (!pathToCheck.isDone()) {
                blockpos = pathToCheck.getNextNodePos();
                // Prevents dweller from "climbing" liquids
                /*
                if (!this.cavedweller.level().getBlockState(blockpos).getFluidState().isEmpty()) {
                    return null;
                }
                */
                boolean flag = blockpos.getY() >= this.cavedweller.blockPosition().getY() + (double)2.0F;
                return flag ? blockpos : null;
            } else {
                return null;
            }
        }
    }

    public void aggroTick() {
        this.cavedweller.playChaseSound();
        this.cavedweller.noPhysics = false;
        this.cavedweller.setNoGravity(false);
        LivingEntity livingentity = this.mob.getTarget();
        // If dweller nav. is not null, and it should squeeze, and it should use short path, start squeezing
        System.out.println("[DwellerChaseGoal] aggroTick(): called checkIfShouldSqueeze()");
        if (this.checkIfShouldSqueeze(this.mob.getNavigation().getPath()) && this.shouldUseShortPath) {
            System.out.println("[DwellerChaseGoal] aggroTick(): checkIfShouldSqueeze() returned true; called startSqueezing()");
            this.startSqueezing();
            // System.out.println("[DwellerChaseGoal] aggroTick(): set this.squeezing to true (redundant?)");
            // this.squeezing = true;
            SynchedEntityData var10000 = this.cavedweller.getEntityData();
            CaveDwellerEntity var10001 = this.cavedweller;
            var10000.set(CaveDwellerEntity.SQUEEZING_ACCESSOR, true);
        } else {
                System.out.println("[DwellerChaseGoal] aggroTick(): checkIfShouldSqueeze() returned false; continuing");
                BlockPos tempClimbPos = this.checkIfShouldClimbAndReturnPos(this.shortPath);
                if (tempClimbPos != null && this.shouldUseShortPath) {
                    this.startClimbing(tempClimbPos);
                    return;
            }

            if (livingentity != null) {
                double d0 = this.mob.distanceToSqr(livingentity);
                this.ticksUntilNextPathRecalculation = Math.max(this.ticksUntilNextPathRecalculation - 1, 0);
                if ((this.followingTargetEvenIfNotSeen || this.mob.getSensing().hasLineOfSight(livingentity) && this.ticksUntilNextPathRecalculation <= 0 && (this.pathedTargetX == (double)0.0F && this.pathedTargetY == (double)0.0F && this.pathedTargetZ == (double)0.0F || livingentity.distanceToSqr(this.pathedTargetX, this.pathedTargetY, this.pathedTargetZ) >= (double)1.0F || this.mob.getRandom().nextFloat() < 0.05F))) {
                    this.pathedTargetX = livingentity.getX();
                    this.pathedTargetY = livingentity.getY();
                    this.pathedTargetZ = livingentity.getZ();
                    this.ticksUntilNextPathRecalculation = 2;
                    if (this.canPenalize) {
                        this.ticksUntilNextPathRecalculation += this.failedPathFindingPenalty;
                        if (this.mob.getNavigation().getPath() != null) {
                            Node finalPathPoint = this.mob.getNavigation().getPath().getEndNode();
                            if (finalPathPoint != null && livingentity.distanceToSqr((double)finalPathPoint.x, (double)finalPathPoint.y, (double)finalPathPoint.z) < (double)1.0F) {
                                this.failedPathFindingPenalty = 0;
                            } else {
                                this.failedPathFindingPenalty += 10;
                            }
                        } else {
                            this.failedPathFindingPenalty += 10;
                        }
                    }

                    this.getShortPath(livingentity);
                    if (this.shortPath != null) {
                        Node finalShortPathPoint = this.shortPath.getEndNode();
                        if (finalShortPathPoint != null && livingentity.distanceToSqr((double) finalShortPathPoint.x, (double) finalShortPathPoint.y, (double) finalShortPathPoint.z) < (double) 2.0F) {
                            this.shortPathAvailable = true;
                        } else {
                            this.shortPathAvailable = false;
                        }
                    } else {
                        this.shortPathAvailable = false;
                    }

                    this.getClimbPath(livingentity);
                    if (this.climbPath != null) {
                        Node finalClimbPathPoint = this.shortPath.getEndNode();
                        if (finalClimbPathPoint != null && livingentity.distanceToSqr((double) finalClimbPathPoint.x, (double) finalClimbPathPoint.y, (double) finalClimbPathPoint.z) < (double) 1.0F) {
                            this.climbPathAvailable = true;
                        } else {
                            this.climbPathAvailable = false;
                        }
                    } else {
                        this.climbPathAvailable = false;
                    }

                    this.shouldUseShortPath = this.shortPathAvailable;
                    this.shouldUseClimbPath = !this.shortPathAvailable && this.climbPathAvailable && !this.normalPathAvailable;
                    if (d0 > (double)1024.0F) {
                        this.ticksUntilNextPathRecalculation += 10;
                    } else if (d0 > (double)256.0F) {
                        this.ticksUntilNextPathRecalculation += 5;
                    }

                    if (!this.shouldUseShortPath && !this.shouldUseClimbPath) {
                        if (!this.mob.getNavigation().moveTo(livingentity, this.speedModifier)) {
                            this.cavedweller.startedMovingChase = true;
                            this.ticksUntilNextPathRecalculation += 8;
                        }
                    } else if (this.shouldUseShortPath) {
                        if (!this.mob.getNavigation().moveTo(this.shortPath, this.speedModifier)) {
                            this.cavedweller.startedMovingChase = true;
                            this.ticksUntilNextPathRecalculation += 8;
                        }
                    } else if (this.shouldUseClimbPath && !this.mob.getNavigation().moveTo(this.climbPath, this.speedModifier)) {
                        this.cavedweller.startedMovingChase = true;
                        this.ticksUntilNextPathRecalculation += 8;
                    }

                    this.ticksUntilNextPathRecalculation = this.adjustedTickDelay(this.ticksUntilNextPathRecalculation);

                }

                this.ticksUntilNextAttack = Math.max(this.ticksUntilNextAttack - 1, 0);
                this.checkAndPerformAttack(livingentity, d0);
            }

            if (this.cavedweller.isInLava() && this.cavedweller.getNavigation().getPath().getNextNodeIndex() < this.cavedweller.getNavigation().getPath().getNodeCount()) {
                System.out.println("ticking lava move");
                Vec3 a = this.cavedweller.position();
                BlockPos b = this.cavedweller.getNavigation().getPath().getNextNodePos();
                Vec3 dir = (new Vec3((double)b.getX() - a.x, (double)b.getY() - a.y, (double)b.getZ() - a.z)).normalize();
                double dist = dir.distanceToSqr(Vec3.ZERO);
                if (dist > this.speedInLavaPerTick) {
                    this.cavedweller.setPos(this.cavedweller.position().add(new Vec3(dir.x * this.speedInLavaPerTick, dir.y * this.speedInLavaPerTick, dir.z * this.speedInLavaPerTick)));
                } else {
                    this.cavedweller.setPos(new Vec3((double)b.getX(), (double)b.getY(), (double)b.getZ()));
                }
            }

        }
    }

    public void climbingTick() {
        this.cavedweller.playClimbSound();
        this.cavedweller.setNoGravity(true);
        this.cavedweller.noPhysics = true;
        LivingEntity livingentity = this.mob.getTarget();
        if (this.mob.getNavigation().getPath() != null) {
            this.nodePos = this.mob.getNavigation().getPath().getNextNodePos();
        }

        this.mob.getNavigation().stop();
        if (this.nodePos == null) {
            this.stopClimbing();
        }

        while(this.climbInt < this.maxClimb && !this.cavedweller.level().getBlockState(this.climbPos).isAir()) {
            this.climbPos = new BlockPos(this.climbPos.getX(), this.climbPos.getY() + 1, this.climbPos.getZ());
            ++this.climbInt;
        }

        if (this.cavedweller.position().y < (double)((float)this.climbPos.getY() - 2.0F)) {
            ++this.climbTicks;
            this.climbRelativeY = this.climbTicks / this.climbSpeed;
            Vec3 rotAxis = new Vec3((double)this.climbPos.getX() - this.climbStartVec.x, (double)0.0F, (double)this.climbPos.getZ() - this.climbStartVec.z);
            rotAxis = rotAxis.normalize();
            double rotAngle = Math.toDegrees(Math.atan2(-rotAxis.x, rotAxis.z));
            this.cavedweller.setYBodyRot((float)rotAngle);
            this.cavedweller.moveTo(this.climbStartVec.x, (double)this.climbRelativeY + this.climbStartVec.y, this.climbStartVec.z, (float)rotAngle, (float)rotAngle);
            BlockPos blockCheckHead = new BlockPos((int)Math.floor(this.climbStartVec.x), (int)Math.floor(this.climbStartVec.y + (double)this.climbRelativeY) + 2, (int)Math.floor(this.climbStartVec.z));
            if (!this.cavedweller.level().getBlockState(blockCheckHead).isAir()) {
                BlockPos spotToCreateArrayAround = new BlockPos(this.climbPos.getX(), blockCheckHead.getY(), this.climbPos.getZ());
                int blockAmountCovered = 0;

                for(int x = -1; x < 2; ++x) {
                    for(int z = -1; z < 2; ++z) {
                        if ((x != 0 || z != 0) && !this.cavedweller.level().getBlockState(new BlockPos(spotToCreateArrayAround.getX() + x, spotToCreateArrayAround.getY(), spotToCreateArrayAround.getZ() + z)).isAir()) {
                            ++blockAmountCovered;
                        }
                    }
                }

                if (blockAmountCovered >= 8) {
                    this.stopClimbing();
                    return;
                }

                BlockPos[] blockPosClimbArray = this.createBlockPosClimbArray(spotToCreateArrayAround);
                boolean[] climbableArray = this.createClimbableArray(blockPosClimbArray);
                boolean[] openingArray = this.createOpeningArray(blockPosClimbArray);
                boolean goingRight = this.rand.nextBoolean();
                int dwellerIndex = 0;
                int tempDwellerIndexCheck = 0;

                for(BlockPos tempPos : blockPosClimbArray) {
                    if (tempPos.getX() == blockCheckHead.getX() && tempPos.getY() == blockCheckHead.getY() && tempPos.getZ() == blockCheckHead.getZ()) {
                        dwellerIndex = tempDwellerIndexCheck;
                        break;
                    }

                    ++tempDwellerIndexCheck;
                }

                int swapIndexRight = -1;
                boolean swapIndexRightStop = false;
                boolean alreadyFlippedRight = false;
                int swapIndexLeft = -1;
                boolean swapIndexLeftStop = false;
                boolean alreadyFlippedLeft = false;

                for(int i = dwellerIndex; !swapIndexRightStop; ++i) {
                    if (i == 8) {
                        i = 0;
                        alreadyFlippedRight = true;
                    }

                    if (i == 7 && alreadyFlippedRight) {
                        swapIndexRightStop = true;
                    }

                    if (openingArray[i]) {
                        swapIndexRight = i;
                        break;
                    }
                }

                for(int i = dwellerIndex; !swapIndexLeftStop; --i) {
                    if (i == -1) {
                        i = 7;
                        alreadyFlippedLeft = true;
                    }

                    if (i == 0 && alreadyFlippedLeft) {
                        swapIndexLeftStop = true;
                    }

                    if (openingArray[i]) {
                        swapIndexLeft = i;
                        break;
                    }
                }

                goingRight = this.rand.nextBoolean();
                alreadyFlippedRight = false;
                alreadyFlippedLeft = false;
                swapIndexRightStop = false;
                swapIndexLeftStop = false;
                boolean couldClimb = false;
                boolean checkRight = false;
                boolean checkLeft = false;
                if (goingRight) {
                    for(int i = dwellerIndex; !swapIndexRightStop; ++i) {
                        if (i == 8) {
                            i = 0;
                            alreadyFlippedRight = true;
                        }

                        if (i == 7 && alreadyFlippedRight) {
                            swapIndexRightStop = true;
                        }

                        if (!climbableArray[i] && i != dwellerIndex) {
                            break;
                        }

                        if (i == swapIndexRight) {
                            couldClimb = true;
                            this.newClimbAroundPos = new Vec3((double)blockPosClimbArray[i].getX(), (double)blockPosClimbArray[i].getY(), (double)blockPosClimbArray[i].getZ());
                            swapIndexLeftStop = true;
                        }
                    }

                    for(int i = dwellerIndex; !swapIndexLeftStop; --i) {
                        if (i == -1) {
                            i = 7;
                            alreadyFlippedLeft = true;
                        }

                        if (i == 0 && alreadyFlippedLeft) {
                            swapIndexLeftStop = true;
                        }

                        if (!climbableArray[i] && i != dwellerIndex) {
                            break;
                        }

                        if (i == swapIndexLeft) {
                            couldClimb = true;
                            this.newClimbAroundPos = new Vec3((double)blockPosClimbArray[i].getX(), (double)blockPosClimbArray[i].getY(), (double)blockPosClimbArray[i].getZ());
                            swapIndexRightStop = true;
                        }
                    }
                } else {
                    for(int i = dwellerIndex; !swapIndexLeftStop; --i) {
                        if (i == -1) {
                            i = 7;
                            alreadyFlippedLeft = true;
                        }

                        if (i == 0 && alreadyFlippedLeft) {
                            swapIndexLeftStop = true;
                        }

                        if (!climbableArray[i] && i != dwellerIndex) {
                            break;
                        }

                        if (i == swapIndexLeft) {
                            couldClimb = true;
                            this.newClimbAroundPos = new Vec3((double)blockPosClimbArray[i].getX(), (double)blockPosClimbArray[i].getY(), (double)blockPosClimbArray[i].getZ());
                            swapIndexRightStop = true;
                        }
                    }

                    for(int i = dwellerIndex; !swapIndexRightStop; ++i) {
                        if (i == 8) {
                            i = 0;
                            alreadyFlippedRight = true;
                        }

                        if (i == 7 && alreadyFlippedRight) {
                            swapIndexRightStop = true;
                        }

                        if (!climbableArray[i] && i != dwellerIndex) {
                            break;
                        }

                        if (i == swapIndexRight) {
                            couldClimb = true;
                            this.newClimbAroundPos = new Vec3((double)blockPosClimbArray[i].getX(), (double)blockPosClimbArray[i].getY(), (double)blockPosClimbArray[i].getZ());
                            swapIndexLeftStop = true;
                        }
                    }
                }

                if (!couldClimb) {
                    this.stopClimbing();
                } else {
                    this.climbStartVec = new Vec3(this.newClimbAroundPos.x + (double)0.4F, this.climbStartVec.y, this.newClimbAroundPos.z + (double)0.4F);
                    this.cavedweller.setYRot((float)Math.toDegrees(Math.atan2((double)this.climbPos.getX() - this.climbStartVec.x, (double)this.climbPos.getZ() - this.climbStartVec.z)) % 360.0F);
                }
            }
        } else {
            this.cavedweller.setPos((double)this.climbPos.getX(), (double)this.climbPos.getY(), (double)this.climbPos.getZ());
            this.stopClimbing();
        }

    }

    private boolean checkIfSpotIsClimbSwappable(BlockPos pPos) {
        return this.cavedweller.level().getBlockState(new BlockPos(pPos.getX(), pPos.getY() - 1, pPos.getZ())).isAir() && this.cavedweller.level().getBlockState(new BlockPos(pPos.getX(), pPos.getY() - 2, pPos.getZ())).isAir();
    }

    private BlockPos[] createBlockPosClimbArray(BlockPos origin) {
        BlockPos[] tempBlockArray = new BlockPos[8];
        tempBlockArray[0] = new BlockPos(origin.getX() - 1, origin.getY(), origin.getZ() - 1);
        tempBlockArray[1] = new BlockPos(origin.getX() + 0, origin.getY(), origin.getZ() - 1);
        tempBlockArray[2] = new BlockPos(origin.getX() + 1, origin.getY(), origin.getZ() - 1);
        tempBlockArray[3] = new BlockPos(origin.getX() + 1, origin.getY(), origin.getZ() + 0);
        tempBlockArray[4] = new BlockPos(origin.getX() + 1, origin.getY(), origin.getZ() + 1);
        tempBlockArray[5] = new BlockPos(origin.getX() + 0, origin.getY(), origin.getZ() + 1);
        tempBlockArray[6] = new BlockPos(origin.getX() - 1, origin.getY(), origin.getZ() + 1);
        tempBlockArray[7] = new BlockPos(origin.getX() - 1, origin.getY(), origin.getZ() + 0);
        return tempBlockArray;
    }

    private boolean[] createClimbableArray(BlockPos[] climbArray) {
        boolean[] tempClimbableArray = new boolean[8];
        int climbableI = 0;

        for(BlockPos pos : climbArray) {
            tempClimbableArray[climbableI] = this.checkIfSpotIsClimbSwappable(pos);
            ++climbableI;
        }

        return tempClimbableArray;
    }

    private boolean[] createOpeningArray(BlockPos[] climbArray) {
        boolean[] tempOpeningArray = new boolean[8];
        int openingI = 0;

        for(BlockPos pos : climbArray) {
            tempOpeningArray[openingI] = this.checkIfSpotIsOpening(pos);
            ++openingI;
        }

        return tempOpeningArray;
    }

    private boolean checkIfSpotIsOpening(BlockPos pPos) {
        return this.cavedweller.level().getBlockState(new BlockPos(pPos.getX(), pPos.getY(), pPos.getZ())).isAir();
    }

    public double getDistance(Vec3 a, Vec3 b) {
        double deltaX = a.x - b.x;
        double deltaY = a.y - b.y;
        double deltaZ = a.z - b.z;
        return Math.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ);
    }

    @Override
    public void tick() {
        this.cavedweller.squeezeCrawling = this.squeezing;
        LivingEntity livingentity = null;
        if (this.cavedweller.getTarget() != null) {
            livingentity = this.mob.getTarget();
        }

        this.tickAggroClock();
        if (!this.squeezing && !this.climbing) {
            if (this.cavedweller.isAggro) {
                this.mob.getLookControl().setLookAt(livingentity, 90.0F, 90.0F);
            } else {
                this.mob.getLookControl().setLookAt(livingentity, 180.0F, 1.0F);
            }
        }

        SynchedEntityData caveDwellerSynchedEntityData = this.cavedweller.getEntityData();
        if (caveDwellerSynchedEntityData.get(CaveDwellerEntity.AGGRO_ACCESSOR)) {
            if (this.squeezing || caveDwellerSynchedEntityData.get(CaveDwellerEntity.SQUEEZING_ACCESSOR) || isStartSqueezingOrSqueezingTickRunning) {
                this.squeezingTick();
                System.out.println("[DwellerChaseGoal] tick(): this.squeezing == true (" + this.squeezing + ",) CaveDwellerEntity.SQUEEZING_ACCESSOR == true (" + caveDwellerSynchedEntityData.get(CaveDwellerEntity.SQUEEZING_ACCESSOR) + ",) or isStartSqueezingOrSqueezingTickRunning == true (" + isStartSqueezingOrSqueezingTickRunning + "); called squeezingTick()");
                this.climbing = false;
            } else if (this.climbing) {
                this.climbingTick();
                System.out.println("[DwellerChaseGoal] tick(): this.climbing == true; called climbingTick()");
            } else {
                // For some reason this gets called even when this.squeezing is true, and even when
                // startSqueezing() is running.
                this.aggroTick();
                System.out.println("[DwellerChaseGoal] tick(): this.squeezing && this.climbing == false; called aggroTick()");
            }
        }

        --this.currentTicksTillLeave;
        if (this.currentTicksTillLeave <= 0 && (!this.isPlayerLookingTowards() || !this.inPlayerLineOfSight())) {
            this.cavedweller.discard();
        }

        this.currentBlock = new BlockPos((int)Math.floor(this.cavedweller.getX()), (int)Math.floor(this.cavedweller.getY()), (int)Math.floor(this.cavedweller.getZ()));
        if (!this.currentBlock.equals(this.oldBlock)) {
            for(int dX = -this.torchDestructionRadius; dX < this.torchDestructionRadius + 1; ++dX) {
                for(int dY = -this.torchDestructionRadius; dY < this.torchDestructionRadius + 1; ++dY) {
                    for(int dZ = -this.torchDestructionRadius; dZ < this.torchDestructionRadius + 1; ++dZ) {
                        this.checkBlockForTorch = new BlockPos(this.currentBlock.getX() + dX, this.currentBlock.getY() + dY, this.currentBlock.getZ() + dZ);
                        if (this.cavedweller.level().getBlockState(this.checkBlockForTorch).is(Blocks.TORCH)) {
                            this.cavedweller.level().destroyBlock(this.checkBlockForTorch, true);
                        } else if (this.cavedweller.level().getBlockState(this.checkBlockForTorch).is(Blocks.WALL_TORCH)) {
                            this.cavedweller.level().destroyBlock(this.checkBlockForTorch, true);
                        } else if (this.cavedweller.level().getBlockState(this.checkBlockForTorch).is(Blocks.SOUL_TORCH)) {
                            this.cavedweller.level().destroyBlock(this.checkBlockForTorch, true);
                        } else if (this.cavedweller.level().getBlockState(this.checkBlockForTorch).is(Blocks.SOUL_WALL_TORCH)) {
                            this.cavedweller.level().destroyBlock(this.checkBlockForTorch, true);
                        }
                    }
                }
            }
        }

        this.oldBlock = this.currentBlock;

    }

    public boolean isPlayerLookingTowards() {
        LivingEntity pendingTarget = this.cavedweller.getTarget();
        Minecraft minecraft = Minecraft.getInstance();
        boolean yawPlayerLookingTowards = false;
        float fov = (float)(Integer)minecraft.options.fov().get();
        float yFovMod = 0.65F;
        float fovMod = (35.0F / fov - 1.0F) * 0.4F + 1.0F;
        fov *= fovMod;
        Vec3 a = pendingTarget.position();
        Vec3 b = this.cavedweller.position();
        Vec2 dist = new Vec2((float)b.x - (float)a.x, (float)b.z - (float)a.z);
        dist = dist.normalized();
        double newAngle = Math.toDegrees(Math.atan2((double)dist.x, (double)dist.y));
        float lookX = (float)pendingTarget.getViewVector(1.0F).x;
        float lookZ = (float)pendingTarget.getViewVector(1.0F).z;
        double newLookAngle = Math.toDegrees(Math.atan2((double)lookX, (double)lookZ));
        double newNewAngle = this.loopAngle(newAngle - newLookAngle) + (double)fov;
        newNewAngle = this.loopAngle(newNewAngle);
        if (newNewAngle > 0.0D && newNewAngle < (double)(fov * 2.0F)) {
            yawPlayerLookingTowards = true;
        }

        boolean pitchPlayerLookingTowards = false;
        boolean shouldOnlyUsePitch = false;
        float yFov = fov * yFovMod;
        Vec2 yDist = new Vec2((float)Math.sqrt((b.x - a.x) * (b.x - a.x) + (b.z - a.z) * (b.z - a.z)), (float)(b.y - a.y));
        yDist = yDist.normalized();
        double yAngle = Math.toDegrees(Math.atan2((double)yDist.x, (double)yDist.y));
        float lookY = (float)pendingTarget.getViewVector(1.0F).y;
        Vec2 lookDist = new Vec2((float)Math.sqrt((double)(lookX * lookX + lookZ * lookZ)), lookY);
        lookDist = lookDist.normalized();
        double yLookAngle = Math.toDegrees(Math.atan2((double)lookDist.x, (double)lookDist.y));
        double newYAngle = this.loopAngle(yAngle - yLookAngle) + (double)yFov;
        newYAngle = this.loopAngle(newYAngle);
        if (newYAngle > (double)0.0F && newYAngle < (double)(yFov * 2.0F)) {
            pitchPlayerLookingTowards = true;
        }

        if (!(yLookAngle < (double)(180.0F - yFov)) || !(yLookAngle > (double)yFov)) {
            shouldOnlyUsePitch = true;
        }

        return (yawPlayerLookingTowards || shouldOnlyUsePitch) && pitchPlayerLookingTowards;
    }

    public double loopAngle(double angle) {
        if (angle > (double)360.0F) {
            double var4;
            return var4 = angle - (double)360.0F;
        } else {
            double var3;
            return angle < (double)0.0F ? (var3 = angle + (double)360.0F) : angle;
        }
    }

    public boolean inPlayerLineOfSight() {
        LivingEntity pendingTarget = this.cavedweller.getTarget();
        return pendingTarget != null ? pendingTarget.canAttack(this.cavedweller) : false;
    }

    protected void checkAndPerformAttack(LivingEntity pEnemy, double pDistToEnemySqr) {
        double d0 = this.getAttackReachSqr(pEnemy);
        if (pDistToEnemySqr <= d0 && this.ticksUntilNextAttack <= 0) {
            this.resetAttackCooldown();
            this.mob.swing(InteractionHand.MAIN_HAND);
            this.mob.doHurtTarget(pEnemy);
            pEnemy.hurt(this.mob.damageSources().mobAttack(this.mob), (float)this.cavedweller.getAttributeValue(Attributes.ATTACK_DAMAGE));
            if (pEnemy instanceof net.minecraft.world.entity.player.Player player) {
                if (player.isBlocking()) {
                    player.getCooldowns().addCooldown(Items.SHIELD, 100);
                    player.stopUsingItem();
                    player.level().broadcastEntityEvent(player, (byte)30);
                }
            }
        }
    }

    protected void resetAttackCooldown() {
        this.ticksUntilNextAttack = this.adjustedTickDelay(20);
    }

    protected boolean isTimeToAttack() {
        return this.ticksUntilNextAttack <= 0;
    }

    protected int getTicksUntilNextAttack() {
        return this.ticksUntilNextAttack;
    }

    protected int getAttackInterval() {
        return this.adjustedTickDelay(20);
    }

    protected double getAttackReachSqr(LivingEntity pAttackTarget) {
        return (double)(this.mob.getBbWidth() * 4.0F * this.mob.getBbWidth() * 4.0F + pAttackTarget.getBbWidth());
    }
}
