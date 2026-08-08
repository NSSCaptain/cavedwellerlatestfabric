package com.gargin.cavenoise.entity.custom;

import com.gargin.cavenoise.sound.ModSounds;
import java.util.Random;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import org.jetbrains.annotations.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animatable.instance.SingletonAnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.animation.Animation.LoopType;
import software.bernie.geckolib.core.object.PlayState;

public class CaveDwellerEntity extends Monster implements GeoEntity {
    private AnimatableInstanceCache cache = new SingletonAnimatableInstanceCache(this);
    public int rRollResult = 4;
    public boolean forcedStalk = false;
    public boolean isAggro;
    private float chanceOfSpawningAsStalker = 0.6F;
    private boolean isAggroState;
    private boolean returnShort = false;
    private boolean inTwoBlockSpace = false;
    public boolean spottedByPlayer = false;
    public boolean spottedOld = false;
    private boolean shouldClearAnim = true;
    public boolean squeezeCrawling = false;
    public boolean isFleeing;
    public boolean startedMovingChase = false;
    private float waitToStartAnimatorController = 20.0F;
    private Vec3 oldPos;
    private int ticksTillRemove;
    private float defaultMaxUpStep = 2.0F;
    private RawAnimation OLD_RUN;
    private RawAnimation IDLE;
    private RawAnimation CHASE;
    private RawAnimation CHASE_IDLE;
    private RawAnimation CROUCH_RUN;
    private RawAnimation CROUCH_IDLE;
    private RawAnimation CALM_RUN;
    private RawAnimation CALM_STILL;
    private RawAnimation IS_SPOTTED;
    private RawAnimation CRAWL;
    private RawAnimation FLEE;
    private RawAnimation STALK;
    private RawAnimation STALK_IDLE;
    private RawAnimation CLIMB;
    private RawAnimation currentAnim;
    public static final EntityDataAccessor<Boolean> FLEEING_ACCESSOR;
    public static final EntityDataAccessor<Boolean> CROUCHING_ACCESSOR;
    public static final EntityDataAccessor<Boolean> AGGRO_ACCESSOR;
    public static final EntityDataAccessor<Boolean> SQUEEZING_ACCESSOR;
    public static final EntityDataAccessor<Boolean> SPOTTED_ACCESSOR;
    public static final EntityDataAccessor<Boolean> CLIMBING_ACCESSOR;
    public static final EntityDataAccessor<Boolean> STALKING_ACCESSOR;
    public Logger logger;
    private float twoBlockSpaceCooldown;
    private float twoBlockSpaceTimer;
    private float movingCooldown;
    private float movingClock;
    private int chaseSoundClockReset;
    private int climbSoundClockReset;
    private int climbSoundClock;
    private int chaseSoundClock;
    private boolean alreadyPlayedFleeSound;
    private boolean alreadyPlayedSpottedSound;
    private boolean startedPlayingChaseSound;
    private boolean alreadyPlayedDeathSound;
    public boolean setCrawling;
    protected boolean isPassenger(Entity pVehicle) { return false; }
    public boolean hasSpawned;
    public boolean pleaseStopMoving;

