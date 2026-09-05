package com.thecaptain.cavedweller.mixin;

import com.thecaptain.cavedweller.CaveDweller;
import com.thecaptain.cavedweller.block.BurntOutTorchBlock;
import com.thecaptain.cavedweller.block.BurntOutWallTorchBlock;
import com.thecaptain.cavedweller.entities.CaveDwellerEntity;
import com.thecaptain.cavedweller.registry.ModBlocks;
import com.thecaptain.cavedweller.registry.ModEntityTypes;
import com.thecaptain.cavedweller.registry.ModSounds;
import com.thecaptain.cavedweller.util.Utils;

import java.util.*;
import java.util.function.BooleanSupplier;

import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerLevel.class)
public abstract class MixinServerWorld {
    @Shadow
    @Final
    private List<ServerPlayer> players;
    private boolean doReload = true;
    private final java.util.List<net.minecraft.server.level.ServerPlayer> spelunkers = new java.util.ArrayList<>();
    java.util.Map<java.util.UUID, PlayerData> playerTimerLedger = new java.util.HashMap<>();
    private final Random random = new Random();
    private boolean dwellerExistsFlag;
    private boolean debug = CaveDweller.CONFIG.DEBUG();
    private int scanTicks = 0;
    private int randomlySelectedBrightnessLevel;
    private boolean shouldTickTimers;
    private boolean isSpawnedStalking;
    private Vec3 caveDwellerPos;
    private boolean notLookingAtDweller;
    private boolean hasLedgerBeenClearedUponReload = false;
    // Scoreboard
    private String calmTimerMinsAndSecs;
    private String vanillaNoiseTimerMinsAndSecs;
    private String dwellerNoiseTimerMinsAndSecs;
    private String stalkNoiseTimerMinsAndSecs;
    private float phase1StartPercent;
    private String phase1StartMinsAndSecs;
    private float phase2StartPercent;
    private String phase2StartMinsAndSecs;
    private int vanillaNoiseTimerSecs;
    private int dwellerNoiseTimerSecs;
    private int spawnAttemptTimerSecs;
    private int stalkNoiseTimerSecs;
    private String timerInactive = "§c§lINACTIVE";
    private int dwellerAliveTimer = 0;
    private boolean isDwellerCurrentlyAggro = false;
    private Enum currentGoal;

    public MixinServerWorld() {
    }

    /// Main tick
    public abstract @Nullable ServerLevel getLevel(ResourceKey<Level> var1);

    public abstract ServerLevel overworld();

    public static class PlayerData {
        private final net.minecraft.util.RandomSource random = net.minecraft.util.RandomSource.create();
        public final java.util.List<ActiveBurnout> activeBurnouts = new java.util.ArrayList<>();
        public int calmTimer;
        public int gracePeriod;
        public int gracePeriodMax;
        public int gracePeriodTimer;
        public int vanillaCaveNoiseTimer;
        public int dwellerCaveNoiseTimer;
        public int stalkNoiseTimer;
        public boolean currentlyPlayingNoise = false;
        public int cooldown = (Utils.secondsToTicks(CaveDweller.CONFIG.RESET_CALM_MAX()) * 2);
        public int calmTimerMax;
        public float phase1StartPercentDecimal;
        public float phase2StartPercentDecimal;
        public int activePhase;
        public int ticksUntilNextPhase;
        public int vanillaStartGate;
        public int dwellerStartGate;
        public String currentActivePhaseName;
        public int burnoutEventTimer;

        public PlayerData() {
            this.resetDwellerCaveNoiseTimer();
            this.resetVanillaCaveNoiseTimer();
            this.resetStalkNoiseTimer();
            this.resetBurnoutEventTimer();
            this.gracePeriod = Utils.secondsToTicks(CaveDweller.CONFIG.GRACE_PERIOD_BEFORE_RESET());
        }

        public static class ActiveBurnout {
            public final BlockPos pos;
            public final BlockState originalState;
            public int ticksLeft;

            public ActiveBurnout(BlockPos pos, BlockState originalState, int durationTicks) {
                this.pos = pos;
                this.originalState = originalState;
                this.ticksLeft = durationTicks;
            }
        }

        /// Reset timers
        public void resetAll(ServerLevel overworld, boolean dwellerExists) {
            this.resetCalmTimer();
            this.caveNoiseTimerCheck();

            if (dwellerExists) {
                this.resetStalkNoiseTimer();
            }
        }

        public void resetCalmTimer() {
            int min = Utils.secondsToTicks(CaveDweller.CONFIG.RESET_CALM_MIN());
            int max = Utils.secondsToTicks(CaveDweller.CONFIG.RESET_CALM_MAX());

            if (max < min) {
                int correctedMax = min;
                min = max;
                max = correctedMax;
            } else if (min == max) {
                max += 30;
            }

            if (this.random.nextDouble() <= CaveDweller.CONFIG.RESET_CALM_COOLDOWN_CHANCE()) {
                this.calmTimer = this.cooldown;
                this.calmTimerMax = this.cooldown;
            } else {
                int range = (max - min) + 1;
                this.calmTimer = min + this.random.nextInt(Math.max(1, range));
                this.calmTimerMax = max;
            }
        }

