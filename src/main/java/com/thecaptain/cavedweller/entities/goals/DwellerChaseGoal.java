package com.thecaptain.cavedweller.entities.goals;

import java.util.EnumSet;

import com.thecaptain.cavedweller.CaveDweller;
import com.thecaptain.cavedweller.entities.CaveDwellerEntity;
import com.thecaptain.cavedweller.util.Utils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Path;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.Vec3;

public class DwellerChaseGoal extends Goal {
    private final CaveDwellerEntity caveDweller;
    private final boolean followTargetEvenIfNotSeen;
    private long lastGameTimeCheck;
    private int ticksUntilLeave;
    private int ticksUntilNextAttack;
    private float ticksUntilCanAttack;
    private BlockPos lastCheckedBlockPos = BlockPos.ZERO;
    private final int torchDestructionRadius = 1;
    private double movementSpeed = CaveDweller.CONFIG.MOVEMENT_SPEED();
    private boolean squeezing;
    private double pathedTargetX;
    private double pathedTargetY;
    private double pathedTargetZ;
    private int failedPathFindingPenalty = 0;
    private boolean canPenalize = false;
    private double speedInLavaPerTick;
    private BlockPos breakingBlockPos = null;
    private float blockBreakProgress = 0.0F;
    private boolean shouldUseShortPath = false;
    private boolean shouldUseClimbPath = false;
    private boolean climbing = false;
    private Path shortPath;
    private Path climbPath;
    private Vec3 vecNodePos;
    private BlockPos climbPos;
    private boolean climbPathAvailable;
    private boolean shortPathAvailable;
    private boolean normalPathAvailable;
    private float climbRelativeY = 0.0F;
    private float climbTicks = 0.0F;
    private final float climbSpeed = 4.0F;
    private final int maxClimb = 50;
    private int climbInt = 0;
    private Vec3 climbStartVec;
    Vec3 newClimbAroundPos = new Vec3((double)0.0F, (double)0.0F, (double)0.0F);
    private final net.minecraft.util.RandomSource rand = net.minecraft.util.RandomSource.create();
    BlockPos currentBlock = new BlockPos(0, 0, 0);
    BlockPos oldBlock = new BlockPos(0, 0, 0);
    private BlockPos nodePos;
    private int ticksUntilNextPathRecalculation;
    private boolean isWallWet;

    public DwellerChaseGoal(CaveDwellerEntity caveDweller, boolean followTargetEvenIfNotSeen, float ticksUntilCanAttack) {
        this.caveDweller = caveDweller;
        this.followTargetEvenIfNotSeen = followTargetEvenIfNotSeen;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        this.ticksUntilCanAttack = ticksUntilCanAttack;
        this.vecNodePos = null;
        this.nodePos = null;
        this.ticksUntilLeave = Utils.secondsToTicks(CaveDweller.CONFIG.TIME_UNTIL_LEAVE_CHASE());
        this.isWallWet = false;
    }

    @Override
    public boolean canUse() {
        if (this.caveDweller.isInvisible()) {
            return false;
        } else if (this.caveDweller.stalking) {
            return false;
        } else if (this.caveDweller.currentRoll != Roll.CHASE) {
            return false;
        } else {
            // Check once every second
            long ticks = this.caveDweller.level().getGameTime();
            if (ticks - this.lastGameTimeCheck < 20L) {
                return false;
            } else {
                this.lastGameTimeCheck = ticks;
                LivingEntity target = this.caveDweller.getTarget();
                if (!Utils.isValidPlayer(target)) {
                    return false;
                } else {
                    Path path = this.caveDweller.getNavigation().createPath(target, 0);
                    if (path != null) {
                        return true;
                    } else {
                        boolean canAttack = this.getAttackReachSqr(target) >= this.caveDweller.distanceToSqr(target);
                        if (canAttack) {
                            return true;
                        } else {
                            if (!this.caveDweller.isSubmerged) {
                                this.caveDweller.getEntityData().set(CaveDwellerEntity.SQUEEZING_ACCESSOR, true);
                                this.caveDweller.refreshDimensions();
                            }
                            this.caveDweller.getEntityData().set(CaveDwellerEntity.SQUEEZING_ACCESSOR, false);
                            path = this.caveDweller.getNavigation().createPath(target, 0);
                            return path != null;
                        }
                    }
                }
            }
        }
    }