    public CaveDwellerEntity(EntityType<? extends CaveDwellerEntity> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
        this.OLD_RUN = RawAnimation.begin().then("animation.cave_dweller.run", LoopType.LOOP);
        this.IDLE = RawAnimation.begin().then("animation.cave_dweller.idle", LoopType.LOOP);
        this.CHASE = RawAnimation.begin().then("animation.cave_dweller.new_run", LoopType.LOOP);
        this.CHASE_IDLE = RawAnimation.begin().then("animation.cave_dweller.run_idle", LoopType.LOOP);
        this.CROUCH_RUN = RawAnimation.begin().then("animation.cave_dweller.crouch_run_new", LoopType.LOOP);
        this.CROUCH_IDLE = RawAnimation.begin().then("animation.cave_dweller.crouch_idle", LoopType.LOOP);
        this.CALM_RUN = RawAnimation.begin().then("animation.cave_dweller.calm_move", LoopType.LOOP);
        this.CALM_STILL = RawAnimation.begin().then("animation.cave_dweller.calm_idle", LoopType.LOOP);
        this.IS_SPOTTED = RawAnimation.begin().then("animation.cave_dweller.spotted", LoopType.HOLD_ON_LAST_FRAME);
        this.CRAWL = RawAnimation.begin().then("animation.cave_dweller.crawl", LoopType.LOOP);
        this.FLEE = RawAnimation.begin().then("animation.cave_dweller.flee", LoopType.LOOP);
        this.STALK = RawAnimation.begin().then("animation.cave_dweller.stalking", LoopType.LOOP);
        this.STALK_IDLE = RawAnimation.begin().then("animation.cave_dweller.stalking_idle", LoopType.HOLD_ON_LAST_FRAME);
        this.CLIMB = RawAnimation.begin().then("animation.cave_dweller.climb", LoopType.LOOP);
        this.logger = LogManager.getLogManager().getLogger("cavenoise");
        this.twoBlockSpaceTimer = 0.0F;
        this.movingCooldown = 3.0F;
        this.movingClock = 3.0F;
        this.chaseSoundClockReset = 80;
        this.climbSoundClockReset = 10;
        this.climbSoundClock = 0;
        this.chaseSoundClock = 0;
        this.alreadyPlayedFleeSound = false;
        this.alreadyPlayedSpottedSound = false;
        this.startedPlayingChaseSound = false;
        this.alreadyPlayedDeathSound = false;
        this.setMaxUpStep(this.defaultMaxUpStep);
        this.reapplyPosition();
        this.twoBlockSpaceCooldown = 5.0F;
        this.oldPos = this.position();
        this.ticksTillRemove = 6000;
        ItemStack enchantedBoots = new ItemStack(Items.DIAMOND_BOOTS);
        enchantedBoots.enchant(Enchantments.DEPTH_STRIDER, 3);
        this.setItemSlot(EquipmentSlot.FEET, enchantedBoots);
        this.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 999999, 100, true, false));
        this.forcedStalk = true;
        this.setPathfindingMalus(net.minecraft.world.level.pathfinder.BlockPathTypes.BLOCKED, 0.0F);
        this.setPathfindingMalus(net.minecraft.world.level.pathfinder.BlockPathTypes.WALKABLE, 0.0F);

    }

    public static AttributeSupplier setAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 75.0D).add(Attributes.ATTACK_DAMAGE, 8.0D).add(Attributes.MOVEMENT_SPEED, 0.42D).add(Attributes.KNOCKBACK_RESISTANCE, 0.6D).add(Attributes.FOLLOW_RANGE, 100.0D).add(Attributes.ARMOR, 3.0D).build();
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(FLEEING_ACCESSOR, false);
        this.entityData.define(CROUCHING_ACCESSOR, false);
        this.entityData.define(AGGRO_ACCESSOR, false);
        this.entityData.define(SQUEEZING_ACCESSOR, false);
        this.entityData.define(SPOTTED_ACCESSOR, false);
        this.entityData.define(CLIMBING_ACCESSOR, false);
        this.entityData.define(STALKING_ACCESSOR, false);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new DwellerBreakInvisGoal(this));
        this.goalSelector.addGoal(3, new DwellerStareGoal(this, 100.0F));
        this.goalSelector.addGoal(4, new DwellerChaseGoal(this, this, 0.85D, true, 20.0F));
        this.goalSelector.addGoal(4, new DwellerFleeGoal(this, 20.0F, 1.0D));
        this.goalSelector.addGoal(4, new DwellerStalkGoal(this, 0.5D, 15.0F));
        this.goalSelector.addGoal(5, new DwellerStrollGoal(this, 0.7D));
        this.targetSelector.addGoal(1, new DwellerTargetTooCloseGoal(this, 12.0F));
        this.targetSelector.addGoal(2, new DwellerTargetSeesMeGoal(this));
    }

    // Causes dweller to burn in sunlight
    @Override
    public void aiStep() {
        boolean isSunBurning = this.isSunBurnTick();
        if (isSunBurning) {
            ItemStack itemStack = this.getItemBySlot(EquipmentSlot.HEAD);
            if (!itemStack.isEmpty()) {
                if (itemStack.isDamageableItem()) {
                    itemStack.setDamageValue(itemStack.getDamageValue() + this.random.nextInt(2));
                    if (itemStack.getDamageValue() >= itemStack.getMaxDamage()) {
                        this.broadcastBreakEvent(EquipmentSlot.HEAD);
                        this.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
                    }
                }
                isSunBurning = false;
            }

            if (isSunBurning) {
                this.setSecondsOnFire(8);
            }
        }
        super.aiStep();
    }

    public boolean hasSpawned() {
        return hasSpawned;
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor pLevel, DifficultyInstance pDifficulty, MobSpawnType pReason, @Nullable SpawnGroupData pSpawnData, @Nullable CompoundTag pDataTag) {
        SpawnGroupData data = super.finalizeSpawn(pLevel, pDifficulty, pReason, pSpawnData, pDataTag);
        BlockPos spawnCheckPos = this.blockPosition();
        BlockPos ceilingBlock = spawnCheckPos.above();
        if (!pLevel.getBlockState(ceilingBlock).isAir()) {
            this.getEntityData().set(SQUEEZING_ACCESSOR, true);
            this.refreshDimensions();
        }

        return data;
    }

    public Vec3 generatePos(Player victim) {
        Random rand = new Random();
        BlockPos playerPos = victim.blockPosition();
        for (int i = 0; i < 40; i++) {
            int offsetX = rand.nextInt(40) - 20;
            int offsetY = rand.nextInt(12) - 6;
            int offsetZ = rand.nextInt(40) - 20;

            BlockPos spawnCheckPos = playerPos.offset(offsetX, offsetY, offsetZ);
            if (spawnCheckPos.getY() < 40
                    && this.level().getBlockState(spawnCheckPos).isAir()
                    && !this.level().getBlockState(spawnCheckPos.below()).isAir()
                    && this.level().getBlockState(spawnCheckPos.below()).getFluidState().isEmpty()) {

                return Vec3.atBottomCenterOf(spawnCheckPos);
            }
        }
        BlockPos fallbackFloor = playerPos;
        while (fallbackFloor.getY() > this.level().getMinBuildHeight() && this.level().getBlockState(fallbackFloor).isAir()) {
            fallbackFloor = fallbackFloor.below();
        }
        return Vec3.atBottomCenterOf(fallbackFloor.above());
    }

    private Vec3i getDirectionVector() {
        return new Vec3i(getDirection().getStepX(), getDirection().getStepY(), getDirection().getStepZ());
    }

    @Override
    public void tick() {
        --this.ticksTillRemove;
        if (this.ticksTillRemove <= 0 && (!this.isPlayerLookingTowards() || !this.inPlayerLineOfSight())) {
            this.playDisappearSound();
            this.discard();
        }

        // Is the dweller squeezing? If it is, set height to 1 block. If not, set to 2 blocks
        // Set the height result to be "heightOffset"
        double heightOffset = this.getEntityData().get(SQUEEZING_ACCESSOR) ? 1.0D : 2.0D;
        BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos(
                this.position().x,
                this.position().y + heightOffset,
                this.position().z
        );
        BlockState blockstate = this.level().getBlockState(blockpos$mutableblockpos);
        // flag = true when the block the dweller is on/in is NOT air
        boolean flag = !blockstate.isAir();
        if (flag) {
            this.twoBlockSpaceTimer = this.twoBlockSpaceCooldown;
            this.inTwoBlockSpace = true;
        } else {
            --this.twoBlockSpaceTimer;
            if (this.twoBlockSpaceTimer <= 0.0F) {
                this.inTwoBlockSpace = false;
            }
        }

        if (this.isAggro || this.isFleeing) {
            this.shouldClearAnim = false;
            this.spottedByPlayer = false;
            this.entityData.set(SPOTTED_ACCESSOR, false);
        }

        this.entityData.set(CROUCHING_ACCESSOR, this.inTwoBlockSpace);
        if ((Boolean)this.entityData.get(SPOTTED_ACCESSOR)) {
            this.playSpottedSound();
        }

        Vec3i offset = getDirectionVector();
        boolean isAboveSolid = this.level().getBlockState(blockPosition().above()).isSolid();
        boolean isTwoAboveSolid = this.level().getBlockState(blockPosition().above(2)).isSolid();
        boolean isFacingSolid = this.level().getBlockState(blockPosition().relative(getDirection())).isSolid();
        boolean isOffsetFacingSolid = this.level().getBlockState(blockPosition().offset(offset)).isSolid();
        boolean isOffsetFacingAboveSolid = this.level().getBlockState(blockPosition().offset(offset).above()).isSolid();
        boolean shouldSqueeze = isAboveSolid || !isOffsetFacingSolid && isOffsetFacingAboveSolid || isFacingSolid && isTwoAboveSolid;

        setCrawling(shouldSqueeze);

        // Original squeeze check; causes dweller to get stuck
        /*
        if (this.getEntityData().get(SQUEEZING_ACCESSOR) && !this.inTwoBlockSpace) {
            BlockPos abovePos = this.blockPosition().above();
            if (this.level().getBlockState(abovePos).isAir()) {
                this.getEntityData().set(SQUEEZING_ACCESSOR, false);
                this.refreshDimensions();
            }
        }
        */
        super.tick();
    }


    public boolean isMoving() {
        Vec3 velocity = this.getDeltaMovement();
        float avgVelocity = (float)(Math.abs(velocity.x) + Math.abs(velocity.z)) / 2.0F;
        if (this.getTarget() != null) {
        }
        return avgVelocity > 0.03F;
    }

    public void setCrawling(boolean shouldCrawl) {
        if (shouldCrawl) {
            getEntityData().set(CROUCHING_ACCESSOR, false);
        }
        getEntityData().set(SQUEEZING_ACCESSOR, shouldCrawl);
        this.refreshDimensions();
    }

    public boolean isCrawling() {
        return entityData.get(SQUEEZING_ACCESSOR);
    }

    private void TriggeredAnimationControllerTick() {
        int testNum = 0;
        --this.waitToStartAnimatorController;
        if (this.waitToStartAnimatorController <= 0.0F) {
            if (this.squeezeCrawling) {
                ++testNum;
                this.triggerAnim("controller", "crawl");
                this.currentAnim = this.CRAWL;
                return;
            }

            if (this.spottedByPlayer) {
                this.triggerAnim("controller", "is_spotted");
                this.currentAnim = this.IS_SPOTTED;
            }
        }
        if (this.getTarget() != null) {
        }
    }

    public void triggerDwellerAnim(@Nullable String controllerName, String animName, RawAnimation animRaw) {
        RawAnimation anim = this.currentAnim;
        if (this.getTarget() != null && anim != null) {
        }

        if (anim != null) {
            if (anim != animRaw) {
                if (this.getTarget() != null) {
                    this.getTarget().sendSystemMessage(Component.literal("anim does not match name, setting." + anim + " -> " + animName));
                }

                if (((Entity)this).level().isClientSide()) {
                    this.getAnimatableInstanceCache().getManagerForId((long)((Entity)this).getId()).tryTriggerAnimation(controllerName, animName);
                } else {
                    this.triggerAnim(controllerName, animName);
                }
            }
        } else {
            if (this.getTarget() != null) {
                this.getTarget().sendSystemMessage(Component.literal("anim null and setting"));
            }

            if (((Entity)this).level().isClientSide()) {
                this.getAnimatableInstanceCache().getManagerForId((long)((Entity)this).getId()).tryTriggerAnimation(controllerName, animName);
            } else {
                this.triggerAnim(controllerName, animName);
            }
        }

}

    public void rRoll() {
        Random rand = new Random();
        this.forcedStalk = false;
        this.rRollResult = rand.nextInt(4);
    }

    public boolean shouldSpawnAsStalker() {
        Random rand = new Random();
        float stalkerResult = rand.nextFloat();
        System.out.println("spawned as stalker: " + ((stalkerResult < this.chanceOfSpawningAsStalker) ? 1 : 0));
        return (stalkerResult < this.chanceOfSpawningAsStalker);
    }


    public Path createShortPath(LivingEntity pathTarget) {
        this.getEntityData().set(SQUEEZING_ACCESSOR, true);
        this.refreshDimensions();
        this.reapplyPosition();
        this.setMaxUpStep(10.0F);
        Path shortPath = this.getNavigation().createPath(pathTarget, 0);
        this.setMaxUpStep(0.0F);
        this.getEntityData().set(SQUEEZING_ACCESSOR, this.squeezeCrawling);
        this.reapplyPosition();
        return shortPath;
    }

    public Path createClimbPath(LivingEntity pathTarget) {
        this.setMaxUpStep(100.0F);
        Path climbPath = this.getNavigation().createPath(pathTarget, 0);
        this.setMaxUpStep(this.defaultMaxUpStep);
        return climbPath;
    }


    private PlayState predicate(AnimationState<?> tAnimationState) {
        if ((Boolean)this.entityData.get(AGGRO_ACCESSOR)) {
            if ((Boolean)this.entityData.get(CLIMBING_ACCESSOR)) {
                return tAnimationState.setAndContinue(this.CLIMB);
            } else if ((Boolean)this.entityData.get(SQUEEZING_ACCESSOR)) {
                return tAnimationState.setAndContinue(this.CRAWL);
            } else if ((Boolean)this.entityData.get(CROUCHING_ACCESSOR)) {
                return tAnimationState.isMoving() ? tAnimationState.setAndContinue(this.CROUCH_RUN) : tAnimationState.setAndContinue(this.CROUCH_IDLE);
            } else {
                return tAnimationState.isMoving() ? tAnimationState.setAndContinue(this.CHASE) : tAnimationState.setAndContinue(this.CHASE_IDLE);
            }
        } else if ((Boolean)this.entityData.get(FLEEING_ACCESSOR)) {
            return tAnimationState.isMoving() ? tAnimationState.setAndContinue(this.FLEE) : tAnimationState.setAndContinue(this.CHASE_IDLE);
        } else if ((Boolean)this.entityData.get(STALKING_ACCESSOR)) {
            return tAnimationState.isMoving() ? tAnimationState.setAndContinue(this.STALK) : tAnimationState.setAndContinue(this.STALK_IDLE);
        } else if ((Boolean)this.entityData.get(SPOTTED_ACCESSOR)) {
            return tAnimationState.setAndContinue(this.IS_SPOTTED);
        } else {
            return tAnimationState.isMoving() ? tAnimationState.setAndContinue(this.CALM_RUN) : tAnimationState.setAndContinue(this.CALM_STILL);
        }
    }

    // Animation controller
    public void registerControllers(AnimatableManager.ControllerRegistrar controllerRegistrar) {
        controllerRegistrar.add(new AnimationController[]{(new AnimationController(this, "controller", 3, this::predicate)).triggerableAnim("calm_run", this.CALM_RUN).triggerableAnim("calm_still", this.CALM_STILL).triggerableAnim("chase", this.CHASE).triggerableAnim("chase_idle", this.CHASE_IDLE).triggerableAnim("crouch_run", this.CROUCH_RUN).triggerableAnim("crouch_idle", this.CROUCH_IDLE).triggerableAnim("is_spotted", this.IS_SPOTTED).triggerableAnim("crawl", this.CRAWL)});
    }

    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    @Override
    protected void playStepSound(BlockPos pPos, BlockState pState) {
        super.playStepSound(pPos, pState);
        this.playEntitySound(this.chooseStep());
    }

    private void playEntitySound(SoundEvent soundEvent) {
        this.playEntitySound(soundEvent, 1.0F, 1.0F);
    }

    private void playEntitySound(SoundEvent soundEvent, float volume, float pitch) {
        this.level().playSound((Player)null, this, soundEvent, SoundSource.HOSTILE, volume, pitch);
    }

    private void playBlockPosSound(SoundEvent soundEvent, float volume, float pitch) {
        BlockPos blockPos = BlockPos.containing(this.position());
        Minecraft.getInstance().getSoundManager().play(new SimpleSoundInstance(soundEvent, SoundSource.HOSTILE, volume, pitch, RandomSource.create(), blockPos));
    }

    public void playChaseSound() {
        if (this.startedPlayingChaseSound || this.isMoving()) {
            if (this.chaseSoundClock <= 0) {
                Random rand = new Random();
                switch (rand.nextInt(4)) {
                    case 0 -> this.playEntitySound((SoundEvent)ModSounds.CHASE_1.get(), 3.0F, 1.0F);
                    case 1 -> this.playEntitySound((SoundEvent)ModSounds.CHASE_2.get(), 3.0F, 1.0F);
                    case 2 -> this.playEntitySound((SoundEvent)ModSounds.CHASE_3.get(), 3.0F, 1.0F);
                    case 3 -> this.playEntitySound((SoundEvent)ModSounds.CHASE_4.get(), 3.0F, 1.0F);
                }

                this.startedPlayingChaseSound = true;
                this.resetChaseSoundClock();
            }

            --this.chaseSoundClock;
        }
    }

    public void playClimbSound() {
        if (this.climbSoundClock <= 0) {
            Random rand = new Random();
            switch (rand.nextInt(8)) {
                case 0 -> this.playEntitySound((SoundEvent)ModSounds.DWELLER_CLIMB_1.get(), 3.0F, 1.0F);
                case 1 -> this.playEntitySound((SoundEvent)ModSounds.DWELLER_CLIMB_2.get(), 3.0F, 1.0F);
                case 2 -> this.playEntitySound((SoundEvent)ModSounds.DWELLER_CLIMB_3.get(), 3.0F, 1.0F);
                case 3 -> this.playEntitySound((SoundEvent)ModSounds.DWELLER_CLIMB_4.get(), 3.0F, 1.0F);
                case 4 -> this.playEntitySound((SoundEvent)ModSounds.DWELLER_CLIMB_5.get(), 3.0F, 1.0F);
                case 5 -> this.playEntitySound((SoundEvent)ModSounds.DWELLER_CLIMB_6.get(), 3.0F, 1.0F);
                case 6 -> this.playEntitySound((SoundEvent)ModSounds.DWELLER_CLIMB_7.get(), 3.0F, 1.0F);
                case 7 -> this.playEntitySound((SoundEvent)ModSounds.DWELLER_CLIMB_8.get(), 3.0F, 1.0F);
            }

            this.resetClimbSoundClock();
        }

        --this.climbSoundClock;
    }

    public void playFleeSound() {
        if (!this.alreadyPlayedFleeSound) {
            Random rand = new Random();
            switch (rand.nextInt(2)) {
                case 0 -> this.playEntitySound((SoundEvent)ModSounds.FLEE_1.get(), 3.0F, 1.0F);
                case 1 -> this.playEntitySound((SoundEvent)ModSounds.FLEE_2.get(), 3.0F, 1.0F);
            }

            this.alreadyPlayedFleeSound = true;
        }

    }

    public void playSpottedSound() {
        if (!this.alreadyPlayedSpottedSound) {
            this.playEntitySound((SoundEvent)ModSounds.SPOTTED.get(), 3.0F, 1.0F);
            this.alreadyPlayedSpottedSound = true;
        }

    }

    public boolean inPlayerLineOfSight() {
        return this.getTarget() != null ? this.getTarget().hasLineOfSight(this) : false;
    }

    public boolean isPlayerLookingTowards() {
        if (this.getTarget() == null) {
            return false;
        } else {
            Minecraft minecraft = Minecraft.getInstance();
            boolean yawPlayerLookingTowards = false;
            float fov = (float)(Integer)minecraft.options.fov().get();
            float yFovMod = 0.65F;
            float fovMod = (35.0F / fov - 1.0F) * 0.4F + 1.0F;
            fov *= fovMod;
            Vec3 a = this.getTarget().position();
            Vec3 b = this.position();
            Vec2 dist = new Vec2((float)b.x - (float)a.x, (float)b.z - (float)a.z);
            dist = dist.normalized();
            double newAngle = Math.toDegrees(Math.atan2((double)dist.x, (double)dist.y));
            float lookX = (float)this.getTarget().getViewVector(1.0F).x;
            float lookZ = (float)this.getTarget().getViewVector(1.0F).z;
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
            float lookY = (float)this.getTarget().getViewVector(1.0F).y;
            Vec2 lookDist = new Vec2((float)Math.sqrt((double)(lookX * lookX + lookZ * lookZ)), lookY);
            lookDist = lookDist.normalized();
            double yLookAngle = Math.toDegrees(Math.atan2((double)lookDist.x, (double)lookDist.y));
            double newYAngle = this.loopAngle(yAngle - yLookAngle) + (double)yFov;
            newYAngle = this.loopAngle(newYAngle);
            if (newYAngle > 0.0D && newYAngle < (double)(yFov * 2.0F)) {
                pitchPlayerLookingTowards = true;
            }

            if (!(yLookAngle < (double)(180.0F - yFov)) || !(yLookAngle > (double)yFov)) {
                shouldOnlyUsePitch = true;
            }

            return (yawPlayerLookingTowards || shouldOnlyUsePitch) && pitchPlayerLookingTowards;
        }
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

    public void playDisappearSound() {
        this.playBlockPosSound((SoundEvent)ModSounds.DISAPPEAR.get(), 3.0F, 1.0F);
    }

    private void resetChaseSoundClock() {
        this.chaseSoundClock = this.chaseSoundClockReset;
    }

    private void resetClimbSoundClock() {
        this.climbSoundClock = this.climbSoundClockReset;
    }

    private SoundEvent chooseStep() {
        Random rand = new Random();
        switch (rand.nextInt(4)) {
            case 0 -> {
                return (SoundEvent)ModSounds.CHASE_STEP_1.get();
            }
            case 1 -> {
                return (SoundEvent)ModSounds.CHASE_STEP_2.get();
            }
            case 2 -> {
                return (SoundEvent)ModSounds.CHASE_STEP_3.get();
            }
            case 3 -> {
                return (SoundEvent)ModSounds.CHASE_STEP_4.get();
            }
            default -> {
                return (SoundEvent)ModSounds.CHASE_STEP_1.get();
            }
        }
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (SQUEEZING_ACCESSOR.equals(key)) {
            this.refreshDimensions();
        }
    }

    private SoundEvent chooseHurtSound() {
        Random rand = new Random();
        switch (rand.nextInt(4)) {
            case 0 -> {
                return (SoundEvent)ModSounds.DWELLER_HURT_1.get();
            }
            case 1 -> {
                return (SoundEvent)ModSounds.DWELLER_HURT_2.get();
            }
            case 2 -> {
                return (SoundEvent)ModSounds.DWELLER_HURT_3.get();
            }
            case 3 -> {
                return (SoundEvent)ModSounds.DWELLER_HURT_4.get();
            }
            default -> {
                return (SoundEvent)ModSounds.DWELLER_HURT_1.get();
            }
        }
    }

    @Override
    protected void playHurtSound(DamageSource pSource) {
        SoundEvent soundevent = this.chooseHurtSound();
        if (soundevent != null) {
            this.playEntitySound(soundevent, 2.0F, 1.0F);
        }
    }

    @Override
    protected void dropAllDeathLoot(DamageSource pSource) {
        this.setItemSlot(EquipmentSlot.FEET, ItemStack.EMPTY);
        super.dropAllDeathLoot(pSource);
        if (!this.alreadyPlayedDeathSound && this.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            SoundEvent deathSound = (SoundEvent)ModSounds.DWELLER_DEATH.get();
            for (net.minecraft.server.level.ServerPlayer player : serverLevel.players()) {
                if (player.distanceToSqr(this) < 64 * 64) {
                    player.connection.send(new net.minecraft.network.protocol.game.ClientboundSoundPacket(
                            net.minecraft.core.registries.BuiltInRegistries.SOUND_EVENT.wrapAsHolder(deathSound),
                            net.minecraft.sounds.SoundSource.HOSTILE,
                            this.getX(), this.getY(), this.getZ(),
                            2.0F, 1.0F,
                            this.level().getRandom().nextLong()
                    ));
                }
            }
            this.alreadyPlayedDeathSound = true;
        }
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSourceIn) {
        return this.chooseHurtSound();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return (SoundEvent)ModSounds.DWELLER_DEATH.get();
    }

    @Override
    protected float getSoundVolume() {
        return 0.4F;
    }


    static {
        FLEEING_ACCESSOR = SynchedEntityData.defineId(CaveDwellerEntity.class, EntityDataSerializers.BOOLEAN);
        CROUCHING_ACCESSOR = SynchedEntityData.defineId(CaveDwellerEntity.class, EntityDataSerializers.BOOLEAN);
        AGGRO_ACCESSOR = SynchedEntityData.defineId(CaveDwellerEntity.class, EntityDataSerializers.BOOLEAN);
        SQUEEZING_ACCESSOR = SynchedEntityData.defineId(CaveDwellerEntity.class, EntityDataSerializers.BOOLEAN);
        SPOTTED_ACCESSOR = SynchedEntityData.defineId(CaveDwellerEntity.class, EntityDataSerializers.BOOLEAN);
        CLIMBING_ACCESSOR = SynchedEntityData.defineId(CaveDwellerEntity.class, EntityDataSerializers.BOOLEAN);
        STALKING_ACCESSOR = SynchedEntityData.defineId(CaveDwellerEntity.class, EntityDataSerializers.BOOLEAN);
    }
}
