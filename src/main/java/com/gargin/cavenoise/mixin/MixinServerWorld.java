package com.gargin.cavenoise.mixin;

import com.gargin.cavenoise.CaveNoise;
import com.gargin.cavenoise.entities.CaveDwellerEntity;
import com.gargin.cavenoise.registry.ModEntityTypes;
import com.gargin.cavenoise.registry.ModSounds;
import com.gargin.cavenoise.util.Utils;

import java.util.ArrayList;
import java.util.Random;
import java.util.function.BooleanSupplier;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerLevel.class)
public abstract class MixinServerWorld {
    private static boolean doReload = true;
    private final java.util.List<net.minecraft.server.level.ServerPlayer> spelunkers = new java.util.ArrayList<>();
    java.util.Map<java.util.UUID, PlayerTimerData> playerTimerLedger = new java.util.HashMap<>();
    private final Random random = new Random();
    private boolean dwellerExistsFlag;
    private int spawnTickCounter = 0;
    private boolean debug = CaveNoise.CONFIG.DEBUG();
    private int scanTicks = 0;
    private int randomlySelectedBrightnessLevel;
    // Scoreboard
    private String calmTimerMinsAndSecs;
    private String vanillaNoiseTimerMinsAndSecs;
    private String dwellerNoiseTimerMinsAndSecs;
    private String stalkNoiseTimerMinsAndSecs;
    private String currentActivePhaseName;
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

    public static class PlayerTimerData {
        private final net.minecraft.util.RandomSource random = net.minecraft.util.RandomSource.create();
        public int calmTimer;
        public int gracePeriod;
        public int vanillaCaveNoiseTimer;
        public int dwellerCaveNoiseTimer;
        public int stalkNoiseTimer;
        public int spawnAttemptTimer;
        public boolean currentlyPlayingNoise = false;
        public int cooldown = (Utils.secondsToTicks(CaveNoise.CONFIG.RESET_CALM_MAX()) * 2);
        public int calmTimerMax;
        public float phase1StartPercentDecimal;
        public float phase2StartPercentDecimal;
        public int activePhase;
        public int ticksUntilNextPhase;
        public int vanillaStartGate;
        public int dwellerStartGate;

        public PlayerTimerData() {
            this.resetCalmTimer();
            this.resetDwellerCaveNoiseTimer();
            this.resetVanillaCaveNoiseTimer();
            this.resetStalkNoiseTimer();
            this.resetSpawnAttemptTimer();
            this.gracePeriod = Utils.secondsToTicks(CaveNoise.CONFIG.GRACE_PERIOD_BEFORE_RESET());
        }

        /// Reset timers
        public void resetAll(ServerLevel overworld, boolean dwellerExistsFlag) {
            this.resetCalmTimer();
            this.caveNoiseTimerCheck();

            if (dwellerExistsFlag) {
                this.resetStalkNoiseTimer();
            } else {
                this.resetSpawnAttemptTimer();
            }
        }