        public void resetVanillaCaveNoiseTimer() {
            int currentVanillaStart = Math.round(this.calmTimerMax * (float) this.phase1StartPercentDecimal);
            int currentVanillaEnd = Math.round(this.calmTimerMax * 0.08F);

            int vanillaDenominator = Math.max(1, currentVanillaStart - currentVanillaEnd);
            float progress = (float) (this.calmTimer - currentVanillaEnd) / vanillaDenominator;
            float weight = Mth.clamp(1.0F - progress, 0.1F, 1.0F);
            int startMinTicks = Utils.secondsToTicks(CaveDweller.CONFIG.RESET_VANILLA_CAVE_NOISE_START_MIN());
            int endMinTicks = Math.max(100, startMinTicks / 10);
            int min = Math.round((float) (endMinTicks - startMinTicks) * weight + (float) startMinTicks);
            int startMaxTicks = startMinTicks * 5 / 2;
            int endMaxTicks = endMinTicks * 2;
            int max = Math.round((float) (endMaxTicks - startMaxTicks) * weight + (float) startMaxTicks);

            if (max < min) {
                int correctedMax = min;
                min = max;
                max = correctedMax;
            } else if (min == max) {
                max += 30;
            }

            int range = Math.max(1, (max - min) + 1);
            this.vanillaCaveNoiseTimer = min + this.random.nextInt(range);
        }

        public void resetDwellerCaveNoiseTimer() {
            int currentDwellerStart = Math.round(this.calmTimerMax * (float) this.phase2StartPercentDecimal);
            int currentDwellerEnd = Math.round(this.calmTimerMax * 0.04F);

            int dwellerDenominator = Math.max(1, currentDwellerStart - currentDwellerEnd);
            float progress = (float) (this.calmTimer - currentDwellerEnd) / dwellerDenominator;
            float weight = Mth.clamp(1.0F - progress, 0.1F, 1.0F);

            int min = Utils.secondsToTicks(CaveDweller.CONFIG.RESET_DWELLER_CAVE_NOISE_MIN());
            int max = min * 2;

            if (max < min) {
                int correctedMax = min;
                min = max;
                max = correctedMax;
            }

            int dynamicGap = Math.round((float) (max - min) * (1.0F - weight));
            int range = Math.max(1, dynamicGap + 1);
            this.dwellerCaveNoiseTimer = min + this.random.nextInt(range);
        }

        public void resetBurnoutEventTimer() {
            long averageBurnInterval;

            if (this.calmTimerMax <= Utils.minutesToTicks(10)) {
                averageBurnInterval = this.calmTimerMax / 2;
            } else {
                averageBurnInterval = Utils.minutesToTicks(4);
            }

            long variation = (long) ((Math.random() - 0.5) * Utils.minutesToTicks(2));
            long nextInterval = Math.max(Utils.secondsToTicks(30), averageBurnInterval + variation);

            this.burnoutEventTimer = (int) nextInterval + Utils.minutesToTicks(2);
        }

        // Ensures at least one cave sound plays during its phase, provided the next phase is not happening in (MinimumSpaceBetweenNoises) ticks/seconds or less
        public void caveNoiseTimerCheck() {
            int MinimumSpaceBetweenVanillaNoises = Utils.secondsToTicks(CaveDweller.CONFIG.RESET_VANILLA_CAVE_NOISE_START_MIN());
            int MinimumSpaceBetweenDwellerNoises = Utils.secondsToTicks(CaveDweller.CONFIG.RESET_DWELLER_CAVE_NOISE_MIN());
            int MinimumSpaceBetweenStalkNoises = Utils.secondsToTicks(CaveDweller.CONFIG.RESET_STALK_NOISE_MIN());

            switch (this.activePhase) {
                case 1 -> {
                    if (this.vanillaCaveNoiseTimer > MinimumSpaceBetweenVanillaNoises) {
                        if (this.calmTimer < this.vanillaCaveNoiseTimer) {
                            if (this.vanillaCaveNoiseTimer > this.ticksUntilNextPhase) {
                                if (this.ticksUntilNextPhase > MinimumSpaceBetweenVanillaNoises) {
                                    this.vanillaCaveNoiseTimer = (this.random.nextInt(Math.max(1, this.ticksUntilNextPhase))) + MinimumSpaceBetweenVanillaNoises;
                                }
                            }
                        }
                    }
                }
                case 2 -> {
                    if (this.dwellerCaveNoiseTimer > MinimumSpaceBetweenDwellerNoises) {
                        if (this.calmTimer < this.dwellerCaveNoiseTimer) {
                            if (this.dwellerCaveNoiseTimer > this.ticksUntilNextPhase) {
                                if (this.ticksUntilNextPhase > MinimumSpaceBetweenDwellerNoises) {
                                    this.dwellerCaveNoiseTimer = (this.random.nextInt(Math.max(1, this.ticksUntilNextPhase))) + MinimumSpaceBetweenDwellerNoises;
                                }
                            }
                        }
                    }
                }
                case 3 -> {
                    if (this.stalkNoiseTimer > MinimumSpaceBetweenStalkNoises) {
                        if (this.calmTimer < this.stalkNoiseTimer) {
                            if (this.stalkNoiseTimer > this.ticksUntilNextPhase) {
                                if (this.ticksUntilNextPhase > MinimumSpaceBetweenStalkNoises) {
                                    this.stalkNoiseTimer = (this.random.nextInt(Math.max(1, this.ticksUntilNextPhase))) + MinimumSpaceBetweenStalkNoises;
                                }
                            }
                        }
                    }
                }
            }
        }

