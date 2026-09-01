package com.gargin.cavenoise.entities;

import com.gargin.cavenoise.CaveNoise;
import com.gargin.cavenoise.entities.goals.*;
import com.gargin.cavenoise.registry.ModSounds;

import java.util.List;
import java.util.Objects;
import java.util.Random;

import com.gargin.cavenoise.util.Utils;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.navigation.WallClimberNavigation;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import org.jetbrains.annotations.NotNull;
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

import static com.gargin.cavenoise.client.CaveDwellerRenderer.timingOffset;

public class CaveDwellerEntity extends Monster implements GeoEntity {
    private net.minecraft.world.entity.ai.navigation.PathNavigation waterNavigation;
    private AnimatableInstanceCache cache = new SingletonAnimatableInstanceCache(this);
    private final RawAnimation CHASE;
    private final RawAnimation CATCH_UP;
    private final RawAnimation IDLE;
    private final RawAnimation CROUCH_RUN;
    private final RawAnimation CROUCH_IDLE;
    private final RawAnimation CALM_RUN;
    private final RawAnimation CALM_STILL;
    private final RawAnimation IS_SPOTTED;
    private final RawAnimation SQUEEZE;
    private final RawAnimation SQUEEZE_END;
    private final RawAnimation HIDE;
    private final RawAnimation STALK;
    private final RawAnimation STALK_IDLE;
    private final RawAnimation CLIMB;
    private final RawAnimation DEATH;
    private final RawAnimation HURT;
    private final RawAnimation STANDOFF;
    public static final EntityDataAccessor<Boolean> STALKING_ACCESSOR;
    public static final EntityDataAccessor<Boolean> HIDING_ACCESSOR;
    public static final EntityDataAccessor<Boolean> CROUCHING_ACCESSOR;
    public static final EntityDataAccessor<Boolean> AGGRO_ACCESSOR;
    public static final EntityDataAccessor<Boolean> SQUEEZING_ACCESSOR;
    public static final EntityDataAccessor<Boolean> SPOTTED_ACCESSOR;
    public static final EntityDataAccessor<Boolean> STANDOFF_ACCESSOR;
    public static final EntityDataAccessor<Boolean> CLIMBING_ACCESSOR;
    public static final EntityDataAccessor<Float> CLIMB_ANGLE_ACCESSOR = SynchedEntityData.defineId(CaveDwellerEntity.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Direction> CLIMB_WALL_ACCESSOR = SynchedEntityData.defineId(CaveDwellerEntity.class, EntityDataSerializers.DIRECTION);
    private static final EntityDataAccessor<Float> JAW_SPEED = SynchedEntityData.defineId(CaveDwellerEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> JAW_DISTANCE = SynchedEntityData.defineId(CaveDwellerEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> JAW_HOLD = SynchedEntityData.defineId(CaveDwellerEntity.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Float> RENDER_ALPHA_ACCESSOR = SynchedEntityData.defineId(CaveDwellerEntity.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Integer> FADE_STATE_ACCESSOR = SynchedEntityData.defineId(CaveDwellerEntity.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<Float> FADE_SPEED_ACCESSOR = SynchedEntityData.defineId(CaveDwellerEntity.class, EntityDataSerializers.FLOAT);
    public boolean debug = CaveNoise.CONFIG.DEBUG();
    private float defaultMaxUpStep = 2.0F;
    private float twoBlockSpaceCooldown;
    public Roll currentRoll;
    public boolean isHiding;
    public int initializationDelayTicks;
    public boolean pleaseStopMoving;
    public boolean targetIsLookingAtMe;
    private float twoBlockSpaceTimer;
    private int ticksUntilRemove;
    private int chaseSoundClock = 5000;
    private int chaseSoundClockReset;
    private int climbSoundClock;
    private int climbSoundClockReset;
    private int heartbeatSoundClock;
    private int heartbeatSoundClockReset;
    private int breathingSoundClock;
    private int breathingSoundClockReset;
    private boolean inTwoBlockSpace;
    private boolean alreadyPlayedFleeSound;
    private boolean alreadyPlayedSpottedSound;
    private boolean canPlayChaseSound;
    private boolean alreadyPlayedDeathSound;
    private boolean startedPlayingHeartbeatSound;
    private boolean startedPlayingBreathingSound;
    private boolean canPlayJawMovement;
    private int squeezingTicks;
    public boolean startedMovingChase = false;
    public boolean isAggro = false;
    public Path shortPath;
    public boolean shouldUseShortPath = false;
    public boolean shortPathAvailable;
    public Direction wallDirection = Direction.NORTH;
    public boolean hiding;
    private float randomPitch = getRandomPitch();
    private float targetAlpha;
    private boolean shouldDisappearAfterFade;
    public boolean isSubmerged = this.isInWater() || this.level().containsAnyLiquid(this.getBoundingBox());

    // DEBUG
    private int spawnScanTickCounter = 0;

    public boolean spottedByPlayer = false;
    public boolean stalking;
    SoundEvent spottedSound;
    SoundEvent deathSound;
    // Added an "_" so there isn't a space in the obfuscated name (for Bedrock parity)
    public String caveDwellerName = "§kCave_Dweller§r";

    /// Animation variable initializations
    private boolean isPlayingDeathAnimation = false;
    public int deathAnimationTicks = 0;
    private boolean isPlayingHurtAnimation = false;
    public int hurtAnimationTicks = 0;

    // Death and hurt animation lengths in ticks
    // TODO: Death -> animation begins -> start 100% transparency fade -> eyes/teeth fade out a little slower? -> body + eyes/teeth reach fully transparent -> add invisibility -> poof appears right after
    public static final int deathAnimationLength = 20 + 20; // Full animation is 20 ticks (1 second), but a buffer is needed so you can't see it "fall over"
    public static final int hurtAnimationLength = 16; // 16 = 0.8 sec

    /// Jaw drop variable initialization
    private int jawAnimationTick = -1;


    /// Initialize CaveDwellerEntity
    public CaveDwellerEntity(EntityType<? extends CaveDwellerEntity> entityType, Level level) {
        super(entityType, level);
        this.CHASE = RawAnimation.begin().then("animation.cave_dweller.walk", LoopType.LOOP);
        this.CATCH_UP = RawAnimation.begin().then("animation.cave_dweller.catch_up", LoopType.LOOP);
        this.IDLE = RawAnimation.begin().then("animation.cave_dweller.idle", LoopType.LOOP);
        this.CROUCH_RUN = RawAnimation.begin().then("animation.cave_dweller.crouch_run_new", LoopType.LOOP);
        this.CROUCH_IDLE = RawAnimation.begin().then("animation.cave_dweller.crouch_idle", LoopType.LOOP);
        this.CALM_RUN = RawAnimation.begin().then("animation.cave_dweller.walk", LoopType.LOOP);
        this.CALM_STILL = RawAnimation.begin().then("animation.cave_dweller.idle", LoopType.LOOP);
        this.IS_SPOTTED = RawAnimation.begin().then("animation.cave_dweller.idle", LoopType.LOOP);
        this.SQUEEZE = RawAnimation.begin().then("animation.cave_dweller.squeeze", LoopType.LOOP);
        this.SQUEEZE_END = RawAnimation.begin().then("animation.cave_dweller.squeeze_end", LoopType.HOLD_ON_LAST_FRAME);
        this.HIDE = RawAnimation.begin().then("animation.cave_dweller.idle", LoopType.LOOP);
        this.STALK = RawAnimation.begin().then("animation.cave_dweller.stalking", LoopType.LOOP);
        this.STALK_IDLE = RawAnimation.begin().then("animation.cave_dweller.stalking_idle", LoopType.LOOP);
        this.CLIMB = RawAnimation.begin().then("animation.cave_dweller.climb", LoopType.LOOP);
        this.DEATH = RawAnimation.begin().then("animation.cave_dweller.death", LoopType.HOLD_ON_LAST_FRAME);
        this.HURT = RawAnimation.begin().then("animation.cave_dweller.hurt", LoopType.PLAY_ONCE);
        this.STANDOFF = RawAnimation.begin().then("animation.cave_dweller.standoff", LoopType.HOLD_ON_LAST_FRAME);
        this.refreshDimensions();
        this.initializationDelayTicks = 10;
        this.twoBlockSpaceTimer = 0.0F;
        this.twoBlockSpaceCooldown = 5.0F;
        this.climbSoundClock = 0;
        this.chaseSoundClock = 0;
        this.heartbeatSoundClock = 0;
        this.breathingSoundClock = 0;
        this.climbSoundClockReset = 20;
        this.alreadyPlayedFleeSound = false;
        this.alreadyPlayedSpottedSound = false;
        this.canPlayChaseSound = false;
        this.alreadyPlayedDeathSound = false;
        this.startedPlayingHeartbeatSound = false;
        this.startedPlayingBreathingSound = false;
        this.canPlayJawMovement = true;
        this.setMaxUpStep(this.defaultMaxUpStep);
        this.ticksUntilRemove = Utils.secondsToTicks(CaveNoise.CONFIG.TIME_UNTIL_LEAVE());
        this.wallDirection = Direction.NORTH;
        this.currentRoll = Roll.STALK;
        this.stalking = true;
        this.noPhysics = false;
        this.setNoGravity(false);
        this.setInStandoff(false);

        ItemStack enchantedBoots = new ItemStack(Items.NETHERITE_BOOTS);
        enchantedBoots.enchant(Enchantments.DEPTH_STRIDER, 3);
        enchantedBoots.enchant(Enchantments.UNBREAKING, 255);
        enchantedBoots.enchant(Enchantments.FALL_PROTECTION, 255);
        this.setItemSlot(EquipmentSlot.FEET, enchantedBoots);
        this.setGlowingTag(this.debug);
        this.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 999999, 100, true, false));
        this.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, 999999, 100, true, false));
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(STALKING_ACCESSOR, true);
        this.entityData.define(HIDING_ACCESSOR, false);
        this.entityData.define(CROUCHING_ACCESSOR, false);
        this.entityData.define(AGGRO_ACCESSOR, false);
        this.entityData.define(SQUEEZING_ACCESSOR, false);
        this.entityData.define(SPOTTED_ACCESSOR, false);
        this.entityData.define(STANDOFF_ACCESSOR, false);
        this.entityData.define(CLIMBING_ACCESSOR, false);
        this.entityData.define(CLIMB_ANGLE_ACCESSOR, this.getYRot());
        // wallDirection just needs to be initialized
        this.wallDirection = Direction.NORTH;
        this.entityData.define(CLIMB_WALL_ACCESSOR, this.wallDirection);
        this.entityData.define(JAW_SPEED, 0.0F);
        this.entityData.define(JAW_DISTANCE, 0.0F);
        this.entityData.define(JAW_HOLD, 0.0F);
        this.entityData.define(RENDER_ALPHA_ACCESSOR, 1.0F);
        this.entityData.define(FADE_STATE_ACCESSOR, 0);
        this.entityData.define(FADE_SPEED_ACCESSOR, 0.05F);
    }

    @Override
    protected void registerGoals() {
        float distanceThreshold = 2.0F;
        this.goalSelector.addGoal(2, new DwellerBreakInvisGoal(this));
        this.goalSelector.addGoal(1, new DwellerChaseGoal(this, true, 10.0F));
        this.goalSelector.addGoal(1, new DwellerHideGoal(this));
        this.goalSelector.addGoal(2, new DwellerStalkGoal(this, distanceThreshold, true));
        this.goalSelector.addGoal(2, new DwellerStareGoal(this));
        this.goalSelector.addGoal(3, new DwellerStrollGoal(this, 1.0F));
        this.targetSelector.addGoal(2, new DwellerTargetSeesMeGoal(this));
        this.targetSelector.addGoal(1, new DwellerTargetTooCloseGoal(this, distanceThreshold));
    }

    // TODO: Add dweller skull as drop
    @Override
    protected boolean shouldDropLoot() {
        return false;
    }

    // TODO: Check if unused and make it used if so
    @Override
    protected boolean canRide(@NotNull Entity vehicle) {
        return CaveNoise.CONFIG.ALLOW_RIDING_IN() ? super.canRide(vehicle) : false;
    }

    @Override
    public boolean startRiding(@NotNull Entity vehicle, boolean force) {
        return CaveNoise.CONFIG.ALLOW_RIDING_IN() ? super.startRiding(vehicle, force) : false;
    }

    /// Spawn code + set attributes
    public @Nullable Vec3 generatePos(Player victim) {
        Level level = this.level();
        int maxWorldHeight = level.getMaxBuildHeight() - 5;
        int minWorldHeight = level.getMinBuildHeight() + 5;
        int safeConfigHeight = Mth.clamp(CaveNoise.CONFIG.SPAWN_HEIGHT(), minWorldHeight, maxWorldHeight);

        int spawnOffsetX = 60;
        int spawnOffsetY = 12;
        int spawnOffsetZ = 60;

        BlockPos playerPos = victim.blockPosition();
        Vec3 playerLookVec = victim.getViewVector(1.0F).normalize();
        Vec3 playerEyePos = victim.getEyePosition(1.0F);

        for (int i = 0; i < 200; i++) {
            int offsetX = this.random.nextInt(spawnOffsetX) - (spawnOffsetX / 2);
            int offsetY = this.random.nextInt(spawnOffsetY) - (spawnOffsetY / 2);
            int offsetZ = this.random.nextInt(spawnOffsetZ) - (spawnOffsetZ / 2);

            BlockPos spawnPos = playerPos.offset(offsetX, offsetY, offsetZ);

            // Step down until finding a solid block underneath
            boolean foundFloor = false;
            while (spawnPos.getY() >= minWorldHeight) {
                BlockPos belowPos = spawnPos.below();
                if (level.getBlockState(spawnPos).isAir() && !level.getBlockState(belowPos).isAir()) {
                    foundFloor = true;
                    break;
                }
                spawnPos = belowPos;
            }

            if (!foundFloor) {
                continue;
            }

            BlockPos groundPos = spawnPos.below();

            Vec3 targetVec = new Vec3(
                    spawnPos.getX() + 0.5 - playerEyePos.x,
                    spawnPos.getY() + 0.5 - playerEyePos.y,
                    spawnPos.getZ() + 0.5 - playerEyePos.z
            );

            double dotProduct = playerLookVec.dot(targetVec.normalize());
            boolean notSeen = dotProduct < 0.5;
            boolean isDarkEnough = (level.getBrightness(LightLayer.BLOCK, spawnPos) == CaveNoise.CONFIG.BLOCK_LIGHT_LEVEL());

            if (this.debug) {
                if (level instanceof ServerLevel serverLevel) {
                    double pX = spawnPos.getX() + 0.5;
                    double pY = spawnPos.getY() + 0.5;
                    double pZ = spawnPos.getZ() + 0.5;

                    Slime checkedSpawnPos = EntityType.SLIME.create(serverLevel);
                    if (checkedSpawnPos != null) {
                        checkedSpawnPos.moveTo(pX, pY, pZ, 0.0F, 0.0F);
                        Objects.requireNonNull(checkedSpawnPos.getAttribute(Attributes.MAX_HEALTH)).setBaseValue(0.0D);
                        checkedSpawnPos.setHealth(0.0F);
                        checkedSpawnPos.setSize(1, true);
                        checkedSpawnPos.setSilent(true);
                        checkedSpawnPos.setGlowingTag(true);
                        checkedSpawnPos.setNoGravity(true);
                        checkedSpawnPos.noPhysics = true;
                        CompoundTag nbt = new CompoundTag();
                        nbt.putString("DeathLootTable", "minecraft:empty");
                        checkedSpawnPos.readAdditionalSaveData(nbt);
                        checkedSpawnPos.addEffect(new MobEffectInstance(MobEffects.WITHER, 999999, 100, true, true));
                        serverLevel.addFreshEntity(checkedSpawnPos);
                    }
                }
            }

            if (spawnPos.getY() < safeConfigHeight && spawnPos.getY() >= minWorldHeight) {
                if (notSeen
                        && level.getBlockState(spawnPos).isAir()
                        && level.getBlockState(spawnPos.above()).isAir()
                        && !level.getBlockState(groundPos).isAir()
                        && level.getFluidState(groundPos).isEmpty()
                        && isDarkEnough) {

                    return Vec3.atBottomCenterOf(spawnPos);
                }
            }
        }

        return null;
    }

    @Nullable
    @Override
    // TODO: Increase animation speed relative to movement speed
    public SpawnGroupData finalizeSpawn(@NotNull ServerLevelAccessor level, @NotNull DifficultyInstance difficulty, @NotNull MobSpawnType reason, @Nullable SpawnGroupData spawnData, @Nullable CompoundTag tagData) {
        this.setAttribute(this.getAttribute(Attributes.MAX_HEALTH), CaveNoise.CONFIG.MAX_HEALTH());
        this.setAttribute(this.getAttribute(Attributes.ATTACK_DAMAGE), CaveNoise.CONFIG.ATTACK_DAMAGE());
        this.setAttribute(this.getAttribute(Attributes.ATTACK_SPEED), CaveNoise.CONFIG.ATTACK_RATE());
        this.setAttribute(this.getAttribute(Attributes.MOVEMENT_SPEED), CaveNoise.CONFIG.MOVEMENT_SPEED());
        this.setAttribute(this.getAttribute(Attributes.KNOCKBACK_RESISTANCE), CaveNoise.CONFIG.KNOCKBACK_RESISTANCE());
        this.setAttribute(this.getAttribute(Attributes.ARMOR), CaveNoise.CONFIG.ARMOR());

        SpawnGroupData data = super.finalizeSpawn(level, difficulty, reason, spawnData, tagData);

        // Ceiling check to prevent spawning in space it would suffocate in
        BlockPos ceilingBlock = this.blockPosition().above();
        if (!level.getBlockState(ceilingBlock).isAir()) {
            this.getEntityData().set(SQUEEZING_ACCESSOR, true);
            this.refreshDimensions();
        }

        return data;
    }

    private void setAttribute(AttributeInstance attribute, double value) {
        if (attribute != null) {
            attribute.setBaseValue(value);
            if (attribute.getAttribute() == Attributes.MAX_HEALTH) {
                this.setHealth((float) value);
            } else if (attribute.getAttribute() == Attributes.MOVEMENT_SPEED) {
                this.setSpeed((float) value);
            }
        }
    }

    public static AttributeSupplier getAttributeBuilder() {
        return Monster.createMobAttributes()
                .add(Attributes.MAX_HEALTH, CaveNoise.CONFIG.MAX_HEALTH())
                .add(Attributes.ATTACK_DAMAGE, CaveNoise.CONFIG.ATTACK_DAMAGE())
                .add(Attributes.ATTACK_SPEED, CaveNoise.CONFIG.ATTACK_RATE())
                .add(Attributes.MOVEMENT_SPEED, CaveNoise.CONFIG.MOVEMENT_SPEED())
                .add(Attributes.KNOCKBACK_RESISTANCE, CaveNoise.CONFIG.KNOCKBACK_RESISTANCE())
                .add(Attributes.ARMOR, CaveNoise.CONFIG.ARMOR())
                .add(Attributes.FOLLOW_RANGE, (double) CaveNoise.CONFIG.SPOTTING_RANGE())
                .build();
    }

    /// Main tick
    @Override
    public void tick() {
        --this.ticksUntilRemove;
        if (this.ticksUntilRemove <= 0) {
            this.disappear();
        }

        if (this.goalSelector.getAvailableGoals().isEmpty() || this.targetSelector.getAvailableGoals().isEmpty()) {
            this.registerGoals();
            this.goalSelector.tick();
            this.targetSelector.tick();
        }

        this.refreshDimensions();
        if (this.getTarget() != null) {
            this.targetIsLookingAtMe = this.isPlayerLookingTowards(this.getTarget());
        }

        boolean shouldCrouch = false;
        if (!(Boolean) this.getEntityData().get(SQUEEZING_ACCESSOR)) {
            boolean isTwoAboveSolid = this.level().getBlockState(this.blockPosition().above().above()).isSolid();
            Vec3i offset = new Vec3i(this.getDirection().getStepX(), this.getDirection().getStepY(), this.getDirection().getStepZ());
            boolean isFacingSolid = this.level().getBlockState(this.blockPosition().relative(this.getDirection())).isSolid();
            if (isFacingSolid) {
                offset = offset.offset(0, 1, 0);
            }

            boolean isOffsetFacingSolid = this.level().getBlockState(this.blockPosition().offset(offset)).isSolid();
            boolean isOffsetFacingTwoAboveSolid = this.level().getBlockState(this.blockPosition().offset(offset).above().above()).isSolid();
            boolean isOffsetFacingAboveSolid = this.level().getBlockState(this.blockPosition().relative(this.getDirection()).above()).isSolid();
            shouldCrouch = (isTwoAboveSolid || !isOffsetFacingSolid && !isOffsetFacingAboveSolid && isOffsetFacingTwoAboveSolid) && !this.isInWater();
        }

        if (shouldCrouch) {
            this.twoBlockSpaceTimer = this.twoBlockSpaceCooldown;
            this.inTwoBlockSpace = true;
        } else {
            --this.twoBlockSpaceTimer;
            if (this.twoBlockSpaceTimer <= 0.0F) {
                this.inTwoBlockSpace = false;
            }
        }

        if (this.level() instanceof ServerLevel) {
            if (this.isPassenger() || this.isHiding) {
                this.entityData.set(SPOTTED_ACCESSOR, false);
            }

            this.entityData.set(CROUCHING_ACCESSOR, this.inTwoBlockSpace);
        }

        // Play hurt animation
        if (this.hurtAnimationTicks > 0) {
            this.isPlayingHurtAnimation = true;
            --this.hurtAnimationTicks;
        } else {
            this.isPlayingHurtAnimation = false;
        }

        // Play any jaw movement
        if (this.jawAnimationTick >= 0) {
            this.jawAnimationTick++;
        }

        // Fading
        // State 1: Fade in
        // State -1: Fade out
        // State 0: Stop
        int fadeState = this.entityData.get(FADE_STATE_ACCESSOR);

        if (fadeState != 0 && !this.isPlayingDeathAnimation) {
            float currentAlpha = this.entityData.get(RENDER_ALPHA_ACCESSOR);
            float currentSpeed = this.entityData.get(FADE_SPEED_ACCESSOR);
            float changeAmount = currentSpeed * fadeState;
            this.targetAlpha = Mth.clamp(currentAlpha + changeAmount, 0.0F, 1.0F);
            this.entityData.set(RENDER_ALPHA_ACCESSOR, this.targetAlpha);

            // When the target alpha is reached, stop
            if (this.targetAlpha <= 0.0F && fadeState == -1) {
                this.entityData.set(FADE_STATE_ACCESSOR, 0);
                if (this.currentRoll != Roll.CHASE && this.shouldDisappearAfterFade) {
                    this.disappear();
                    this.shouldDisappearAfterFade = false;
                } else if (this.currentRoll == Roll.CHASE) {
                    this.entityData.set(FADE_STATE_ACCESSOR, 1);
                    this.shouldDisappearAfterFade = false;
                }
            } else if (this.targetAlpha == 1.0F && fadeState == 1) {
                this.entityData.set(FADE_STATE_ACCESSOR, 0);
            }
        }

        // Check if spotted
        if (this.entityData.get(SPOTTED_ACCESSOR)) {
            this.playSpottedSound();
        }

        // Reset gravity + physics to normal when not climbing unless dying
        if (!this.entityData.get(CLIMBING_ACCESSOR)) {
            this.noPhysics = false;
            this.setNoGravity(false);
        }
        
        this.playHeartbeatSound();
        this.playBreathingSound();

        super.tick();
    }

    /// Dimensions
    // Also check ModEntityTypes when modifying
    @Override
    public @NotNull EntityDimensions getDimensions(@NotNull Pose pose) {
        if ((Boolean) this.entityData.get(SQUEEZING_ACCESSOR)) {
            return new EntityDimensions(0.5F, 0.5F, true);
        } else {
            return (Boolean) this.entityData.get(CROUCHING_ACCESSOR) ? new EntityDimensions(0.5F, 1.9F, true) : super.getDimensions(pose);
        }
    }

    /// Roll
    public void reRoll() {
        this.stalking = false;
        this.currentRoll = Roll.fromValue(this.random.nextInt(5));
    }

    public void pickRoll(@NotNull List<Roll> rolls) {
        if (rolls.isEmpty()) {
            return;
        }

        this.stalking = false;
        this.currentRoll = rolls.get(this.random.nextInt(rolls.size()));
    }

    /// Animation
    private PlayState predicate(AnimationState<CaveDwellerEntity> state) {
        // 1. Death state evaluation takes priority
        if (this.isPlayingDeathAnimation()) {
            this.pleaseStopMoving = true;
            this.getNavigation().stop();
            return state.setAndContinue(this.DEATH);
        }

        if (this.isInStandoff()) {
            return PlayState.STOP;
        }

        // 2. Aggro state animations processing loop
        if (this.entityData.get(AGGRO_ACCESSOR)) {
            if (this.entityData.get(SQUEEZING_ACCESSOR)) {
                this.squeezingTicks = Utils.secondsToTicks(1);
                return state.setAndContinue(this.SQUEEZE);
            } else if (this.squeezingTicks > 0) {
                --this.squeezingTicks;
                return state.setAndContinue(this.SQUEEZE);
                //return state.setAndContinue(this.SQUEEZE_END);
            } else if (this.entityData.get(CROUCHING_ACCESSOR)) {
                return state.isMoving() ? state.setAndContinue(this.CROUCH_RUN) : state.setAndContinue(this.CROUCH_IDLE);
            } else if (this.entityData.get(CLIMBING_ACCESSOR)) {
                return state.setAndContinue(this.CLIMB);
            } else if (this.initializationDelayTicks > 0 && !state.isMoving() && this.canPlayChaseSound) {
                return state.setAndContinue(this.STALK_IDLE);
            } else {
                return state.isMoving() ? state.setAndContinue(this.CHASE) : state.setAndContinue(this.IDLE);
            }
        }
        // 3. Hiding state updates
        else if (this.entityData.get(HIDING_ACCESSOR)) {
            return state.setAndContinue(this.IDLE);
        }
        // 4. Stalking phase updates
        else if (this.entityData.get(STALKING_ACCESSOR)) {
            return state.isMoving() ? state.setAndContinue(this.SQUEEZE) : state.setAndContinue(this.STALK_IDLE);
        }
        // 5. Standoff state
        // Shouldn't be moving in this state, so there's no check for it
        else if (this.entityData.get(STANDOFF_ACCESSOR) && this.isInStandoff()) {
            return state.setAndContinue(this.STANDOFF);
        }
        // 6. Spotted state
        else if (this.entityData.get(SPOTTED_ACCESSOR) && !this.isInStandoff()) {
            return state.isMoving() ? state.setAndContinue(this.SQUEEZE) : state.setAndContinue(this.IDLE);
        }
        // 7. Default behavior
        return state.isMoving() ? state.setAndContinue(this.SQUEEZE) : state.setAndContinue(this.IDLE);
    }

    public void registerControllers(AnimatableManager.ControllerRegistrar controllerRegistrar) {
        controllerRegistrar.add(new AnimationController[]{
                (new AnimationController(this, "controller", 3, this::predicate))
                        .triggerableAnim("calm_run", this.CALM_RUN)
                        .triggerableAnim("calm_still", this.CALM_STILL)
                        .triggerableAnim("walk", this.CHASE)
                        .triggerableAnim("catch_up", this.CATCH_UP)
                        .triggerableAnim("idle", this.IDLE)
                        .triggerableAnim("crouch_run", this.CROUCH_RUN)
                        .triggerableAnim("crouch_idle", this.CROUCH_IDLE)
                        .triggerableAnim("is_spotted", this.IS_SPOTTED)
                        .triggerableAnim("squeeze", this.SQUEEZE)
                        .triggerableAnim("squeeze_end", this.SQUEEZE_END)
                        .triggerableAnim("death", this.DEATH)
                        .triggerableAnim("hurt", this.HURT)
                        .triggerableAnim("standoff", this.STANDOFF),

                new AnimationController<>(this, "override_controller", 3, state -> {
                    if (this.isInStandoff()) {
                        return state.setAndContinue(this.STANDOFF);
                    }
                    return software.bernie.geckolib.core.object.PlayState.STOP;
                })
        });
    }

    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    /// Standoff
    public boolean isInStandoff() {
        return this.entityData.get(STANDOFF_ACCESSOR);
    }

    public void setInStandoff(boolean inStandoff) {
        this.entityData.set(STANDOFF_ACCESSOR, inStandoff);
    }

    /// Jaw code
    public void triggerJawMovement(float speed, float distance, float holdTime) {
        if (this.canPlayJawMovement) {
            if (!this.level().isClientSide()) {
                this.entityData.set(JAW_SPEED, speed);
                this.entityData.set(JAW_DISTANCE, distance);
                this.entityData.set(JAW_HOLD, holdTime);

                this.level().broadcastEntityEvent(this, (byte) 105);
            }
        }
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == 105) {
            if (this.jawAnimationTick == -1) {
                this.jawAnimationTick = 0;
            }
        } else {
            super.handleEntityEvent(id);
        }
    }

    public float getJawSpeed() {
        return this.entityData.get(JAW_SPEED);
    }

    public float getJawDistance() {
        return this.entityData.get(JAW_DISTANCE);
    }

    public float getJawHoldTime() {
        return this.entityData.get(JAW_HOLD);
    }

    public int getJawAnimationTick() {
        return this.jawAnimationTick;
    }

    public void resetJawAnimation() {
        this.jawAnimationTick = -1;
    }

    /// Fading
    public float getRenderAlpha() {
        return this.entityData.get(RENDER_ALPHA_ACCESSOR);
    }

    // State 1: Fade in
    // State -1: Fade out
    // State 0: Stop
    public void setFadeState(int fadeState, float fadeSpeed, boolean shouldDisappearAfter) {
        this.entityData.set(FADE_STATE_ACCESSOR, fadeState);
        this.entityData.set(FADE_SPEED_ACCESSOR, fadeSpeed);

        if (shouldDisappearAfter) {
            this.shouldDisappearAfterFade = true;
        }
    }

    /// Death
    /// (Transparency + jaw movement controller is in CaveDwellerRenderer)
    @Override
    public void die(DamageSource damageSource) {
        super.die(damageSource);
        this.isPlayingDeathAnimation = true;
        // shouldDisappearAfter set to false here because it effectively already does so upon die() being called
        this.setFadeState(-1, 1.0F / (float) this.deathAnimationLength, false);
    }

    @Override
    protected void tickDeath() {
        this.deathAnimationTicks++;
        float progress = ((float) this.deathAnimationTicks / (float) this.deathAnimationLength) + timingOffset;

        // Make dweller invisible to prevent transparency rendering issues
        // There are still rendering priority issues, but this is a band-aid fix
        if (progress >= 1.0F && !this.hasEffect(MobEffects.INVISIBILITY)) {
            this.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 999999, 100, true, false));
        }
        if (this.deathAnimationTicks >= this.deathAnimationLength) {
            super.tickDeath();
        } else {
            this.deathTime = 0;
        }
    }

    @Override
    protected void dropAllDeathLoot(DamageSource pSource) {
        this.setItemSlot(EquipmentSlot.FEET, ItemStack.EMPTY);
        super.dropAllDeathLoot(pSource);
        if (!this.alreadyPlayedDeathSound && this.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            deathSound = this.chooseDeathSound();
            this.triggerJawMovement(5.0F, 6.0F, 5.0F);
            for (net.minecraft.server.level.ServerPlayer player : serverLevel.players()) {
                if (player.distanceToSqr(this) < 64 * 64) {
                    player.connection.send(new net.minecraft.network.protocol.game.ClientboundSoundPacket(
                            net.minecraft.core.registries.BuiltInRegistries.SOUND_EVENT.wrapAsHolder(deathSound),
                            net.minecraft.sounds.SoundSource.HOSTILE,
                            this.getX(), this.getY(), this.getZ(),
                            12.0F, 1.0F,
                            this.level().random.nextLong()
                    ));
                }
            }
        }

        this.alreadyPlayedDeathSound = true;
    }

    public boolean isPlayingDeathAnimation() {
        return this.isPlayingDeathAnimation;
    }

    /// Hurt
    @Override
    public boolean hurt(net.minecraft.world.damagesource.DamageSource source, float amount) {
        boolean tookDamage = super.hurt(source, amount);

        if (tookDamage) {
            this.hurtAnimationTicks = this.hurtAnimationLength;
            // If hurt (and idle), play hurt animation
            if (this.isPlayingHurtAnimation()) {
                if (!this.isMoving()) {
                    // Ignore the warning; triggerAnim is NOT being overridden here
                    this.triggerAnim("controller", "hurt");
                }
            }

            net.minecraft.world.entity.Entity attacker = source.getEntity();

            if (Utils.isValidPlayer(attacker)) {
                this.setTarget((net.minecraft.world.entity.LivingEntity) attacker);
                this.stalking = false;
                this.getEntityData().set(CaveDwellerEntity.AGGRO_ACCESSOR, true);
                this.currentRoll = Roll.CHASE;
            }
        }

        return tookDamage;
    }

    public boolean isPlayingHurtAnimation() {
        return this.isPlayingHurtAnimation;
    }

    /// Sound
    @Override
    protected void playStepSound(@NotNull BlockPos pPos, @NotNull BlockState pState) {
        super.playStepSound(pPos, pState);
        this.playEntitySound(this.chooseStep());
    }

    // Shortcut for the one below
    private void playEntitySound(SoundEvent soundEvent) {
        this.playEntitySound(soundEvent, 1.0F, 1.0F);
    }

    private void playEntitySound(SoundEvent soundEvent, float volume, float pitch) {
        this.level().playSound((Player) null, this, soundEvent, SoundSource.HOSTILE, volume, pitch);
    }

    // TODO: Check if unused
    // I think the purpose of this was to play a fake block breaking sound? That's what it does in Bedrock
    private void playBlockPosSound(SoundEvent soundEvent, float volume, float pitch) {
        BlockPos blockPos = BlockPos.containing(this.position());
        Minecraft.getInstance().getSoundManager().play(new SimpleSoundInstance(soundEvent, SoundSource.HOSTILE, volume, pitch, RandomSource.create(), blockPos));
    }

    public void playChaseSound() {
        if (!this.canPlayChaseSound) {
            this.canPlayJawMovement = true;
            int holdTime = Utils.ticksToSeconds((this.initializationDelayTicks * 3) + 10);
            this.triggerJawMovement(4.0F,6.5F, holdTime);
            this.canPlayJawMovement = false;
            switch (this.random.nextInt(4)) {
                case 0 -> this.playEntitySound(ModSounds.TRANSITION_1, 3.0F, 1.0F);
                case 1 -> this.playEntitySound(ModSounds.TRANSITION_2, 3.0F, 1.0F);
                case 2 -> this.playEntitySound(ModSounds.TRANSITION_3, 3.0F, 1.0F);
                case 3 -> this.playEntitySound(ModSounds.TRANSITION_4, 3.0F, 1.0F);
            }

            this.chaseSoundClock = this.initializationDelayTicks;
            this.canPlayChaseSound = true;
        }

        --this.chaseSoundClock;

        if (this.canPlayChaseSound) {
            if (this.chaseSoundClock <= 0) {
                this.triggerJawMovement(4.0F, 6.0F, 1.8F);
                this.canPlayJawMovement = true;
                switch (this.random.nextInt(4)) {
                    case 0 -> this.playEntitySound(ModSounds.CHASE_1, 3.0F, 1.0F);
                    case 1 -> this.playEntitySound(ModSounds.CHASE_2, 3.0F, 1.0F);
                    case 2 -> this.playEntitySound(ModSounds.CHASE_3, 3.0F, 1.0F);
                    case 3 -> this.playEntitySound(ModSounds.CHASE_4, 3.0F, 1.0F);
                }

                this.canPlayChaseSound = true;
                this.resetChaseSoundClock();
            }
        }
    }
    
    private void resetChaseSoundClock() {
        this.chaseSoundClockReset = this.random.nextIntBetweenInclusive(200, 300);
        this.chaseSoundClock = this.chaseSoundClockReset;
    }

    public void playDisappearSound() {
        Level currentLevel = this.level();

        if (!currentLevel.isClientSide()) {
            currentLevel.playSound(
                    null,
                    this.getX(),
                    this.getY(),
                    this.getZ(),
                    ModSounds.DISAPPEAR,
                    SoundSource.HOSTILE,
                    1.0F,
                    1.0F
            );
        }
    }

    @Override
    protected void playHurtSound(DamageSource pSource) {
        SoundEvent soundevent = this.chooseHurtSound();
        if (soundevent != null) {
            this.playEntitySound(soundevent, 1.0F, randomPitch);
        }
    }

    @Override
    protected float getSoundVolume() {
        return 0.4F;
    }

    protected float getRandomPitch() {
        float pitch = this.random.nextFloat();
        return Mth.clamp(pitch, 0.9F, 1.1F);
    }

    public void playBreathingSound() {
        if (!this.startedPlayingBreathingSound && !this.isDeadOrDying()) {
            if (this.breathingSoundClock <= 0) {
                if ((!this.isMoving() && !this.isAggro && !this.stalking)) {
                    if (this.random.nextBoolean()) {
                        this.playEntitySound(ModSounds.IDLE_1, 3.0F, randomPitch);
                    } else {
                        this.playEntitySound(ModSounds.IDLE_2, 3.0F, randomPitch);
                    }
                    // TODO: prevent breathing from playing if playing chase sound?
                } else if (this.isMoving() && this.isAggro) {
                    switch (this.random.nextInt(3)) {
                        case 0 -> this.playEntitySound(ModSounds.BREATHING_1, 3.0F, 1.0F);
                        case 1 -> this.playEntitySound(ModSounds.BREATHING_2, 3.0F, 1.0F);
                        case 2 -> this.playEntitySound(ModSounds.BREATHING_3, 3.0F, 1.0F);
                    }
                } else {
                    return;
                }

                this.startedPlayingBreathingSound = true;
                this.resetBreathingSoundClock();
            }

            --this.breathingSoundClock;
        }
    }

    private void resetBreathingSoundClock() {
        if (this.isAggro) {
            this.breathingSoundClockReset = this.random.nextIntBetweenInclusive(70,110);
        } else {
            this.breathingSoundClockReset = this.random.nextIntBetweenInclusive(80,150);
        }

        this.startedPlayingBreathingSound = false;
        this.breathingSoundClock = this.breathingSoundClockReset;
    }

    public void playHeartbeatSound() {
        LivingEntity target = this.getTarget();

        if (!this.startedPlayingHeartbeatSound && Utils.isValidPlayer(target)) {
            if (this.heartbeatSoundClock <= 0) {
                if (target instanceof ServerPlayer serverPlayer) {
                    float pitch = this.isAggro ? 1.1F : 1.0F;
                    serverPlayer.connection.send(new ClientboundSoundPacket(
                            BuiltInRegistries.SOUND_EVENT.wrapAsHolder(ModSounds.HEARTBEAT),
                            SoundSource.HOSTILE,
                            serverPlayer.getX(),
                            serverPlayer.getY(),
                            serverPlayer.getZ(),
                            1.0F,
                            pitch,
                            serverPlayer.getRandom().nextLong()
                    ));
                }

                this.startedPlayingHeartbeatSound = true;
                this.resetHeartbeatSoundClock();
            }

            --this.heartbeatSoundClock;
        }
    }

    private void resetHeartbeatSoundClock() {
        if (this.isAggro) {
            this.heartbeatSoundClockReset = 12;
        } else {
            this.heartbeatSoundClockReset = 20;
        }

        this.startedPlayingHeartbeatSound = false;
        this.heartbeatSoundClock = this.heartbeatSoundClockReset;
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
            // 11 blocks squared = 121
            double spottedSoundDistance = 121.0D;
            SoundEvent selectedSound;
            if (this.distanceToSqr(player) <= spottedSoundDistance) {
                selectedSound = switch (this.random.nextInt(3)) {
                    case 0 -> ModSounds.SPOTTED_1;
                    case 1 -> ModSounds.SPOTTED_2;
                    default -> ModSounds.SPOTTED_3;
                };
            } else {
                selectedSound = ModSounds.SPOTTED_DISTANT;
            }

            //player.playNotifySound(selectedSound, SoundSource.HOSTILE, 1.0F, 1.0F);
            this.level().playSound((Player) null, this.getX(), this.getY(), this.getZ(), selectedSound, SoundSource.HOSTILE, 3.0F, 1.0F);
        }

        this.alreadyPlayedSpottedSound = true;
    }

    public void playClimbSound() {
        if (this.climbSoundClock <= 0) {
            switch (this.random.nextInt(4)) {
                case 0 -> this.playEntitySound(ModSounds.CLIMB_1, 1.0F, 1.0F);
                case 1 -> this.playEntitySound(ModSounds.CLIMB_2, 1.0F, 1.0F);
                case 2 -> this.playEntitySound(ModSounds.CLIMB_3, 1.0F, 1.0F);
                case 3 -> this.playEntitySound(ModSounds.CLIMB_4, 1.0F, 1.0F);
            }

            this.resetClimbSoundClock();
        }

        --this.climbSoundClock;
    }

    private void resetClimbSoundClock() {
        this.climbSoundClock = this.climbSoundClockReset;
    }

    /// In line of sight
    // Occlusion check
    // Checks in a straight line if there are any blocks in-between player and dweller
    public boolean inPlayerLineOfSight() {
        return this.getTarget() != null ? this.getTarget().hasLineOfSight(this) : false;
    }

    // In view check
    // Checks in a cone if the dweller is inside it, regardless of blocks
    public boolean isPlayerLookingTowards(Entity target) {
        if (!Utils.isValidPlayer(target)) {
            return false;
        } else {
            float fov = 70.0F;
            float yFovMod = 0.65F;
            float fovMod = (35.0F / fov - 1.0F) * 0.4F + 1.0F;
            fov *= fovMod;

            Vec3 a = target.position();
            Vec3 b = this.position();

            Vec2 dist = new Vec2((float)b.x - (float)a.x, (float)b.z - (float)a.z);
            dist = dist.normalized();
            double newAngle = Math.toDegrees(Math.atan2((double)dist.x, (double)dist.y));

            float lookX = (float)target.getViewVector(1.0F).x;
            float lookZ = (float)target.getViewVector(1.0F).z;
            double newLookAngle = Math.toDegrees(Math.atan2((double)lookX, (double)lookZ));

            double newNewAngle = this.loopAngle(newAngle - newLookAngle) + (double)fov;
            newNewAngle = this.loopAngle(newNewAngle);

            boolean yawPlayerLookingTowards = newNewAngle > 0.0D && newNewAngle < (double)(fov * 2.0F);

            boolean pitchPlayerLookingTowards = false;
            boolean shouldOnlyUsePitch = false;
            float yFov = fov * yFovMod;

            Vec2 yDist = new Vec2((float)Math.sqrt((b.x - a.x) * (b.x - a.x) + (b.z - a.z) * (b.z - a.z)), (float)(b.y - a.y));
            yDist = yDist.normalized();
            double yAngle = Math.toDegrees(Math.atan2((double)yDist.x, (double)yDist.y));

            float lookY = (float)target.getViewVector(1.0F).y;
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
        if (angle > 360.0D) {
            return angle - 360.0D;
        } else {
            return angle < 0.0D ? angle + 360.0D : angle;
        }
    }

    public void disappear() {
        this.playDisappearSound();
        this.delete();
    }

    public void delete() {
        this.discard();
    }

    /// Choosing sounds
    private SoundEvent chooseStep() {
        
        switch (this.random.nextInt(4)) {
            case 0 -> {
                return ModSounds.STEP_1;
            }
            case 1 -> {
                return ModSounds.STEP_2;
            }
            case 2 -> {
                return ModSounds.STEP_3;
            }
            default -> {
                return ModSounds.STEP_4;
            }
        }
    }

    private SoundEvent chooseHurtSound() {
        
        switch (this.random.nextInt(4)) {
            case 0 -> {
                return ModSounds.HURT_1;
            }
            case 1 -> {
                return ModSounds.HURT_2;
            }
            case 2 -> {
                return ModSounds.HURT_3;
            }
            default -> {
                return ModSounds.HURT_4;
            }
        }
    }

    protected SoundEvent chooseDeathSound() {
        
        switch (this.random.nextInt(4)) {
            case 0 -> {
                return ModSounds.DWELLER_DEATH_1;
            }
            case 1 -> {
                return ModSounds.DWELLER_DEATH_2;
            }
            case 2 -> {
                return ModSounds.DWELLER_DEATH_3;
            }
            default -> {
                return ModSounds.DWELLER_DEATH_4;
            }
        }
    }

    @Override
    protected SoundEvent getHurtSound(@NotNull DamageSource damageSourceIn) {
        return this.chooseHurtSound();
    }

    /// Pathfinding
    // Prevents getting stuck
    public boolean teleportToNextPathPoint() {
        net.minecraft.world.level.pathfinder.Path activePath = this.getNavigation().getPath();

        if (activePath == null || activePath.isDone()) {
            return false;
        } else {

            int nextNodeIndex = activePath.getNextNodeIndex();

            if (nextNodeIndex >= activePath.getNodeCount()) {
                return false;
            }

            net.minecraft.world.level.pathfinder.Node nextNode = activePath.getNode(nextNodeIndex);

            double d1 = nextNode.x + 0.5D;
            double d2 = nextNode.y;
            double d3 = nextNode.z + 0.5D;

            BlockPos.MutableBlockPos validPosition = new BlockPos.MutableBlockPos(d1, d2, d3);

            while (validPosition.getY() > this.level().getMinBuildHeight() && this.level().getBlockState(validPosition).isAir()) {
                if (!this.entityData.get(SQUEEZING_ACCESSOR)) {
                    this.entityData.set(SQUEEZING_ACCESSOR, true);
                }
                validPosition.move(Direction.DOWN);
            }

            this.teleportTo((double) validPosition.getX() + 0.5D, (double) (validPosition.getY() + 1), (double) validPosition.getZ() + 0.5D);
            return true;
        }
    }

    public boolean isMoving() {
        Vec3 velocity = this.getDeltaMovement();
        float avgVelocity = (float) (Math.abs(velocity.x) + Math.abs(velocity.z)) / 2.0F;
        return avgVelocity > 0.02F;
    }

    // Hacky pathfinding fix
    public Path createShortPath(LivingEntity pathTarget) {
        this.refreshDimensions();
        this.setMaxUpStep(100.0F);
        Path shortPath = this.getNavigation().createPath(pathTarget, 0);
        this.setMaxUpStep(this.defaultMaxUpStep);
        this.refreshDimensions();
        return shortPath;
    }

    public Path getShortPath(LivingEntity target) {
        return this.shortPath = this.createShortPath(target);
    }

    public Path createClimbPath(LivingEntity pathTarget) {
        this.setMaxUpStep(100.0F);
        Path climbPath = this.getNavigation().createPath(pathTarget, 0);
        this.setMaxUpStep(this.defaultMaxUpStep);
        return climbPath;
    }

    @Override
    protected @NotNull PathNavigation createNavigation(@NotNull Level level) {
        WallClimberNavigation navigation = new WallClimberNavigation(this, level);

        navigation.setCanOpenDoors(true);
        navigation.setCanPassDoors(true);
        navigation.setCanWalkOverFences(true);
        navigation.setCanFloat(true);

        this.waterNavigation = new net.minecraft.world.entity.ai.navigation.WaterBoundPathNavigation(this, level());
        this.setPathfindingMalus(net.minecraft.world.level.pathfinder.BlockPathTypes.WATER, 0.0F);
        this.setPathfindingMalus(net.minecraft.world.level.pathfinder.BlockPathTypes.LAVA, 0.0F);

        return navigation;
    }

    @Override
    public @NotNull net.minecraft.world.entity.ai.navigation.PathNavigation getNavigation() {
        if (this.isInWater()) {
            this.setSwimming(true);
            this.getEntityData().set(CaveDwellerEntity.SQUEEZING_ACCESSOR, false);
            return this.waterNavigation;
        } else {
            this.setSwimming(false);
            return super.getNavigation();
        }
    }

    @Override
    public void customServerAiStep() {
        super.customServerAiStep();

        net.minecraft.world.entity.ai.navigation.PathNavigation mainNav = this.getNavigation();

        if (this.isInWater() && mainNav.getPath() != null && !mainNav.getPath().isDone()) {

            net.minecraft.world.level.pathfinder.Node nextNode = mainNav.getPath().getNextNode();

            if (nextNode != null) {
                if (nextNode.y > this.getY()) {
                    this.setDeltaMovement(this.getDeltaMovement().add(0.0, 0.06, 0.0));
                } else if (nextNode.y <= this.getY()) {
                    this.setDeltaMovement(this.getDeltaMovement().add(0.0, -0.06, 0.0));
                }
            }
        }
    }

    @Override
    public void aiStep() {
        super.aiStep();

        if (this.isEffectiveAi() && this.isInWater()) {
            this.waterNavigation.tick();
        }
    }

    @Override
    protected float getWaterSlowDown() {
        return 1.0F; // default is 0.8
    }

    // Disable jumping since it has auto step-up
    // Only jumps when not climbing or in water, and the target is 2 blocks higher
    @Override
    public void setJumping(boolean jumping) {
        LivingEntity target = this.getTarget();

        boolean shouldJump = jumping;

        if (target != null) {
            double yDiff = target.getY() - this.getY();

            if (yDiff > 2.0 && yDiff <= 3.0 && (!this.getEntityData().get(CaveDwellerEntity.CLIMBING_ACCESSOR) && !this.isSubmerged)) {
                shouldJump = true;
            }
        }

        super.setJumping(shouldJump);
    }

    /// Custom name code
    @Override
    public boolean isCustomNameVisible() {
        return false;
    }

    @Override
    public boolean hasCustomName() {
        return false;
    }

    @Override
    public Component getCustomName() {
        return this.caveDwellerName != null ? Component.literal(this.caveDwellerName) : null;
    }

    @Override
    public Component getDisplayName() {
        return this.caveDwellerName != null ? Component.literal(this.caveDwellerName) : super.getDisplayName();
    }

    /// Accessors
    static {
        HIDING_ACCESSOR = SynchedEntityData.defineId(CaveDwellerEntity.class, EntityDataSerializers.BOOLEAN);
        CROUCHING_ACCESSOR = SynchedEntityData.defineId(CaveDwellerEntity.class, EntityDataSerializers.BOOLEAN);
        AGGRO_ACCESSOR = SynchedEntityData.defineId(CaveDwellerEntity.class, EntityDataSerializers.BOOLEAN);
        SQUEEZING_ACCESSOR = SynchedEntityData.defineId(CaveDwellerEntity.class, EntityDataSerializers.BOOLEAN);
        SPOTTED_ACCESSOR = SynchedEntityData.defineId(CaveDwellerEntity.class, EntityDataSerializers.BOOLEAN);
        STANDOFF_ACCESSOR = SynchedEntityData.defineId(CaveDwellerEntity.class, EntityDataSerializers.BOOLEAN);
        CLIMBING_ACCESSOR = SynchedEntityData.defineId(CaveDwellerEntity.class, EntityDataSerializers.BOOLEAN);
        STALKING_ACCESSOR = SynchedEntityData.defineId(CaveDwellerEntity.class, EntityDataSerializers.BOOLEAN);
    }
}