        public void resetCalmTimer() {
            int min = Utils.secondsToTicks(CaveNoise.CONFIG.RESET_CALM_MIN());
            int max = Utils.secondsToTicks(CaveNoise.CONFIG.RESET_CALM_MAX());

            if (max < min) {
                int correctedMax = min;
                min = max;
                max = correctedMax;
            } else if (min == max) {
                max += 30;
            }

            if (this.random.nextDouble() <= CaveNoise.CONFIG.RESET_CALM_COOLDOWN_CHANCE()) {
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

            //
            int startMinTicks = Utils.secondsToTicks(CaveNoise.CONFIG.RESET_VANILLA_CAVE_NOISE_START_MIN());

            // The frantic end interval is calculated as 10% of the starting minimum
            int endMinTicks = Math.max(100, startMinTicks / 10);
            int min = Math.round((float) (endMinTicks - startMinTicks) * weight + (float) startMinTicks);

            // The starting maximum interval scales out to 2.5x the starting minimum
            int startMaxTicks = startMinTicks * 5 / 2;
            // The frantic end maximum collapses down tightly to double the end minimum floor
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

            int min = Utils.secondsToTicks(CaveNoise.CONFIG.RESET_DWELLER_CAVE_NOISE_MIN());
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

        // Ensures at least one cave sound plays during its phase, provided the next phase is not happening in (MinimumSpaceBetweenNoises) ticks/seconds or less
        public void caveNoiseTimerCheck() {
            int MinimumSpaceBetweenVanillaNoises = Utils.secondsToTicks(CaveNoise.CONFIG.RESET_VANILLA_CAVE_NOISE_START_MIN());
            int MinimumSpaceBetweenDwellerNoises = Utils.secondsToTicks(CaveNoise.CONFIG.RESET_DWELLER_CAVE_NOISE_MIN());
            int MinimumSpaceBetweenStalkNoises = Utils.secondsToTicks(CaveNoise.CONFIG.RESET_STALK_NOISE_MIN());

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
            int min = CaveNoise.CONFIG.RESET_STALK_NOISE_MIN();
            int max = min * 2;

            if (max < min) {
                int temp = min;
                min = max;
                max = temp;
            }
            this.stalkNoiseTimer = this.random.nextInt(Utils.secondsToTicks(min), Utils.secondsToTicks(max + 1));
        }

        public void resetSpawnAttemptTimer() {
            int min = 10;
            int max = 15;

            if (max < min) {
                int temp = min;
                min = max;
                max = temp;
            }
            this.spawnAttemptTimer = this.random.nextInt(Utils.secondsToTicks(min), Utils.secondsToTicks(max + 1));
        }
    }

    /// Timers
    // Phase 1: Vanilla Noises
    public boolean playVanillaCaveNoiseToSpelunkers(ServerPlayer player, PlayerTimerData data) {

        double targetX = player.getX() + (double) (-6 + this.random.nextInt(13));
        double targetY = player.getY();
        double targetZ = player.getZ() + (double) (-6 + this.random.nextInt(13));

        ServerLevel serverLevel = player.serverLevel();
        serverLevel.playSound(null, targetX, targetY, targetZ, SoundEvents.AMBIENT_CAVE.value(), SoundSource.AMBIENT, 1.0F, 1.0F);

        data.currentlyPlayingNoise = false;

        return true;
    }

    // Phase 2: Dweller Noises
    public boolean playDwellerCaveNoiseToSpelunkers(ServerPlayer player, PlayerTimerData data) {

        int currentDwellerStart = Math.round(data.calmTimerMax * (float) data.phase2StartPercentDecimal);
        int currentDwellerEnd = Math.round(data.calmTimerMax * 0.04F);

        int dwellerDenominator = Math.max(1, currentDwellerStart - currentDwellerEnd);
        float alpha = (float) (data.calmTimer - currentDwellerEnd) / dwellerDenominator;
        float weight = Mth.clamp(1.0F - alpha, 0.1F, 1.0F);

        float minVol = 0.1F;
        float maxVol = 1.0F;
        float dynamicVolume = minVol + (maxVol - minVol) * weight;

        SoundEvent selectedSound = switch (this.random.nextInt(9)) {
            case 0 -> ModSounds.CAVENOISE_1;
            case 1 -> ModSounds.CAVENOISE_2;
            case 2 -> ModSounds.CAVENOISE_3;
            case 3 -> ModSounds.CAVENOISE_4;
            case 4 -> ModSounds.CAVENOISE_5;
            case 5 -> ModSounds.CAVENOISE_6;
            case 6 -> ModSounds.CAVENOISE_7;
            case 7 -> ModSounds.CAVENOISE_8;
            default -> ModSounds.CAVENOISE_9;
        };

        double targetX = player.getX() + (double) (-6 + this.random.nextInt(13));
        double targetY = player.getY();
        double targetZ = player.getZ() + (double) (-6 + this.random.nextInt(13));

        ServerLevel serverLevel = player.serverLevel();
        serverLevel.playSound(null, targetX, targetY, targetZ, selectedSound, SoundSource.AMBIENT, dynamicVolume, 1.0F);
        data.currentlyPlayingNoise = false;

        return true;
    }

    // Phase 3: Stalk / Failed spawn attempt Noises
    public boolean playStalkNoiseToSpelunkers(ServerPlayer player, PlayerTimerData data) {

        SoundEvent selectedSound = switch (this.random.nextInt(5)) {
            case 0 -> ModSounds.STALK_1;
            case 1 -> ModSounds.STALK_2;
            case 3 -> ModSounds.STALK_4;
            case 2 -> ModSounds.STALK_3;
            default -> ModSounds.STALK_5;
        };

        double targetX = player.getX() + (double) (-6 + this.random.nextInt(13));
        double targetY = player.getY();
        double targetZ = player.getZ() + (double) (-6 + this.random.nextInt(13));

        ServerLevel serverLevel = player.serverLevel();
        serverLevel.playSound(null, targetX, targetY, targetZ, selectedSound, SoundSource.AMBIENT, 1.0F, 1.0F);

        data.resetStalkNoiseTimer();
        data.currentlyPlayingNoise = false;

        return true;
    }

    @Inject(method = "tick", at = @At("TAIL"))
    public void tickServer(BooleanSupplier booleanSupplier, CallbackInfo ci) {
        // FIXED: Convert 'this' directly into your Level instance context
        ServerLevel overworld = (ServerLevel) (Object) this;
        if (overworld == null) {
            return;
        }

        if (doReload) {
            this.debug = CaveNoise.CONFIG.DEBUG();
            this.playerTimerLedger.clear();
            this.spelunkers.clear();
            this.randomlySelectedBrightnessLevel = (this.random.nextInt(6, 10));
            this.resetScoreboard(overworld);
            doReload = false;
        }

        this.dwellerExistsFlag = false;

        Iterable<Entity> entities = overworld.getAllEntities();
        java.util.concurrent.atomic.AtomicBoolean dwellerExists = new java.util.concurrent.atomic.AtomicBoolean(false);
        for (Entity entity : entities) {
            if (entity instanceof CaveDwellerEntity) {
                dwellerExists.set(true);
                break;
            }
        }

        // Step 1: Scan every 3 seconds for spelunkers, then puts them on a list
        this.scanTicks++;
        if (this.scanTicks >= 60) {
            this.scanTicks = 0;
            this.spelunkers.clear();
            overworld.players().forEach(this::listSpelunkers);

            for (ServerPlayer player : this.spelunkers) {
                java.util.UUID uuid = player.getUUID();
                if (!this.playerTimerLedger.containsKey(uuid)) {
                    this.playerTimerLedger.put(uuid, new PlayerTimerData());
                }
            }
        }

        // Step 2: Tick down grace period for players who stop being spelunkers
        java.util.Iterator<java.util.Map.Entry<java.util.UUID, PlayerTimerData>> iterator = this.playerTimerLedger.entrySet().iterator();

        while (iterator.hasNext()) {
            java.util.Map.Entry<java.util.UUID, PlayerTimerData> entry = iterator.next();
            java.util.UUID playerUuid = entry.getKey();
            PlayerTimerData data = entry.getValue();

            // Verify if this specific tracked person is actively a spelunker right now
            boolean isCurrentlySpelunker = false;
            for (ServerPlayer p : this.spelunkers) {
                if (p.getUUID().equals(playerUuid)) {
                    isCurrentlySpelunker = true;
                    break;
                }
            }

            if (isCurrentlySpelunker) {
                // Refresh their personal grace pool allotment window
                data.gracePeriod = Utils.secondsToTicks(CaveNoise.CONFIG.GRACE_PERIOD_BEFORE_RESET());

                // If no dweller is active globally, tick down their packet numbers
                if (!dwellerExists.get()) {
                    --data.calmTimer;
                    --data.dwellerCaveNoiseTimer;
                    --data.vanillaCaveNoiseTimer;
                    --data.stalkNoiseTimer;
                }

                if (!dwellerExists.get() && data.calmTimer > 0) {
                    data.resetSpawnAttemptTimer();
                }

                float phase1StartPercent = 75;
                float phase2StartPercent = 40;
                data.phase1StartPercentDecimal = phase1StartPercent / 100;
                data.phase2StartPercentDecimal = phase2StartPercent / 100;

                data.vanillaStartGate = Math.round(data.calmTimerMax * (float) data.phase1StartPercentDecimal);
                data.dwellerStartGate = Math.round(data.calmTimerMax * (float) data.phase2StartPercentDecimal);

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
                    case 0 -> {
                        this.currentActivePhaseName = "§l" + "Quiet (Phase 0)";
                        data.ticksUntilNextPhase = data.calmTimer - data.vanillaStartGate;
                    }
                    case 1 -> {
                        this.currentActivePhaseName = "§a" + "§l" + "Vanilla phase (Phase 1)";
                        data.ticksUntilNextPhase = data.calmTimer - data.dwellerStartGate;
                    }
                    case 2 -> {
                        this.currentActivePhaseName = "§b" + "§l" + "Dweller phase (Phase 2)";
                        data.ticksUntilNextPhase = data.calmTimer;
                    }
                    case 3 -> {
                        this.currentActivePhaseName = "§4" + "§l" + "Stalk phase (Phase 3)";
                        data.ticksUntilNextPhase = 0;
                    }
                    default -> {
                        this.currentActivePhaseName = "§d" + "§l" + "Invalid";
                        data.ticksUntilNextPhase = data.calmTimer;
                    }
                }

                if (!dwellerExists.get() && data.calmTimer > 0 && data.activePhase < 3) {
                    // Check if the tracker was previously exhausted before updating
                    if (data.spawnAttemptTimer <= 20) {
                        data.resetSpawnAttemptTimer();
                    }
                }

                boolean canSpawn = data.calmTimer <= 0 && data.spawnAttemptTimer > 0;
                if (data.calmTimer < 0) {
                    data.calmTimer = 0;
                }

                switch (data.activePhase) {
                    case 1 -> {
                        data.dwellerCaveNoiseTimer = -1;
                        data.caveNoiseTimerCheck();
                        if (data.vanillaCaveNoiseTimer <= 0) {
                            ServerPlayer targetPlayer = (ServerPlayer) overworld.getPlayerByUUID(playerUuid);
                            if (targetPlayer != null && !data.currentlyPlayingNoise) {
                                data.currentlyPlayingNoise = true;
                                this.playVanillaCaveNoiseToSpelunkers(targetPlayer, data);
                            }
                            data.resetVanillaCaveNoiseTimer();
                        }
                    }
                    case 2 -> {
                        data.vanillaCaveNoiseTimer = -1;
                        data.caveNoiseTimerCheck();
                        if (data.dwellerCaveNoiseTimer <= 0) {
                            ServerPlayer targetPlayer = (ServerPlayer) overworld.getPlayerByUUID(playerUuid);
                            if (targetPlayer != null && !data.currentlyPlayingNoise) {
                                data.currentlyPlayingNoise = true;
                                this.playDwellerCaveNoiseToSpelunkers(targetPlayer, data);
                            }
                            data.resetDwellerCaveNoiseTimer();
                        }
                    }
                    case 3 -> {
                        data.vanillaCaveNoiseTimer = -1;
                        data.dwellerCaveNoiseTimer = -1;
                        data.caveNoiseTimerCheck();
                        if (data.stalkNoiseTimer <= 0 && !this.isDwellerCurrentlyAggro) {
                            ServerPlayer targetPlayer = (ServerPlayer) overworld.getPlayerByUUID(playerUuid);
                            if (targetPlayer != null && !data.currentlyPlayingNoise) {
                                data.currentlyPlayingNoise = true;
                                this.playStalkNoiseToSpelunkers(targetPlayer, data);
                            }
                            data.resetStalkNoiseTimer();
                        }
                        data.currentlyPlayingNoise = false;
                    }
                    default -> {
                        data.vanillaCaveNoiseTimer = -1;
                        data.dwellerCaveNoiseTimer = -1;
                        data.currentlyPlayingNoise = false;
                    }
                }

                if (data.calmTimer <= 0 && !dwellerExists.get()) {
                    --data.spawnAttemptTimer;

                    if (data.spawnAttemptTimer <= 0) {
                        ServerPlayer targetPlayer = (ServerPlayer) overworld.getPlayerByUUID(playerUuid);
                        if (targetPlayer != null && !data.currentlyPlayingNoise) {
                            data.currentlyPlayingNoise = true;
                            this.playStalkNoiseToSpelunkers(targetPlayer, data);
                            CaveDwellerEntity caveDweller = new CaveDwellerEntity(ModEntityTypes.CAVEDWELLER, overworld);
                            Vec3 spawnPos = caveDweller.generatePos(targetPlayer);
                            if (spawnPos != null) {
                                caveDweller.moveTo(caveDweller.generatePos(targetPlayer));
                                boolean success = overworld.addFreshEntity(caveDweller);
                                caveDweller.setInvisible(true);
                                System.out.println("SPAWNED DWELLER FOR PLAYER: " + targetPlayer.getGameProfile().getName() + " | SUCCESS: " + success);
                            }

                            // Safely drops this specific player profile from the ledger map loop
                            iterator.remove();
                        }
                    }
                }
            } else {
                // Player stepped into torchlight or logged out; tick down their grace window
                data.gracePeriod--;

                data.ticksUntilNextPhase = data.gracePeriod;
                if (data.gracePeriod <= 0) {
                    iterator.remove();
                }
            }

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
            this.spawnAttemptTimerSecs = Math.max(0, data.spawnAttemptTimer / 20);
            this.stalkNoiseTimerSecs = Math.max(0, data.stalkNoiseTimer / 20);

            this.updateScoreboardDisplay(overworld, data);
        }
    }