        public void resetStalkNoiseTimer() {
            int min = CaveDweller.CONFIG.RESET_STALK_NOISE_MIN();
            int max = min * 2;

            if (max < min) {
                int temp = min;
                min = max;
                max = temp;
            }
            this.stalkNoiseTimer = this.random.nextInt(Utils.secondsToTicks(min), Utils.secondsToTicks(max + 1));
        }
    }

    /// Timers
    // Phase 1: Vanilla Noises
    public boolean playVanillaCaveNoiseToSpelunkers(ServerPlayer player, PlayerData data) {

        double targetX = player.getX() + (double) (-6 + this.random.nextInt(13));
        double targetY = player.getY();
        double targetZ = player.getZ() + (double) (-6 + this.random.nextInt(13));

        ServerLevel serverLevel = player.serverLevel();
        serverLevel.playSound(null, targetX, targetY, targetZ, SoundEvents.AMBIENT_CAVE.value(), SoundSource.AMBIENT, 1.0F, 1.0F);

        data.currentlyPlayingNoise = false;

        return true;
    }

    // Phase 2: Dweller Noises
    public boolean playDwellerCaveNoiseToSpelunkers(ServerPlayer player, PlayerData data) {

        int currentDwellerStart = Math.round(data.calmTimerMax * (float) data.phase2StartPercentDecimal);
        int currentDwellerEnd = Math.round(data.calmTimerMax * 0.04F);

        int dwellerDenominator = Math.max(1, currentDwellerStart - currentDwellerEnd);
        float alpha = (float) (data.calmTimer - currentDwellerEnd) / dwellerDenominator;
        float weight = Mth.clamp(1.0F - alpha, 0.1F, 1.0F);

        float minVol = 0.1F;
        float maxVol = 1.0F;
        float dynamicVolume = minVol + (maxVol - minVol) * weight;

        double targetX = player.getX() + (double) (-6 + this.random.nextInt(13));
        double targetY = player.getY();
        double targetZ = player.getZ() + (double) (-6 + this.random.nextInt(13));

        ServerLevel serverLevel = player.serverLevel();
        serverLevel.playSound(null, targetX, targetY, targetZ, ModSounds.CAVEDWELLER_AMBIENT, SoundSource.AMBIENT, dynamicVolume, 1.0F);
        data.currentlyPlayingNoise = false;

        return true;
    }

    // Phase 3: Stalk / Failed spawn attempt Noises
    public boolean playStalkNoiseToSpelunkers(ServerPlayer player, PlayerData data) {
        double targetX = player.getX() + (double) (-6 + this.random.nextInt(13));
        double targetY = player.getY();
        double targetZ = player.getZ() + (double) (-6 + this.random.nextInt(13));

        ServerLevel serverLevel = player.serverLevel();
        serverLevel.playSound(null, targetX, targetY, targetZ, ModSounds.CAVEDWELLER_STALK, SoundSource.AMBIENT, 1.0F, 1.0F);

        data.currentlyPlayingNoise = false;

        return true;
    }
    