    @Override
    public boolean canContinueToUse() {
        LivingEntity target = this.caveDweller.getTarget();
        if (!Utils.isValidPlayer(target)) {
            if (CaveDweller.CONFIG.DISAPPEAR()) {
                this.caveDweller.disappear();
            }
            return false;
        } else if (!this.followTargetEvenIfNotSeen) {
            return !this.caveDweller.getNavigation().isDone();
        } else {
            return this.caveDweller.isWithinRestriction(target.blockPosition());
        }
    }

    @Override
    public void start() {
        this.caveDweller.setNoGravity(false);
        this.caveDweller.noPhysics = false;
        this.caveDweller.stalking = false;
        this.caveDweller.getEntityData().set(CaveDwellerEntity.STALKING_ACCESSOR, false);
        this.caveDweller.setAggressive(true);
        this.ticksUntilNextAttack = 0;
        this.caveDweller.playChaseSound();
        this.caveDweller.pleaseStopMoving = true;
        this.caveDweller.setDeltaMovement(Vec3.ZERO);
    }

    @Override
    public void stop() {
        this.climbing = false;
        this.caveDweller.getEntityData().set(CaveDwellerEntity.CLIMBING_ACCESSOR, false);
        this.caveDweller.setNoGravity(false);
        this.caveDweller.noPhysics = false;

        LivingEntity target = this.caveDweller.getTarget();
        if (!Utils.isValidPlayer(target)) {
            this.caveDweller.setTarget((LivingEntity) null);
        }

        if (this.breakingBlockPos != null) {
            this.caveDweller.level().destroyBlockProgress(this.caveDweller.getId(), this.breakingBlockPos, -1);
            this.breakingBlockPos = null;
        }

        this.caveDweller.getEntityData().set(CaveDwellerEntity.AGGRO_ACCESSOR, false);
        this.caveDweller.refreshDimensions();
        this.caveDweller.getNavigation().stop();
        super.stop();

    }

    /// Main tick
    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    public void tickAggroClock() {
        --this.ticksUntilCanAttack;
        if (this.ticksUntilCanAttack <= 0.0F) {
            this.caveDweller.getEntityData().set(CaveDwellerEntity.AGGRO_ACCESSOR, true);
        }

        this.caveDweller.isAggro = true;
    }

    @Override
    public void tick() {

        if (this.caveDweller.initializationDelayTicks > 0) {
            this.caveDweller.initializationDelayTicks--;

            this.caveDweller.playChaseSound();
            this.caveDweller.pleaseStopMoving = true;
            this.caveDweller.setDeltaMovement(Vec3.ZERO);

            if (this.caveDweller.initializationDelayTicks <= 0) {
                this.caveDweller.pleaseStopMoving = false;
            }

            return;
        }

        this.caveDweller.setInStandoff(false);


        LivingEntity target = null;

        if (this.caveDweller.getTarget() != null) {
            target = this.caveDweller.getTarget();
        }

        this.tickAggroClock();

        if (this.ticksUntilLeave <= 0 && !this.caveDweller.targetIsLookingAtMe) {
            this.caveDweller.disappear();
            return;
        }

        if (!squeezing) {
            if (this.caveDweller.isAggro) {
                this.caveDweller.getLookControl().setLookAt(target, 90.0F, 90.0F);
            }

            this.movementSpeed = (CaveDweller.CONFIG.MOVEMENT_SPEED());

        } else {
            this.movementSpeed = (CaveDweller.CONFIG.MOVEMENT_SPEED() * 1.2);
        }

        if (this.caveDweller.getEntityData().get(CaveDwellerEntity.AGGRO_ACCESSOR)) {
            if (this.squeezing) {
                this.climbing = false;
            } else if (this.climbing) {
                this.climbingTick();
            } else {
                this.aggroTick();
            }
        }

        if (Utils.isValidPlayer(target)) {
            if (CaveDweller.CONFIG.GIVE_DARKNESS()) {
                target.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 100, 0, false, false));
            }