    /// DEBUG
    // Scoreboard
    private void resetScoreboard(ServerLevel level) {
        net.minecraft.world.scores.Scoreboard scoreboard = level.getScoreboard();
        net.minecraft.world.scores.Objective objective = scoreboard.getObjective("§dweller_debug");
        
        if (this.debug) {
            System.out.println("Debug is enabled!");
            System.out.println("Enjoy the cool scoreboard!");
            if (scoreboard.hasObjective("§dweller_debug")) {
                scoreboard.removeObjective(objective);
            }

            if (!scoreboard.hasObjective("§dweller_debug")) {
                scoreboard.addObjective(
                        "§dweller_debug",
                        net.minecraft.world.scores.criteria.ObjectiveCriteria.DUMMY,
                        net.minecraft.network.chat.Component.literal("§e§l[DEBUG]§r MixinServerWorld Info"),
                        net.minecraft.world.scores.criteria.ObjectiveCriteria.RenderType.INTEGER
                );
            }

            objective = scoreboard.getObjective("§dweller_debug");
            scoreboard.setDisplayObjective(net.minecraft.world.scores.Scoreboard.DISPLAY_SLOT_SIDEBAR, objective);
        } else {
            System.out.println("Debug is not enabled!");
            if (scoreboard.hasObjective("§dweller_debug")) {
                scoreboard.removeObjective(objective);
            }
            
            return;
        }
    }

