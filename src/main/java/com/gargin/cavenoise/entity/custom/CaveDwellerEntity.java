package com.gargin.cavenoise.entity.custom;

import com.gargin.cavenoise.sound.ModSounds;

import java.util.Random;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
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
    private RawAnimation DEATH;
    private RawAnimation currentAnim;
    public static final EntityDataAccessor<Boolean> FLEEING_ACCESSOR;
    public static final EntityDataAccessor<Boolean> CROUCHING_ACCESSOR;
    public static final EntityDataAccessor<Boolean> AGGRO_ACCESSOR;
    public static final EntityDataAccessor<Boolean> SQUEEZING_ACCESSOR;
    public static final EntityDataAccessor<Boolean> SPOTTED_ACCESSOR;
    public static final EntityDataAccessor<Boolean> CLIMBING_ACCESSOR;
    public static final EntityDataAccessor<Boolean> STALKING_ACCESSOR;
    public static final EntityDataAccessor<Float> JAW_TRANSLATION = SynchedEntityData.defineId(CaveDwellerEntity.class, EntityDataSerializers.FLOAT);
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
    public boolean hasSpawned;
    public boolean pleaseStopMoving;
    SoundEvent spottedSound;
    SoundEvent deathSound;

    /// Death animation variable initialization
    private boolean isPlayingDeathAnimation = false;
    public int deathAnimationTicks = 0;
    // Death animation length in ticks
    public static final int DEATH_ANIMATION_LENGTH = 30;

    /// Jaw drop variable initialization
    public float prevJawTranslation = 0.0F;
    public float jawTranslation = 0.0F;
    // jawSpeed is from 0.0-1.0
    private float jawSpeed = 0.3F;
    private float jawTargetDistance = 0.0F;
    private int jawHoldTicks = 0;
    private boolean isOpening = false;

    public CaveDwellerEntity(EntityType<? extends CaveDwellerEntity> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
        this.OLD_RUN = RawAnimation.begin().then("animation.cave_dweller.run", LoopType.LOOP);
        this.IDLE = RawAnimation.begin().then("animation.cave_dweller.idle", LoopType.LOOP);
        this.CHASE = RawAnimation.begin().then("animation.cave_dweller.run", LoopType.LOOP);
        this.CHASE_IDLE = RawAnimation.begin().then("animation.cave_dweller.run_idle", LoopType.LOOP);
        this.CROUCH_RUN = RawAnimation.begin().then("animation.cave_dweller.crouch_run_new", LoopType.LOOP);
        this.CROUCH_IDLE = RawAnimation.begin().then("animation.cave_dweller.crouch_idle", LoopType.LOOP);
        this.CALM_RUN = RawAnimation.begin().then("animation.cave_dweller.run", LoopType.LOOP);
        this.CALM_STILL = RawAnimation.begin().then("animation.cave_dweller.idle", LoopType.LOOP);
        this.IS_SPOTTED = RawAnimation.begin().then("animation.cave_dweller.idle", LoopType.LOOP);
        this.CRAWL = RawAnimation.begin().then("animation.cave_dweller.crawl", LoopType.LOOP);
        this.FLEE = RawAnimation.begin().then("animation.cave_dweller.flee", LoopType.LOOP);
        this.STALK = RawAnimation.begin().then("animation.cave_dweller.stalking", LoopType.LOOP);
        this.STALK_IDLE = RawAnimation.begin().then("animation.cave_dweller.stalking_idle", LoopType.LOOP);
        this.CLIMB = RawAnimation.begin().then("animation.cave_dweller.climb", LoopType.LOOP);
        this.DEATH = RawAnimation.begin().then("animation.cave_dweller.death", LoopType.HOLD_ON_LAST_FRAME);
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
        this.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 999999, 100, true, false));
        this.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, 999999, 100, true, false));
        this.forcedStalk = true;
        this.setPathfindingMalus(net.minecraft.world.level.pathfinder.BlockPathTypes.BLOCKED, 0.0F);
        this.setPathfindingMalus(net.minecraft.world.level.pathfinder.BlockPathTypes.WALKABLE, 0.0F);
    }

    public static AttributeSupplier setAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 150.0D).add(Attributes.ATTACK_DAMAGE, 6.0D).add(Attributes.MOVEMENT_SPEED, 0.44D).add(Attributes.KNOCKBACK_RESISTANCE, 0.6D).add(Attributes.FOLLOW_RANGE, 100.0D).add(Attributes.ARMOR, 3.0D).build();
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
        this.entityData.define(JAW_TRANSLATION, 0.0F);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new DwellerBreakInvisGoal(this));
        this.goalSelector.addGoal(3, new DwellerStareGoal(this, 100.0F));
        this.goalSelector.addGoal(4, new DwellerChaseGoal(this, this, 0.85D, true, 80.0F));
        this.goalSelector.addGoal(4, new DwellerFleeGoal(this, 20.0F, 1.0D));
        this.goalSelector.addGoal(4, new DwellerStalkGoal(this, 0.5D, 15.0F));
        this.goalSelector.addGoal(5, new DwellerStrollGoal(this, 0.7D));
        this.targetSelector.addGoal(1, new DwellerTargetTooCloseGoal(this, 4.0F));
        this.targetSelector.addGoal(2, new DwellerTargetSeesMeGoal(this));
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

    public float getJawTranslation() {
        return this.entityData.get(JAW_TRANSLATION);
    }

    public void setJawTranslation(float translation) {
        this.entityData.set(JAW_TRANSLATION, translation);
    }

    public void dropJaw(float speed, float distance, float holdTimeInSeconds) {
        System.out.println("[CaveDwellerEntity] dropJaw(): called with the following inputs:");
        System.out.println("[CaveDwellerEntity] dropJaw(): speed: " + speed + " | distance: " + distance + " | hold time in seconds: " + holdTimeInSeconds);
        if (!this.level().isClientSide()) {
            this.jawSpeed = speed;
            this.jawTargetDistance = -Math.abs(distance);
            int jawHoldTicks = (int) (holdTimeInSeconds * 20.0F);
            this.isOpening = true;
        }
    }

    @Override
    public void tick() {
        --this.ticksTillRemove;
        if (this.ticksTillRemove <= 0 && (!this.isPlayerLookingTowards() || !this.inPlayerLineOfSight())) {
            this.playDisappearSound();
            this.discard();
        }

        // Jaw opening/closing
        if (!this.level().isClientSide()) {
            this.prevJawTranslation = this.jawTranslation;
            if (this.isOpening) {
                // Smoothly lerp towards the open target distance
                this.jawTranslation += (this.jawTargetDistance - this.jawTranslation) * this.jawSpeed;
                // If close enough to target, start the hold timer
                if (Math.abs(this.jawTargetDistance - this.jawTranslation) < 0.01F) {
                    this.jawTranslation = this.jawTargetDistance;
                    this.isOpening = false; // Stop opening, transition to holding
                }
            } else if (this.jawHoldTicks > 0) {
                // Hold the jaw wide open until the timer runs out
                this.jawHoldTicks--;
            } else {
                // Smoothly lerp back to closed (0.0F)
                this.jawTranslation += (0.0F - this.jawTranslation) * this.jawSpeed;
                if (Math.abs(0.0F - this.jawTranslation) < 0.01F) {
                    this.jawTranslation = 0.0F;
                }
            }

            // Sync the true smooth position to the client renderer
            this.setJawTranslation(this.jawTranslation);
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

    // Choose what state the dweller is in
    private PlayState predicate(AnimationState<?> tAnimationState) {
        // Death check takes priority
        if (this.isPlayingDeathAnimation()) {
            return tAnimationState.setAndContinue(this.DEATH);
        }
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

    /// Death animation
    @Override
    public void die(DamageSource damageSource) {
        super.die(damageSource);
        this.isPlayingDeathAnimation = true;
    }

    @Override
    protected void tickDeath() {
        this.deathAnimationTicks++;
        if (this.deathAnimationTicks >= DEATH_ANIMATION_LENGTH) {
            super.tickDeath();
        }
    }

    public boolean isPlayingDeathAnimation() {
        return this.isPlayingDeathAnimation;
    }
    /// End of death animation

    // Animation controller
    public void registerControllers(AnimatableManager.ControllerRegistrar controllerRegistrar) {
        controllerRegistrar.add(new AnimationController[]{(new AnimationController(this, "controller", 3, this::predicate))
                .triggerableAnim("calm_run", this.CALM_RUN)
                .triggerableAnim("calm_still", this.CALM_STILL)
                .triggerableAnim("new_run", this.CHASE)
                .triggerableAnim("idle", this.CHASE_IDLE)
                .triggerableAnim("crouch_run", this.CROUCH_RUN)
                .triggerableAnim("crouch_idle", this.CROUCH_IDLE)
                .triggerableAnim("is_spotted", this.IS_SPOTTED)
                .triggerableAnim("crawl", this.CRAWL)
                .triggerableAnim("death", this.DEATH)
        });
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
                this.dropJaw(0.3F, 5.0F, 3.0F);
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
        if (this.level().isClientSide()) {
            return;
        }
        ServerPlayer player = (ServerPlayer) this.level().getNearestPlayer(this, 64.0D);
        if (player == null) {
            return;
        }
        if (!this.alreadyPlayedSpottedSound) {
            // If the player is within ~12 blocks (adjusted to 11 to account for imprecise coordinates), play SPOTTED_#
            // If they are further than that, play SPOTTED_DISTANT
            // 11 blocks squared = 121
            double spottedSoundDistance = 121.0D;
            if (this.distanceToSqr(player) <= spottedSoundDistance) {
                Random rand = new Random();
                spottedSound = switch (rand.nextInt(3)) {
                    case 0 -> (SoundEvent) ModSounds.SPOTTED_1.get();
                    case 1 -> (SoundEvent) ModSounds.SPOTTED_2.get();
                    default -> (SoundEvent) ModSounds.SPOTTED_3.get();
                };
                player.connection.send(new net.minecraft.network.protocol.game.ClientboundSoundPacket(
                        net.minecraft.core.registries.BuiltInRegistries.SOUND_EVENT.wrapAsHolder(spottedSound),
                        net.minecraft.sounds.SoundSource.HOSTILE,
                        player.getX(), player.getY(), player.getZ(),
                        1.0F, 1.0F,
                        this.level().getRandom().nextLong()
                ));
            } else {
                player.connection.send(new net.minecraft.network.protocol.game.ClientboundSoundPacket(
                        net.minecraft.core.registries.BuiltInRegistries.SOUND_EVENT.wrapAsHolder(ModSounds.SPOTTED_DISTANT.get()),
                        net.minecraft.sounds.SoundSource.HOSTILE,
                        player.getX(), player.getY(), player.getZ(),
                        1.0F, 1.0F,
                        this.level().getRandom().nextLong()
                ));
            }
        }

        this.alreadyPlayedSpottedSound = true;
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
                return (SoundEvent)ModSounds.STEP_1.get();
            }
            case 1 -> {
                return (SoundEvent)ModSounds.STEP_2.get();
            }
            case 2 -> {
                return (SoundEvent)ModSounds.STEP_3.get();
            }
            default -> {
                return (SoundEvent)ModSounds.STEP_4.get();
            }
        }
    }

    // Responsible for updating dimensions?
    // Doesn't do anything?
    public EntityDimensions getEntityDimensions(Pose pPose) {
        if (this.isAggro) {
            return this.returnShort ? new EntityDimensions(0.5F, 0.9F, true) : new EntityDimensions(0.5F, 1.9F, true);
        } else {
            return new EntityDimensions(0.5F, 1.9F, true);
        }
    }

    private SoundEvent chooseHurtSound() {
        Random rand = new Random();
        switch (rand.nextInt(4)) {
            case 0 -> {
                return (SoundEvent)ModSounds.HURT_1.get();
            }
            case 1 -> {
                return (SoundEvent)ModSounds.HURT_2.get();
            }
            case 2 -> {
                return (SoundEvent)ModSounds.HURT_3.get();
            }
            default -> {
                return (SoundEvent)ModSounds.HURT_4.get();
            }
        }
    }

    @Override
    protected void playHurtSound(DamageSource pSource) {
        SoundEvent soundevent = this.chooseHurtSound();
        if (soundevent != null) {
            this.playEntitySound(soundevent, 1.0F, 1.0F);
        }
    }

    protected SoundEvent chooseDeathSound() {
        Random rand = new Random();
        switch (rand.nextInt(4)) {
            case 0 -> {
                return (SoundEvent)ModSounds.DWELLER_DEATH_1.get();
            }
            case 1 -> {
                return (SoundEvent)ModSounds.DWELLER_DEATH_2.get();
            }
            case 2 -> {
                return (SoundEvent)ModSounds.DWELLER_DEATH_3.get();
            }
            default -> {
                return (SoundEvent)ModSounds.DWELLER_DEATH_4.get();
            }
        }
    }

    // Made the death sound volume 10.0F to match Bedrock's loudness
    @Override
    protected void dropAllDeathLoot(DamageSource pSource) {
        this.setItemSlot(EquipmentSlot.FEET, ItemStack.EMPTY);
        super.dropAllDeathLoot(pSource);
        if (!this.alreadyPlayedDeathSound && this.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            deathSound = this.chooseDeathSound();
            for (net.minecraft.server.level.ServerPlayer player : serverLevel.players()) {
                if (player.distanceToSqr(this) < 64 * 64) {
                    player.connection.send(new net.minecraft.network.protocol.game.ClientboundSoundPacket(
                            net.minecraft.core.registries.BuiltInRegistries.SOUND_EVENT.wrapAsHolder(deathSound),
                            net.minecraft.sounds.SoundSource.HOSTILE,
                            this.getX(), this.getY(), this.getZ(),
                            10.0F, 1.0F,
                            this.level().getRandom().nextLong()
                    ));
                }
            }
        }
        this.alreadyPlayedDeathSound = true;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSourceIn) {
        return this.chooseHurtSound();
    }

    @Override
    protected float getSoundVolume() {
        return 0.4F;
    }

    /// Custom name setup
    @Override
    public boolean isCustomNameVisible() {
        return false;
    }

    @Override
    public boolean hasCustomName() {
        return this.isDeadOrDying();
    }

    @Override
    public Component getCustomName() {
        if (!this.isDeadOrDying()) {
            // Added an "_" so there isn't a space in the obfuscated name
            return Component.literal("§kCave_Dweller");
        }
        return null;
    }

    @Override
    public Component getDisplayName() {
        return Component.literal("§kCave_Dweller");
    }
    /// End of custom name setup

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