    @Inject(method = "tick", at = @At("TAIL"))
    public void tickServer(BooleanSupplier booleanSupplier, CallbackInfo ci) {
        
        ServerLevel overworld = (ServerLevel) (Object) this;
        if (overworld == null) {
            return;
        }

        if (this.doReload) {
            this.debug = CaveDweller.CONFIG.DEBUG();
            if (!this.hasLedgerBeenClearedUponReload) {
                this.playerTimerLedger.clear();
                this.spelunkers.clear();
            }
            this.hasLedgerBeenClearedUponReload = true;
            this.randomlySelectedBrightnessLevel = (this.random.nextInt(12, 15));
            this.resetScoreboard(overworld);
            this.doReload = false;
        }

        // Get cave dweller info
        Iterable<Entity> entities = overworld.getAllEntities();
        java.util.concurrent.atomic.AtomicBoolean dwellerExists = new java.util.concurrent.atomic.AtomicBoolean(false);
        for (Entity entity : entities) {
            if (entity instanceof CaveDwellerEntity caveDweller) {
                dwellerExists.set(true);
                if (caveDweller.spawnedStalking) {
                    this.isSpawnedStalking = true;
                }
                Vec3 rawPos = caveDweller.position();
                this.caveDwellerPos = new Vec3((int) rawPos.x, (int) rawPos.y, (int) rawPos.z);
                this.isDwellerCurrentlyAggro = caveDweller.isAggro;
                this.currentGoal = caveDweller.currentRoll;

                if (isDwellerCurrentlyAggro) {
                    this.dwellerAliveTimer = caveDweller.ticksUntilRemoveChase;
                } else {
                    this.dwellerAliveTimer = caveDweller.ticksUntilRemove;
                }

                this.notLookingAtDweller = !caveDweller.targetIsLookingAtMe;
                break;
            }
        }

        // Scan every 3 seconds for spelunkers, then put them on a list
        this.scanTicks++;

        if (this.scanTicks >= 60 || this.playerTimerLedger.isEmpty()) {
            this.scanTicks = 0;
            this.spelunkers.clear();
            overworld.players().forEach(this::listSpelunkers);

            for (ServerPlayer player : this.spelunkers) {
                java.util.UUID uuid = player.getUUID();
                if (!this.playerTimerLedger.containsKey(uuid)) {
                    this.playerTimerLedger.put(uuid, new PlayerData());
                }
            }
        }

        java.util.Iterator<java.util.Map.Entry<java.util.UUID, PlayerData>> iterator = this.playerTimerLedger.entrySet().iterator();

        // For every player in the world...
        while (iterator.hasNext()) {
            java.util.Map.Entry<java.util.UUID, PlayerData> entry = iterator.next();
            java.util.UUID playerUuid = entry.getKey();
            PlayerData data = entry.getValue();

            ServerPlayer targetPlayer = (ServerPlayer) overworld.getPlayerByUUID(playerUuid);
            boolean isCurrentlySpelunker = false;
            for (ServerPlayer player : this.spelunkers) {
                if (player.getUUID().equals(playerUuid)) {
                    isCurrentlySpelunker = true;
                    targetPlayer = player;
                    break;
                }
            }

            // Surface check (ensures players who are in full view of the sky have their grace timers and calm timers reset)
            BlockPos playerPos = targetPlayer.blockPosition();

            boolean isOnSurface = !CaveDweller.CONFIG.ALLOW_SURFACE_SPAWN() &&
                    playerPos.getY() >= overworld.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, playerPos.getX(), playerPos.getZ());
            boolean exceedsSpawnHeight = !CaveDweller.CONFIG.ALLOW_SURFACE_SPAWN() &&
                    targetPlayer.getY() > (double) CaveDweller.CONFIG.SPAWN_HEIGHT();

            if (isOnSurface || exceedsSpawnHeight) {
                data.resetCalmTimer();
                iterator.remove();
                continue;
            }

            int skyLightLevel = overworld.getBrightness(LightLayer.SKY, playerPos) - overworld.getSkyDarken();
            if (skyLightLevel > 0) {
                float sunAngle = overworld.getSunAngle(1.0F);
                float f1 = sunAngle < (float) Math.PI ? 0.0F : ((float) Math.PI * 2F);
                sunAngle += (f1 - sunAngle) * 0.2F;
                skyLightLevel = Math.round((float) skyLightLevel * Mth.cos(sunAngle));
            }
            skyLightLevel = Mth.clamp(skyLightLevel, 0, 15);

            if (data.gracePeriod >= 0 && skyLightLevel > CaveDweller.CONFIG.SKY_LIGHT_LEVEL()) {
                data.resetCalmTimer();
                iterator.remove();
            }

            // Torch burnout
            if (targetPlayer != null) {
                ServerLevel level = targetPlayer.serverLevel();

                if ((data.activePhase == 1 || data.activePhase == 2) && data.burnoutEventTimer <= 0) {
                    List<BlockPos> torchPositions = new ArrayList<>();

                    Block burntOutTorchBlock = ModBlocks.getBurntOutTorch();
                    Block burntOutWallTorchBlock = ModBlocks.getBurntOutWallTorch();

                    for (BlockPos targetPos : BlockPos.betweenClosed(
                            playerPos.offset(-30, -6, -30),
                            playerPos.offset(30, 6, 30))) {

                        BlockState state = level.getBlockState(targetPos);
                        Block block = state.getBlock();

                        if ((block instanceof TorchBlock || block instanceof WallTorchBlock)
                                && !(block instanceof RedstoneTorchBlock)
                                && !(block instanceof RedstoneWallTorchBlock)
                                && block != burntOutTorchBlock
                                && block != burntOutWallTorchBlock) {
                            torchPositions.add(targetPos.immutable());
                        }
                    }

                    if (!torchPositions.isEmpty()) {
                        // Sort torches by distance to the player (closest first)
                        torchPositions.sort(Comparator.comparingDouble(pos -> pos.distSqr(playerPos)));

                        // Determine chance to pick a near torch. Lower calmTimer increases this chance
                        float nearChance = 1.0F - ((float) Math.min(data.calmTimer, data.calmTimerMax) / data.calmTimerMax);

                        BlockPos chosenTorch;
                        boolean isNearSelection = level.random.nextFloat() < nearChance;
                        int halfSize = torchPositions.size() / 2;

                        if (isNearSelection || halfSize == 0) {
                            // Pick from the closer half of the list
                            int indexRange = halfSize > 0 ? halfSize : torchPositions.size();
                            chosenTorch = torchPositions.get(level.random.nextInt(indexRange));

                            // Only play the ambient wind sound if the chosen torch is within 6 blocks of the player
                            if (chosenTorch.distSqr(playerPos) <= 36.0D) {
                                level.playSound(null, playerPos, ModSounds.CAVEDWELLER_WIND, SoundSource.AMBIENT, 1.0F, 1.0F);
                            }
                        } else {
                            // Pick from the further half of the list
                            chosenTorch = torchPositions.get(halfSize + level.random.nextInt(torchPositions.size() - halfSize));

                            if (chosenTorch.distSqr(playerPos) <= 36.0D) {
                                level.playSound(null, playerPos, ModSounds.CAVEDWELLER_WIND, SoundSource.AMBIENT, 1.0F, 1.0F);
                            }
                        }

                        BlockState originalState = level.getBlockState(chosenTorch);
                        BlockState initialBurntState;
                        if (originalState.getBlock() instanceof WallTorchBlock) {
                            initialBurntState = burntOutWallTorchBlock.defaultBlockState()
                                    .setValue(WallTorchBlock.FACING, originalState.getValue(WallTorchBlock.FACING));
                        } else {
                            initialBurntState = burntOutTorchBlock.defaultBlockState();
                        }

                        initialBurntState = initialBurntState.setValue(com.thecaptain.cavedweller.block.BurntOutTorchBlock.LIGHT, 14);
                        level.setBlock(chosenTorch, initialBurntState, 3);

                        level.sendParticles(
                                net.minecraft.core.particles.ParticleTypes.SMOKE,
                                chosenTorch.getX() + 0.5D,
                                chosenTorch.getY() + 0.7D,
                                chosenTorch.getZ() + 0.5D,
                                15,
                                0.05D,
                                0.1D,
                                0.05D,
                                0.02D
                        );

                        level.playSound(null, chosenTorch, SoundEvents.REDSTONE_TORCH_BURNOUT, SoundSource.BLOCKS, 1.0F, 1.0F);
                    }
                }
            }

            // Timers
            int pauseThreshold = Utils.secondsToTicks(30);
            boolean isCalmTimerAtPauseThreshold = (data.calmTimer > 0 && data.calmTimer <= pauseThreshold);

            if (dwellerExists.get()) {
                data.activePhase = 3;
            }

            if (data.gracePeriod < data.gracePeriodMax) {
                if (isCalmTimerAtPauseThreshold) {
                    this.shouldTickTimers = false;
                } else {
                    if (data.activePhase <= 2) {
                        this.shouldTickTimers = (overworld.getGameTime() % 2 == 0);
                    } else {
                        this.shouldTickTimers = true;
                    }
                }
            } else {
                this.shouldTickTimers = true;
            }

            if (this.shouldTickTimers) {
                if (!dwellerExists.get()) {
                    if (data.calmTimer <= 0) {
                        data.resetCalmTimer();
                    } else {
                        --data.calmTimer;
                    }

                    if (data.burnoutEventTimer <= 0) {
                        data.resetBurnoutEventTimer();
                    } else if (data.burnoutEventTimer > 0 && (data.activePhase == 1 || data.activePhase == 2) && data.calmTimer > data.burnoutEventTimer) {
                        --data.burnoutEventTimer;
                    }

                    --data.dwellerCaveNoiseTimer;
                    --data.vanillaCaveNoiseTimer;
                }
            }

            if (!dwellerExists.get() && data.calmTimer == -1 || data.gracePeriod <= 0) {
                data.resetCalmTimer();
                iterator.remove();
            }

            // Phases
            data.activePhase = 0;

            if (!dwellerExists.get() && data.calmTimer > 0) {
                if (data.calmTimer <= data.dwellerStartGate) {
                    data.activePhase = 2;
                } else if (data.calmTimer <= data.vanillaStartGate) {
                    data.activePhase = 1;
                }
            // Enter phase 3 once dweller needs to start spawning in, not just if the dweller exists
            } else if (data.calmTimer <= 0) {
                data.activePhase = 3;
            }

            switch (data.activePhase) {
                case 1 -> {
                    data.currentActivePhaseName = "§a" + "§l" + "Vanilla phase (Phase 1)";
                    data.ticksUntilNextPhase = data.calmTimer - data.dwellerStartGate;
                    data.dwellerCaveNoiseTimer = -1;
                    data.caveNoiseTimerCheck();
                    if (data.vanillaCaveNoiseTimer <= 0) {
                        if (targetPlayer != null && !data.currentlyPlayingNoise) {
                            data.currentlyPlayingNoise = true;
                            this.playVanillaCaveNoiseToSpelunkers(targetPlayer, data);
                        }
                        data.resetVanillaCaveNoiseTimer();
                    }
                }
                case 2 -> {
                    data.currentActivePhaseName = "§b" + "§l" + "Dweller phase (Phase 2)";
                    data.ticksUntilNextPhase = data.calmTimer;
                    data.vanillaCaveNoiseTimer = -1;
                    data.caveNoiseTimerCheck();
                    if (data.dwellerCaveNoiseTimer <= 0) {
                        if (targetPlayer != null && !data.currentlyPlayingNoise) {
                            data.currentlyPlayingNoise = true;
                            this.playDwellerCaveNoiseToSpelunkers(targetPlayer, data);
                        }
                        data.resetDwellerCaveNoiseTimer();
                    }
                }
                case 3 -> {
                    data.currentActivePhaseName = "§4" + "§l" + "Spawn phase (Phase 3)";
                    data.ticksUntilNextPhase = 0;
                    data.vanillaCaveNoiseTimer = -1;
                    data.caveNoiseTimerCheck();
                    if (this.isSpawnedStalking) {
                        data.dwellerCaveNoiseTimer = -1;
                        if (data.stalkNoiseTimer <= 0 && !this.isDwellerCurrentlyAggro && this.notLookingAtDweller) {
                            if (targetPlayer != null && !data.currentlyPlayingNoise) {
                                data.currentlyPlayingNoise = true;
                                this.playStalkNoiseToSpelunkers(targetPlayer, data);
                            }
                            data.resetStalkNoiseTimer();
                            data.currentlyPlayingNoise = false;
                        }
                    } else {
                        data.stalkNoiseTimer = -1;
                        if (data.dwellerCaveNoiseTimer <= 0) {
                            if (targetPlayer != null && !data.currentlyPlayingNoise) {
                                data.currentlyPlayingNoise = true;
                                this.playDwellerCaveNoiseToSpelunkers(targetPlayer, data);
                            }
                            data.resetDwellerCaveNoiseTimer();
                        }
                    }
                }
                default -> {
                    data.currentActivePhaseName = "§l" + "Quiet (Phase 0)";
                    data.ticksUntilNextPhase = data.calmTimer - data.vanillaStartGate;
                    data.vanillaCaveNoiseTimer = -1;
                    data.dwellerCaveNoiseTimer = -1;
                    data.currentlyPlayingNoise = false;
                }
            }

            // If dweller doesn't exist...
            if (!dwellerExists.get()) {
                // and if the player is a spelunker...
                if (isCurrentlySpelunker) {
                    // Set max grace period based on current phase
                    int gracePeriodMaxIncreaseBy = Utils.secondsToTicks(10);
                    int gracePeriodTimerMax = Utils.secondsToTicks(5);

                    if (data.activePhase <= 1) {
                        data.gracePeriodMax = Utils.secondsToTicks(CaveDweller.CONFIG.GRACE_PERIOD_BEFORE_RESET());
                        data.gracePeriod = data.gracePeriodMax;
                        data.gracePeriodTimer = gracePeriodTimerMax;
                    } else if (data.activePhase == 2) {
                        if (data.gracePeriodTimer > 0) {
                            --data.gracePeriodTimer;
                        }

                        if (data.gracePeriodTimer <= 0) {
                            data.gracePeriodMax += gracePeriodMaxIncreaseBy;
                            data.gracePeriodTimer = gracePeriodTimerMax;
                        }

                        data.gracePeriod = data.gracePeriodMax;
                    } else {
                        if (data.gracePeriodTimer > 0) {
                            --data.gracePeriodTimer;
                        }

                        if (data.gracePeriodTimer <= 0) {
                            data.gracePeriodMax += gracePeriodMaxIncreaseBy;
                            data.gracePeriodTimer = gracePeriodTimerMax;
                        }
                    }

                    // Define when phase 1 and 2 start
                    float phase1StartPercent = 75;
                    float phase2StartPercent = 40;
                    data.phase1StartPercentDecimal = phase1StartPercent / 100;
                    data.phase2StartPercentDecimal = phase2StartPercent / 100;

                    data.vanillaStartGate = Math.round(data.calmTimerMax * (float) data.phase1StartPercentDecimal);
                    data.dwellerStartGate = Math.round(data.calmTimerMax * (float) data.phase2StartPercentDecimal);

                    // When calmTimer runs out, attempt to spawn Dweller
                    if (data.calmTimer <= 0) {
                        if (targetPlayer != null) {
                            if (!data.currentlyPlayingNoise) {
                                data.currentlyPlayingNoise = true;
                                if (this.isSpawnedStalking) {
                                    this.playStalkNoiseToSpelunkers(targetPlayer, data);
                                } else {
                                    this.playDwellerCaveNoiseToSpelunkers(targetPlayer, data);
                                }
                            }

                            CaveDwellerEntity caveDweller = new CaveDwellerEntity(ModEntityTypes.CAVEDWELLER, overworld);
                            Vec3 spawnPos = caveDweller.generatePos(targetPlayer);

                            if (spawnPos != null) {
                                caveDweller.moveTo(spawnPos.x, spawnPos.y, spawnPos.z, 0.0F, 0.0F);
                                caveDweller.setInvisible(true);
                                overworld.addFreshEntity(caveDweller);
                                caveDweller.lookAt(EntityAnchorArgument.Anchor.EYES, targetPlayer.getEyePosition(1.0F));
                                caveDweller.refreshDimensions();
                            } else {
                                data.resetCalmTimer();
                            }

                            iterator.remove();

                            break;
                        }
                    }
                // If dweller doesn't exist and player is not a spelunker...
                } else {
                    if (data.activePhase <= 2) {
                        data.gracePeriod--;
                    }
                }
            }

            if (this.isSpawnedStalking){
                if (dwellerExists.get() && data.calmTimer == -1) {
                    --data.stalkNoiseTimer;
                }
            }
            if (dwellerExists.get() && data.calmTimer <= 0) {
                data.calmTimer = -1;
            }

            // For debug scoreboard
            this.phase1StartPercent = data.phase1StartPercentDecimal * 100;
            this.phase2StartPercent = data.phase2StartPercentDecimal * 100;
            this.phase1StartMinsAndSecs = Utils.ticksToMinutesAndSeconds(data.vanillaStartGate);
            this.phase2StartMinsAndSecs = Utils.ticksToMinutesAndSeconds(data.dwellerStartGate);

            this.calmTimerMinsAndSecs = Utils.ticksToMinutesAndSeconds(data.calmTimer);

            this.vanillaNoiseTimerMinsAndSecs = Utils.ticksToMinutesAndSeconds(data.vanillaCaveNoiseTimer);
            this.dwellerNoiseTimerMinsAndSecs = Utils.ticksToMinutesAndSeconds(data.dwellerCaveNoiseTimer);
            this.stalkNoiseTimerMinsAndSecs = Utils.ticksToMinutesAndSeconds(data.stalkNoiseTimer);

            this.vanillaNoiseTimerSecs = data.vanillaCaveNoiseTimer < 0 ? -1 : data.vanillaCaveNoiseTimer / 20;
            this.dwellerNoiseTimerSecs = data.dwellerCaveNoiseTimer < 0 ? -1 : data.dwellerCaveNoiseTimer / 20;
            this.stalkNoiseTimerSecs = Math.max(0, data.stalkNoiseTimer / 20);

            this.dwellerExistsFlag = dwellerExists.get();

            this.updateScoreboardDisplay(overworld, data);
        }
    }

    /// Define a spelunker
    // Player is NOT a spelunker if...
    // -They are dead/null
    // -They are invisible (according to config)
    // -They are either in Creative or Spectator mode
    // AND
    // -They are above the config's dweller spawn height
    // -Their skylight level is above the config's max skylight
    // -Their block light level is above the config's max block light
    // Lastly, if all that returns false, return if surface spawning is allowed OR if the player can't see the sky
    public boolean isPlayerSpelunker(ServerPlayer player) {
        if (!Utils.isValidPlayer(player)) {
            return false;
        }

        if (!CaveDweller.CONFIG.ALLOW_SURFACE_SPAWN() && player.getY() > (double) CaveDweller.CONFIG.SPAWN_HEIGHT()) {
            return false;
        }

        Level serverLevel = player.level();
        BlockPos playerPos = player.blockPosition();

        int skyLightLevel = serverLevel.getBrightness(LightLayer.SKY, playerPos) - serverLevel.getSkyDarken();
        if (skyLightLevel > 0) {
            float sunAngle = serverLevel.getSunAngle(1.0F);
            float f1 = sunAngle < (float) Math.PI ? 0.0F : ((float) Math.PI * 2F);
            sunAngle += (f1 - sunAngle) * 0.2F;
            skyLightLevel = Math.round((float) skyLightLevel * Mth.cos(sunAngle));
        }
        skyLightLevel = Mth.clamp(skyLightLevel, 0, 15);

        if (skyLightLevel > CaveDweller.CONFIG.SKY_LIGHT_LEVEL()) {
            return false;
        }

        int blockLightLevel = serverLevel.getBrightness(LightLayer.BLOCK, playerPos);
        if (blockLightLevel >= this.randomlySelectedBrightnessLevel) {
            return false;
        }

        if (CaveDweller.CONFIG.ALLOW_SURFACE_SPAWN()) {
            return true;
        }

        return CaveDweller.CONFIG.ALLOW_SURFACE_SPAWN() || !serverLevel.canSeeSky(playerPos);
    }

    public void listSpelunkers(net.minecraft.server.level.ServerPlayer player) {
        if (this.isPlayerSpelunker(player)) {
            this.spelunkers.add(player);
        }
    }

    /// DEBUG
    // Scoreboard
    private void resetScoreboard(ServerLevel level) {
        net.minecraft.world.scores.Scoreboard scoreboard = level.getScoreboard();
        // Had to delete the one with the section sign (§) because apparently Minecraft doesn't like that symbol being in the chat
        net.minecraft.world.scores.Objective objectiveOld = scoreboard.getObjective("§dweller_debug");
        net.minecraft.world.scores.Objective objective = scoreboard.getObjective("dweller_debug");

        if (this.debug) {
            if (objectiveOld != null) {
                scoreboard.removeObjective(objectiveOld);
            }
            if (objective != null) {
                scoreboard.removeObjective(objective);
            }

            if (!scoreboard.hasObjective("dweller_debug")) {
                scoreboard.addObjective(
                        "dweller_debug",
                        net.minecraft.world.scores.criteria.ObjectiveCriteria.DUMMY,
                        net.minecraft.network.chat.Component.literal("§e§l[DEBUG]§r MixinServerWorld Info"),
                        net.minecraft.world.scores.criteria.ObjectiveCriteria.RenderType.INTEGER
                );
            }

            objective = scoreboard.getObjective("dweller_debug");
            scoreboard.setDisplayObjective(net.minecraft.world.scores.Scoreboard.DISPLAY_SLOT_SIDEBAR, objective);
        } else {
            if (scoreboard.hasObjective("dweller_debug") || scoreboard.hasObjective("§dweller_debug")) {
                scoreboard.removeObjective(objective);
            }
        }
    }

    public String[] getScoreboardText(PlayerData data) {
        String gracePeriodSB;

        if (data.gracePeriod < Utils.secondsToTicks(10)) {
            gracePeriodSB = "§2Grace period: §4" + Utils.ticksToMinutesAndSeconds(data.gracePeriod);
        } else {
            gracePeriodSB = "§2Grace period: §6" + Utils.ticksToMinutesAndSeconds(data.gracePeriod);
        }

        String calmTimerMaxPossibleSecondsSB = "§fCalm Timer Max Possible: §6" + Utils.ticksToSeconds(data.calmTimerMax) + "s§f (§6" + data.calmTimerMax + "t§f)";

        int localCalmTimerSecs = data.calmTimer < 0 ? -1 : data.calmTimer / 20;
        String calmTimerTicksSB = "§fCalm Timer (ticks): §6" + (localCalmTimerSecs == -1? this.timerInactive : data.calmTimer + "t§r");
        String calmTimerMinutesSecondsSB = "§fCalm Timer (minutes): §6" + (localCalmTimerSecs == -1? this.timerInactive : Utils.ticksToMinutesAndSeconds(data.calmTimer));

        String currentActivePhaseSB = "§fActive phase: " + data.currentActivePhaseName;

        String phase1StartSB = "§aPhase 1 §fstarts at §6" + this.phase1StartPercent + "%§f, or at §6" + this.phase1StartMinsAndSecs;

        int localVanillaTimerSecs = data.vanillaCaveNoiseTimer < 0 ? -1 : data.vanillaCaveNoiseTimer / 20;
        String vanillaTimerMinutesSecondsSB = "§aVanilla Cave Noise Timer (minutes): §6" + (localVanillaTimerSecs == -1 ? this.timerInactive : Utils.ticksToMinutesAndSeconds(data.vanillaCaveNoiseTimer));

        String phase2StartSB = "§bPhase 2 §fstarts at §6" + this.phase2StartPercent + "%§f, or at §6" + this.phase2StartMinsAndSecs;

        int localDwellerTimerSecs = data.dwellerCaveNoiseTimer < 0 ? -1 : data.dwellerCaveNoiseTimer / 20;
        String dwellerTimerMinutesSecondsSB = "§bDweller Cave Noise Timer (minutes): §6" + (localDwellerTimerSecs == -1 ? this.timerInactive : Utils.ticksToMinutesAndSeconds(data.dwellerCaveNoiseTimer));

        String torchBurnoutEventSB = "§dTorch Burnout Event Timer (minutes): §6" + Utils.ticksToMinutesAndSeconds(data.burnoutEventTimer);

        String dwellerExistsSB = "§6Dweller exists? " + (this.dwellerExistsFlag ? "§4YES, at: §r" + this.caveDwellerPos : "§cNO§r");

        String lifetimeText = Utils.ticksToMinutesAndSeconds(this.dwellerAliveTimer);
        String dwellerLifetimeSB;
        if (this.dwellerAliveTimer <= 0) {
            dwellerLifetimeSB = "§4Dweller disappearing once out of sight...";
        } else {
            dwellerLifetimeSB = "§4Dweller currently exists for §6" + lifetimeText + "§4 more";
        }

        int localStalkSecs = data.stalkNoiseTimer < 0 ? -1 : data.stalkNoiseTimer / 20;
        String stalkNoiseTimerMinutesSecondsSB = "§4Stalk Noise Timer (minutes): §6" + (localStalkSecs == -1 ? this.timerInactive : Utils.ticksToMinutesAndSeconds(data.stalkNoiseTimer));

        String goalText = this.currentGoal != null ? this.currentGoal.name() : "NONE";
        String dwellerCurrentGoalSB = "§4Current goal: §6" + goalText;

        if (this.dwellerExistsFlag && this.isSpawnedStalking) {
            return new String[]{
                    gracePeriodSB,
                    calmTimerMaxPossibleSecondsSB,
                    calmTimerTicksSB,
                    calmTimerMinutesSecondsSB,
                    currentActivePhaseSB,
                    phase1StartSB,
                    vanillaTimerMinutesSecondsSB,
                    phase2StartSB,
                    dwellerTimerMinutesSecondsSB,
                    torchBurnoutEventSB,
                    dwellerExistsSB,
                    dwellerLifetimeSB,
                    dwellerCurrentGoalSB,
                    stalkNoiseTimerMinutesSecondsSB
            };
        } else if (this.dwellerExistsFlag) {
            return new String[]{
                    gracePeriodSB,
                    calmTimerMaxPossibleSecondsSB,
                    calmTimerTicksSB,
                    calmTimerMinutesSecondsSB,
                    currentActivePhaseSB,
                    phase1StartSB,
                    vanillaTimerMinutesSecondsSB,
                    phase2StartSB,
                    dwellerTimerMinutesSecondsSB,
                    torchBurnoutEventSB,
                    dwellerExistsSB,
                    dwellerLifetimeSB,
                    dwellerCurrentGoalSB,
                    "§6(Did not spawn stalking)"
            };
        } else {
        return new String[]{
                gracePeriodSB,
                calmTimerMaxPossibleSecondsSB,
                calmTimerTicksSB,
                calmTimerMinutesSecondsSB,
                currentActivePhaseSB,
                phase1StartSB,
                vanillaTimerMinutesSecondsSB,
                phase2StartSB,
                dwellerTimerMinutesSecondsSB,
                torchBurnoutEventSB,
                dwellerExistsSB
        };
        }
    }

    private void updateScoreboardDisplay(ServerLevel level, PlayerData data) {
        if (this.debug) {
            net.minecraft.world.scores.Scoreboard scoreboard = level.getScoreboard();
            net.minecraft.world.scores.Objective objective = scoreboard.getObjective("dweller_debug");

            if (objective == null) {
                return;
            }

            if (scoreboard.getDisplayObjective(net.minecraft.world.scores.Scoreboard.DISPLAY_SLOT_SIDEBAR) != objective) {
                scoreboard.setDisplayObjective(net.minecraft.world.scores.Scoreboard.DISPLAY_SLOT_SIDEBAR, objective);
            }

            String[] diagnosticLines = this.getScoreboardText(data);

            if (diagnosticLines == null || diagnosticLines.length == 0) {
                scoreboard.getOrCreatePlayerScore("No diagnostics available.", objective).setScore(0);
                return;
            }

            for (String scoreHolder : new java.util.ArrayList<>(scoreboard.getTrackedPlayers())) {
                if (scoreboard.hasPlayerScore(scoreHolder, objective)) {
                    scoreboard.resetPlayerScore(scoreHolder, objective);
                }
            }

            for (int i = 0; i < diagnosticLines.length; i++) {
                String lineText = diagnosticLines[i];
                int positionScore = diagnosticLines.length - 1 - i;

                scoreboard.getOrCreatePlayerScore(lineText, objective).setScore(positionScore);
            }
        }
    }
}