    public String[] getScoreboardText(PlayerTimerData data) {
        String gracePeriodSB;

        if (data.gracePeriod < 200) {
            gracePeriodSB = "§2Grace period: §4" + Utils.ticksToMinutesAndSeconds(data.gracePeriod);
        } else {
            gracePeriodSB = "§2Grace period: §6" + Utils.ticksToMinutesAndSeconds(data.gracePeriod);
        }

        String calmTimerMaxSecondsSB = "§fCalm Timer Max: §6" + Utils.ticksToSeconds(data.calmTimerMax) + "s§f (§6" + data.calmTimerMax + "t§f)";
        String calmTimerTicksSB = "§fCalm Timer (ticks): §6" + data.calmTimer + "t§r";
        String calmTimerMinutesSecondsSB = "§fCalm Timer (minutes): §6" + Utils.ticksToMinutesAndSeconds(data.calmTimer);
        String currentActivePhaseSB = "§fActive phase: " + this.currentActivePhaseName;

        String phase1StartSB = "§aPhase 1 §fstarts at §6" + this.phase1StartPercent + "%§f, or at §6" + this.phase1StartMinsAndSecs;

        int localVanillaSecs = data.vanillaCaveNoiseTimer < 0 ? -1 : data.vanillaCaveNoiseTimer / 20;
        String vanillaTimerMinutesSecondsSB = "§aVanilla Cave Noise Timer (minutes): §6" +
                (localVanillaSecs == -1 ? this.timerInactive : Utils.ticksToMinutesAndSeconds(data.vanillaCaveNoiseTimer));

        String phase2StartSB = "§bPhase 2 §fstarts at §6" + this.phase2StartPercent + "%§f, or at §6" + this.phase2StartMinsAndSecs;

        int localDwellerSecs = data.dwellerCaveNoiseTimer < 0 ? -1 : data.dwellerCaveNoiseTimer / 20;
        String dwellerTimerMinutesSecondsSB = "§bDweller Cave Noise Timer (minutes): §6" +
                (localDwellerSecs == -1 ? this.timerInactive : Utils.ticksToMinutesAndSeconds(data.dwellerCaveNoiseTimer));

        String dwellerExistsSB = "§6Dweller exists? " + (this.dwellerExistsFlag ? "§4YES§r" : "§cNO§r");
        String spawnAttemptTimerSecondsSB = "§eSpawn Attempt Timer (seconds): §6" + Math.max(0, data.spawnAttemptTimer / 20) + "s§r";
        String dwellerLifetimeSB = "§4Dweller currently exists for §6" + Utils.ticksToMinutesAndSeconds(this.dwellerAliveTimer) + "§4 more";
        String stalkNoiseTimerTicksSB = "§4Stalk Noise Timer (ticks): §6" + (data.stalkNoiseTimer <= 0 ? this.timerInactive : data.stalkNoiseTimer + "t§r");

        int localStalkSecs = Math.max(0, data.stalkNoiseTimer / 20);
        String stalkNoiseTimerMinutesSecondsSB = "§4Stalk Noise Timer (minutes): §6" +
                (localStalkSecs <= 0 ? this.timerInactive : Utils.ticksToMinutesAndSeconds(data.stalkNoiseTimer));

        String dwellerCurrentGoalSB = "§4Current goal: §6" + this.currentGoal;

        if (this.dwellerExistsFlag) {
            return new String[]{
                    gracePeriodSB,
                    calmTimerMaxSecondsSB,
                    calmTimerTicksSB,
                    calmTimerMinutesSecondsSB,
                    currentActivePhaseSB,
                    phase1StartSB,
                    vanillaTimerMinutesSecondsSB,
                    phase2StartSB,
                    dwellerTimerMinutesSecondsSB,
                    dwellerExistsSB,
                    dwellerLifetimeSB,
                    stalkNoiseTimerTicksSB,
                    stalkNoiseTimerMinutesSecondsSB,
                    dwellerCurrentGoalSB
            };
        } else if (!this.dwellerExistsFlag && data.activePhase == 3) {
            return new String[]{
                    gracePeriodSB,
                    calmTimerMaxSecondsSB,
                    calmTimerTicksSB,
                    calmTimerMinutesSecondsSB,
                    currentActivePhaseSB,
                    phase1StartSB,
                    vanillaTimerMinutesSecondsSB,
                    phase2StartSB,
                    dwellerTimerMinutesSecondsSB,
                    dwellerExistsSB,
                    spawnAttemptTimerSecondsSB
            };
        } else {
            return new String[]{
                    gracePeriodSB,
                    calmTimerMaxSecondsSB,
                    calmTimerTicksSB,
                    calmTimerMinutesSecondsSB,
                    currentActivePhaseSB,
                    phase1StartSB,
                    vanillaTimerMinutesSecondsSB,
                    phase2StartSB,
                    dwellerTimerMinutesSecondsSB,
                    dwellerExistsSB
            };
        }
    }