            Path path = this.caveDweller.getNavigation().getPath();
            if (path == null || path.isDone() || path.getEndNode() == null || path.getEndNode().asBlockPos().distSqr(target.blockPosition()) > 0.25D) {
                path = this.caveDweller.getNavigation().createPath(target, 0);
            }

            boolean squeezing = false;
            boolean shouldClimb = target.getY() > this.caveDweller.getY() + 2.0D;
            if ((!shouldClimb) && (path == null || path.isDone() || path.getNodeCount() == 1)) {
                squeezing = true;
                this.caveDweller.getEntityData().set(CaveDwellerEntity.SQUEEZING_ACCESSOR, true);
                this.caveDweller.refreshDimensions();
                path = this.caveDweller.getNavigation().createPath(target, 0);
            } else if (this.caveDweller.isSubmerged) {
                //System.out.println("Tried to squeeze, but can't because of liquid!");
                this.caveDweller.getEntityData().set(CaveDwellerEntity.SQUEEZING_ACCESSOR, false);
                this.caveDweller.refreshDimensions();
                path = this.caveDweller.getNavigation().createPath(target, 0);
            }

            if (path != null && !path.isDone()) {
                boolean isAboveSolid = !this.caveDweller.level().getBlockState(this.caveDweller.blockPosition().above()).isAir();
                boolean isNextAboveSolid = !this.caveDweller.level().getBlockState(path.getNextNodePos().above()).isAir();
                boolean extraCheck = this.caveDweller.getEntityData().get(CaveDwellerEntity.CROUCHING_ACCESSOR);
                extraCheck = extraCheck && path.getNextNodePos().getY() > this.caveDweller.blockPosition().getY();

                squeezing = (isAboveSolid || isNextAboveSolid || extraCheck) && !this.caveDweller.isSubmerged;
                this.caveDweller.getEntityData().set(CaveDwellerEntity.SQUEEZING_ACCESSOR, squeezing);
                this.caveDweller.refreshDimensions();
            }

            // TODO: Reuse for "catchu_up" anim?
            /*
            double movementSpeed = 0.85 / (double) this.maxSpeedReached * (double) this.speedUp;
            this.caveDweller.getNavigation().moveTo(path, 1.0);

            if (this.speedUp < this.maxSpeedReached) {
                ++this.speedUp;
            }
            */
            this.caveDweller.getNavigation().moveTo(path, 1.0);

            this.tickBlockDestructionEngine();