    private void updateScoreboardDisplay(ServerLevel level, PlayerTimerData data) {
        if (this.debug) {
            net.minecraft.world.scores.Scoreboard scoreboard = level.getScoreboard();
            net.minecraft.world.scores.Objective objective = scoreboard.getObjective("§dweller_debug");

            if (objective == null) {
                return;
            }

            for (String player : new ArrayList<>(scoreboard.getTrackedPlayers())) {
                if (player.startsWith("§") || player.contains("Remaining") || player.contains("Timer") || player.contains("Active")) {
                    scoreboard.resetPlayerScore(player, objective);
                }
            }

            String[] diagnosticLines = this.getScoreboardText(data);

            for (int i = 0; i < diagnosticLines.length; i++) {
                String lineText = diagnosticLines[i];
                int positionScore = diagnosticLines.length - 1 - i;

                scoreboard.getOrCreatePlayerScore(lineText, objective).setScore(positionScore);
            }
        } else {
            return;
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

        // If surface spawning is enabled, players at any height can trigger a spawn attempt
        if (!CaveNoise.CONFIG.ALLOW_SURFACE_SPAWN() && player.getY() > (double) CaveNoise.CONFIG.SPAWN_HEIGHT()) {
            return false;
        }

        Level serverLevel = player.level();
        BlockPos playerPos = player.blockPosition();

        // Dynamic real-time skylight adjustments factoring day/night cycle angles
        int skyLightLevel = serverLevel.getBrightness(LightLayer.SKY, playerPos) - serverLevel.getSkyDarken();
        if (skyLightLevel > 0) {
            float sunAngle = serverLevel.getSunAngle(1.0F);
            float f1 = sunAngle < (float) Math.PI ? 0.0F : ((float) Math.PI * 2F);
            sunAngle += (f1 - sunAngle) * 0.2F;
            skyLightLevel = Math.round((float) skyLightLevel * Mth.cos(sunAngle));
        }
        skyLightLevel = Mth.clamp(skyLightLevel, 0, 15);

        if (skyLightLevel > CaveNoise.CONFIG.SKY_LIGHT_LEVEL()) {
            return false;
        }

        int blockLightLevel = serverLevel.getBrightness(LightLayer.BLOCK, playerPos);
        if (blockLightLevel > this.randomlySelectedBrightnessLevel) {
            return false;
        }

        // If surface spawning is globally enabled, let them pass
        // Otherwise, require them to be completely hidden away from open sky blocks
        return CaveNoise.CONFIG.ALLOW_SURFACE_SPAWN() || !serverLevel.canSeeSky(playerPos);
    }

    public void listSpelunkers(net.minecraft.server.level.ServerPlayer player) {
        if (this.isPlayerSpelunker(player)) {
            this.spelunkers.add(player);
        }
    }
}