            --this.ticksUntilLeave;
            this.ticksUntilNextPathRecalculation = this.adjustedTickDelay(this.ticksUntilNextPathRecalculation);
        }
    }

    public void aggroTick() {
        this.caveDweller.playChaseSound();
        this.caveDweller.noPhysics = false;
        this.caveDweller.setNoGravity(false);
        LivingEntity target = this.caveDweller.getTarget();
        if (this.caveDweller.getNavigation().getPath() != null) {
            BlockPos tempClimbPos = this.checkIfShouldClimbAndReturnPos(this.shortPath);
            if (tempClimbPos != null && (this.shouldUseShortPath && CaveDweller.CONFIG.CAN_CLIMB())) {
                this.startClimbing(tempClimbPos);
                return;
            }

            if (target != null) {
                double distanceToTarget = this.getAttackReachSqr(target);
                this.ticksUntilNextPathRecalculation = Math.max(this.ticksUntilNextPathRecalculation - 1, 0);
                if ((this.followTargetEvenIfNotSeen
                        || this.caveDweller.getSensing().hasLineOfSight(target) && this.ticksUntilNextPathRecalculation <= 0 && (this.pathedTargetX == (double) 0.0F && this.pathedTargetY == (double) 0.0F && this.pathedTargetZ == (double) 0.0F
                        || target.distanceToSqr(this.pathedTargetX, this.pathedTargetY, this.pathedTargetZ) >= 1.0D
                        || this.caveDweller.getRandom().nextFloat() < 0.05F))) {
                    this.pathedTargetX = target.getX();
                    this.pathedTargetY = target.getY();
                    this.pathedTargetZ = target.getZ();
                    this.ticksUntilNextPathRecalculation = 2;
                    if (this.canPenalize) {
                        this.ticksUntilNextPathRecalculation += this.failedPathFindingPenalty;
                        if (this.caveDweller.getNavigation().getPath() != null) {
                            net.minecraft.world.level.pathfinder.Node finalPathPoint = this.caveDweller.getNavigation().getPath().getEndNode();
                            if (finalPathPoint != null && target.distanceToSqr((double) finalPathPoint.x, (double) finalPathPoint.y, (double) finalPathPoint.z) < (double) 1.0F) {
                                this.failedPathFindingPenalty = 0;
                            } else {
                                this.failedPathFindingPenalty += 10;
                            }
                        } else {
                            this.failedPathFindingPenalty += 10;
                        }
                    }

                    this.getShortPath(target);
                    if (this.shortPath != null) {
                        net.minecraft.world.level.pathfinder.Node finalShortPathPoint = this.shortPath.getEndNode();
                        if (finalShortPathPoint != null && target.distanceToSqr((double)finalShortPathPoint.x, (double)finalShortPathPoint.y, (double)finalShortPathPoint.z) < (double)2.0F) {
                            this.shortPathAvailable = true;
                        } else {
                            this.shortPathAvailable = false;
                        }
                    } else {
                        this.shortPathAvailable = false;
                    }

                    this.getClimbPath(target);
                    if (this.climbPath != null) {
                        net.minecraft.world.level.pathfinder.Node finalClimbPathPoint = this.shortPath.getEndNode();
                        if (finalClimbPathPoint != null && target.distanceToSqr((double)finalClimbPathPoint.x, (double)finalClimbPathPoint.y, (double)finalClimbPathPoint.z) < (double)1.0F) {
                            this.climbPathAvailable = true;
                        } else {
                            this.climbPathAvailable = false;
                        }
                    } else {
                        this.climbPathAvailable = false;
                    }

                    this.shouldUseShortPath = this.shortPathAvailable;
                    this.shouldUseClimbPath = !this.shortPathAvailable && this.climbPathAvailable && !this.normalPathAvailable;
                    if (distanceToTarget > (double)1024.0F) {
                        this.ticksUntilNextPathRecalculation += 10;
                    } else if (distanceToTarget > (double)256.0F) {
                        this.ticksUntilNextPathRecalculation += 5;
                    }

                    if (!this.shouldUseShortPath && !this.shouldUseClimbPath) {
                        if (!this.caveDweller.getNavigation().moveTo(target, this.movementSpeed)) {
                            this.caveDweller.startedMovingChase = true;
                            this.ticksUntilNextPathRecalculation += 8;
                        }
                    } else if (this.shouldUseShortPath) {
                        if (!this.caveDweller.getNavigation().moveTo(this.shortPath, this.movementSpeed)) {
                            this.caveDweller.startedMovingChase = true;
                            this.ticksUntilNextPathRecalculation += 8;
                        }
                    } else if (this.shouldUseClimbPath && !this.caveDweller.getNavigation().moveTo(this.climbPath, this.movementSpeed)) {
                        this.caveDweller.startedMovingChase = true;
                        this.ticksUntilNextPathRecalculation += 8;
                    }

                    this.ticksUntilNextPathRecalculation = this.adjustedTickDelay(this.ticksUntilNextPathRecalculation);
                }

                this.ticksUntilNextAttack = Math.max(this.ticksUntilNextAttack - 1, 0);
                double distance = this.caveDweller.distanceToSqr(target);
                this.checkAndPerformAttack(target, distance);
            }

            // TODO: replace with water logic?
            if (this.caveDweller.isInLava() && this.caveDweller.getNavigation().getPath() != null && this.caveDweller.getNavigation().getPath().getNextNodeIndex() < this.caveDweller.getNavigation().getPath().getNodeCount()) {
                System.out.println("ticking lava move");
                Vec3 a = this.caveDweller.position();
                BlockPos b = this.caveDweller.getNavigation().getPath().getNextNodePos();
                Vec3 dir = (new Vec3((double) b.getX() - a.x, (double) b.getY() - a.y, (double) b.getZ() - a.z)).normalize();
                double dist = dir.length();
                this.speedInLavaPerTick = 0.2;
                if (dist > this.speedInLavaPerTick) {
                    this.caveDweller.setPos(this.caveDweller.position().add(new Vec3(dir.x * this.speedInLavaPerTick, dir.y * this.speedInLavaPerTick, dir.z * this.speedInLavaPerTick)));
                } else {
                    this.caveDweller.setPos(new Vec3((double) b.getX(), (double) b.getY(), (double) b.getZ()));
                }
            }

        }
    }

    /// Block breaking
    // TODO: Change to Bedrock system of burning out torches
    private void tickBlockDestructionEngine() {
        BlockPos currentBlockPos = new BlockPos((int)Math.floor(this.caveDweller.getX()), (int)Math.floor(this.caveDweller.getY()), (int)Math.floor(this.caveDweller.getZ()));
        if (currentBlockPos.equals(this.lastCheckedBlockPos) && this.breakingBlockPos == null) {
            return;
        }

        if (this.breakingBlockPos == null) {
            blockSearchLoop:
            for (int dX = -this.torchDestructionRadius; dX <= this.torchDestructionRadius; ++dX) {
                for (int dY = -this.torchDestructionRadius; dY <= this.torchDestructionRadius; ++dY) {
                    for (int dZ = -this.torchDestructionRadius; dZ <= this.torchDestructionRadius; ++dZ) {
                        BlockPos targetBlockPos = currentBlockPos.offset(dX, dY, dZ);
                        BlockState blockstate = this.caveDweller.level().getBlockState(targetBlockPos);

                        if (blockstate.is(Blocks.TORCH)
                                || blockstate.is(Blocks.WALL_TORCH)
                                || blockstate.is(Blocks.SOUL_TORCH)
                                || blockstate.is(Blocks.SOUL_WALL_TORCH)
                                || blockstate.is(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.BLOCK, new net.minecraft.resources.ResourceLocation("doors")))
                                || blockstate.is(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.BLOCK, new net.minecraft.resources.ResourceLocation("trapdoors")))
                                || blockstate.is(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.BLOCK, new net.minecraft.resources.ResourceLocation("c", "glass_blocks")))
                                || blockstate.is(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.BLOCK, new net.minecraft.resources.ResourceLocation("c", "glass_panes"))))
                        {
                            if (blockstate.is(Blocks.TORCH) || blockstate.is(Blocks.WALL_TORCH) || blockstate.is(Blocks.SOUL_TORCH) || blockstate.is(Blocks.SOUL_WALL_TORCH)) {
                                this.caveDweller.level().destroyBlock(targetBlockPos, true, this.caveDweller);
                            } else {
                                this.breakingBlockPos = targetBlockPos;
                                this.blockBreakProgress = 0.0F;
                                break blockSearchLoop;
                            }
                        }
                    }
                }
            }
            this.lastCheckedBlockPos = currentBlockPos;
        }

        if (this.breakingBlockPos != null) {
            BlockState targetState = this.caveDweller.level().getBlockState(this.breakingBlockPos);

            if (targetState.isAir()) {
                this.caveDweller.level().destroyBlockProgress(this.caveDweller.getId(), this.breakingBlockPos, -1);
                this.breakingBlockPos = null;
                return;
            }

            this.caveDweller.getNavigation().stop();
            this.caveDweller.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
            this.blockBreakProgress += 0.3F;
            int visualProgressIndex = (int) (this.blockBreakProgress * 10.0F);
            this.caveDweller.level().destroyBlockProgress(this.caveDweller.getId(), this.breakingBlockPos, visualProgressIndex);

            if (this.blockBreakProgress >= 1.0F) {
                this.caveDweller.level().destroyBlockProgress(this.caveDweller.getId(), this.breakingBlockPos, -1);
                this.caveDweller.level().destroyBlock(this.breakingBlockPos, true, this.caveDweller);
                this.breakingBlockPos = null;
            }
        }
    }

    /// Climbing
    public void startClimbing(BlockPos climbPos) {
        this.isWallWet = !this.caveDweller.level().getFluidState(climbPos).isEmpty();
        if (this.caveDweller.isSubmerged || this.isWallWet) {
            //System.out.println("Tried to climb, but can't because of liquid!");
            return;
        }

        this.climbStartVec = this.caveDweller.position();
        this.climbRelativeY = 0.0F;
        this.climbTicks = 0.0F;
        this.climbInt = 0;
        this.climbPos = climbPos;
        this.climbing = true;
        this.caveDweller.getEntityData().set(CaveDwellerEntity.CLIMBING_ACCESSOR, true);
        this.caveDweller.getEntityData().set(CaveDwellerEntity.CLIMB_ANGLE_ACCESSOR, this.caveDweller.getYRot());
        this.caveDweller.wallDirection = Direction.getNearest((float)(this.climbPos.getX() - this.caveDweller.getX()), 0.0F, (float)(this.climbPos.getZ() - this.caveDweller.getZ()));
        this.caveDweller.getEntityData().set(CaveDwellerEntity.CLIMB_WALL_ACCESSOR, this.caveDweller.wallDirection);
        System.out.println("started climbing with pos: " + this.climbPos);
        System.out.println("started climbing on wall to the " + this.caveDweller.wallDirection);
    }

    public void stopClimbing() {
        this.climbing = false;
        this.caveDweller.getEntityData().set(CaveDwellerEntity.CLIMBING_ACCESSOR, false);
        this.caveDweller.setNoGravity(false);
        this.caveDweller.noPhysics = false;
    }

    public Path getClimbPath(LivingEntity target) {
        return this.climbPath = this.caveDweller.createClimbPath(target);
    }

    public Path getShortPath(LivingEntity target) {
        return this.shortPath = this.caveDweller.createShortPath(target);
    }

    // TODO: Stops moving after climbing in specific scenarios (like climbing up a 1*1 hole?)
    public void climbingTick() {
        this.caveDweller.playClimbSound();
        this.caveDweller.setNoGravity(true);
        this.caveDweller.noPhysics = true;
        // Had to add this line to prevent a crash...
        Path currentPath = this.caveDweller.getNavigation().getPath();
        // ...and changed requirements here
        if (currentPath != null && !currentPath.isDone() && currentPath.getNextNodeIndex() < currentPath.getNodeCount()) {
            this.nodePos = this.caveDweller.getNavigation().getPath().getNextNodePos();
        }

        this.caveDweller.getNavigation().stop();
        if (this.nodePos == null) {
            this.stopClimbing();
        }

        while(this.climbInt < this.maxClimb && !this.caveDweller.level().getBlockState(this.climbPos).isAir()) {
            this.climbPos = new BlockPos(this.climbPos.getX(), this.climbPos.getY() + 1, this.climbPos.getZ());
            ++this.climbInt;
        }

        if (this.caveDweller.position().y < (double)((float)this.climbPos.getY() - 2.0F)) {
            ++this.climbTicks;
            this.climbRelativeY = this.climbTicks / this.climbSpeed;
            Vec3 rotAxis = new Vec3((double)this.climbPos.getX() - this.climbStartVec.x, (double)0.0F, (double)this.climbPos.getZ() - this.climbStartVec.z);
            rotAxis = rotAxis.normalize();
            double rotAngle = Math.toDegrees(Math.atan2(-rotAxis.x, rotAxis.z));
            this.caveDweller.setYBodyRot((float)rotAngle);
            this.caveDweller.moveTo(this.climbStartVec.x, (double)this.climbRelativeY + this.climbStartVec.y, this.climbStartVec.z, (float)rotAngle, (float)rotAngle);
            BlockPos blockCheckHead = new BlockPos((int)Math.floor(this.climbStartVec.x), (int)Math.floor(this.climbStartVec.y + (double)this.climbRelativeY) + 2, (int)Math.floor(this.climbStartVec.z));
            if (!this.caveDweller.level().getBlockState(blockCheckHead).isAir()) {
                BlockPos spotToCreateArrayAround = new BlockPos(this.climbPos.getX(), blockCheckHead.getY(), this.climbPos.getZ());
                int blockAmountCovered = 0;

                for(int x = -1; x < 2; ++x) {
                    for(int z = -1; z < 2; ++z) {
                        if ((x != 0 || z != 0) && !this.caveDweller.level().getBlockState(new BlockPos(spotToCreateArrayAround.getX() + x, spotToCreateArrayAround.getY(), spotToCreateArrayAround.getZ() + z)).isAir()) {
                            ++blockAmountCovered;
                        }
                    }
                }

                if (blockAmountCovered >= 8) {
                    //System.out.println("blockAmountCovered >= 8; stopped climbing");
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
                    //System.out.println("!couldClimb; stopped climbing");
                    this.stopClimbing();
                } else {
                    this.climbStartVec = new Vec3(this.newClimbAroundPos.x + (double)0.4F, this.climbStartVec.y, this.newClimbAroundPos.z + 0.4);
                    this.caveDweller.setYRot((float)Math.toDegrees(Math.atan2((double)this.climbPos.getX() - this.climbStartVec.x, (double)this.climbPos.getZ() - this.climbStartVec.z)) % 360.0F);
                }
            }
        } else {
            this.caveDweller.setPos((double)this.climbPos.getX(), (double)this.climbPos.getY(), (double)this.climbPos.getZ());
            //float temp = this.climbPos.getY() - 2.0F;
            //System.out.println("(this.caveDweller.position().y ("+ this.caveDweller.position().y + ") !< this.climbPos.getY() - 2.0F (" + temp + "); stopped climbing");
            this.stopClimbing();
        }

    }

    public BlockPos checkIfShouldClimbAndReturnPos(Path pathToCheck) {
        if (pathToCheck == null) {
            //System.out.println("pathToCheck == null!");
            return null;
        } else {
            if (!pathToCheck.isDone()) {
                BlockPos blockpos = pathToCheck.getNextNodePos();
                //System.out.println("blockpos = " + blockpos);
                boolean flag = (double) blockpos.getY() > this.caveDweller.getY() + (double) 2.0F;
                //System.out.println("blockpos.getY: " + blockpos.getY() + ", caveDweller.getY + 2.0: " + (this.caveDweller.getY() + (double) 2.0F));
                //System.out.println("flag: " + (flag ? blockpos : null));
                return flag ? blockpos : null;
            } else {
                //System.out.println("pathToCheck.isDone!");
                return null;
            }
        }
    }

    private boolean checkIfSpotIsClimbSwappable(BlockPos pPos) {
        return this.caveDweller.level().getBlockState(new BlockPos(pPos.getX(), pPos.getY() - 1, pPos.getZ())).isAir() && this.caveDweller.level().getBlockState(new BlockPos(pPos.getX(), pPos.getY() - 2, pPos.getZ())).isAir();
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
        return this.caveDweller.level().getBlockState(new BlockPos(pPos.getX(), pPos.getY(), pPos.getZ())).isAir();
    }

    /// Attacking
    protected void checkAndPerformAttack(LivingEntity target, double distance) {
        double distanceToTarget = this.getAttackReachSqr(target);
        if (distance <= distanceToTarget && this.ticksUntilNextAttack <= 0) {
            this.resetAttackCooldown();
            this.caveDweller.swing(InteractionHand.MAIN_HAND);
            this.caveDweller.doHurtTarget(target);
            target.hurt(this.caveDweller.damageSources().mobAttack(this.caveDweller), (float)this.caveDweller.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE));
            // Disable shield if player is blocking
            if (target instanceof net.minecraft.world.entity.player.Player player) {
                if (player.isBlocking()) {
                    player.disableShield(true);
                }
            }
        }
    }

    private void resetAttackCooldown() {
        int attackSpeed = Utils.secondsToTicks((int) CaveDweller.CONFIG.ATTACK_RATE());
        this.ticksUntilNextAttack = this.adjustedTickDelay(attackSpeed);
    }

    protected double getAttackReachSqr(LivingEntity target) {
        float modifier = 3.0F;
        return (double)(this.caveDweller.getBbWidth() * modifier * this.caveDweller.getBbWidth() * modifier + target.getBbWidth());
    }

}