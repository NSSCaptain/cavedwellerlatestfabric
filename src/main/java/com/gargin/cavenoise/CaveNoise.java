package com.gargin.cavenoise;

import com.gargin.cavenoise.entity.ModEntityTypes;
import com.gargin.cavenoise.entity.custom.CaveDwellerEntity;
import com.gargin.cavenoise.item.ModItems;
import com.gargin.cavenoise.sound.ModSounds;
import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.level.Level;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import org.slf4j.Logger;
import software.bernie.geckolib.GeckoLib;


public class CaveNoise implements ModInitializer {
    public static final String MODID = "cavenoise";
    private static final Logger LOGGER = LogUtils.getLogger();
    private float currentSpeedMod = 1.0F;
    private final int creepyCaveNoiseStart = 8000;
    private final int vanillaCaveNoiseStartBuild = 15000;
    private int ticksCalmResetMin;
    private int ticksCalmResetMax;
    private int ticksCalmResetCooldown;
    private int ticksNoiseResetMin;
    private int ticksNoiseResetMax;
    private int calmTimer = 0;
    private int noiseTimer = 0;
    private int stalkNoiseTimer = 0;
    private int vanillaNoiseTimer = 0;
    private boolean anySpelunkers = false;
    private List<Player> spelunkers = new ArrayList<>();


    @Override
    public void onInitialize() {
        this.currentSpeedMod = 5.0F;

        ModItems.register();
        ModEntityTypes.register();
        ModSounds.register();
        GeckoLib.initialize();
        FabricDefaultAttributeRegistry.register(ModEntityTypes.CAVE_DWELLER, CaveDwellerEntity.setAttributes());

        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.SPAWN_EGGS).register(content -> {
            content.accept(ModItems.CAVE_DWELLER_SPAWN_EGG.get());
        });

        // TODO: reduce spawn rate
        this.ticksCalmResetMin = 15000;         // default: 15000
        this.ticksCalmResetMax = 18000;         // default: 18000
        this.ticksCalmResetCooldown = 16000;    // default: 16000
        this.ticksNoiseResetMin = 2000;         // default: 2000
        this.ticksNoiseResetMax = 1600;         // default: 1600
        this.calmTimer = 25000;                 // default: 25000
        this.noiseTimer = 4800;                 // default: 4800

        ServerTickEvents.END_SERVER_TICK.register(this::serverTick);
    }

    public void onServerStarting() {
        LOGGER.info("HELLO from server starting");
        this.resetCalmTimer();
    }

    public void serverTick(net.minecraft.server.MinecraftServer server) {

        Iterable<Entity> entities = server.getLevel(Level.OVERWORLD).getAllEntities();
        AtomicBoolean dwellerExists = new AtomicBoolean(false);

        entities.forEach((entity) -> {
            if (entity instanceof CaveDwellerEntity) {
                dwellerExists.set(true);
                this.resetCalmTimer();
            }

        });

        // Was originally just --this.noiseTimer; vanillaNoiseTimer and stalkNoiseTimer did not exist
        this.noiseTimer -= (int)(this.currentSpeedMod);
        this.vanillaNoiseTimer -= (int)(this.currentSpeedMod);
        this.stalkNoiseTimer -= (int)(this.currentSpeedMod);

        if (!dwellerExists.get()) {
            if (this.noiseTimer <= 0 && this.calmTimer <= this.creepyCaveNoiseStart) {
                for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                    this.playCaveSoundToSpelunkers(player);
                }
            }
            if (this.vanillaNoiseTimer <= 0 && this.calmTimer <= this.vanillaCaveNoiseStartBuild) {
                for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                    this.playVanillaCaveSoundToSpelunkers(player);
                }
            } else if (this.stalkNoiseTimer <= 0) {
                for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                    this.playStalkSoundToSpelunkers(player);
                }
            }
        }

        boolean canSpawn = this.calmTimer <= 0;

        this.calmTimer -= (int)(this.currentSpeedMod);
        if (canSpawn && !dwellerExists.get()) {
            Random rand = new Random();
            // Default: 0.005
            double chanceToSpawnPerTick = 0.001;
            if (rand.nextDouble() <= chanceToSpawnPerTick) {
                this.spelunkers.clear();
                this.anySpelunkers = false;

                for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                    this.listSpelunkers(player);
                }

                if (this.anySpelunkers) {
                    Player victim = this.spelunkers.get(rand.nextInt(this.spelunkers.size()));
                    if (victim instanceof ServerPlayer serverPlayer) {
                        this.playCaveSoundToSpelunkers(serverPlayer);
                        net.minecraft.server.level.ServerLevel playerWorld = serverPlayer.serverLevel();
                        CaveDwellerEntity cavedweller = new CaveDwellerEntity(ModEntityTypes.CAVE_DWELLER, playerWorld);
                        cavedweller.setInvisible(true);
                        cavedweller.hasSpawned = true;
                        System.out.println("SPAWNED CD");
                        cavedweller.moveTo(cavedweller.generatePos(victim));
                        System.out.println("POS: " + cavedweller.position());
                        boolean success = playerWorld.addFreshEntity(cavedweller);
                        System.out.println("ADDED SUCCESSFULLY: " + success);

                        this.resetCalmTimer();
                    }
                }
            }
        }
    }


    public boolean listSpelunkers(ServerPlayer player) {
        if (this.checkIfPlayerIsSpelunker(player)) {
            this.anySpelunkers = true;
            this.spelunkers.add(player);
        }

        return true;
    }

    public boolean playCaveSoundToSpelunkers(ServerPlayer player) {
        int creepyCaveNoiseEndBuild = 1000;
        float a = (float)((this.calmTimer - creepyCaveNoiseEndBuild) / (this.creepyCaveNoiseStart - creepyCaveNoiseEndBuild));
        float b = 1.0F - a;
        b = Math.max(0.0F, b);
        b = Math.min(1.0F, b);
        float creepyCaveNoiseMinVol = 0.2F;
        float creepyCaveNoiseMaxVol = 1.0F;
        float vol = creepyCaveNoiseMinVol + (creepyCaveNoiseMaxVol - creepyCaveNoiseMinVol) * b;
        Random rand = new Random();
        if (this.checkIfPlayerIsSpelunker(player) && !player.isSpectator() && !player.isCreative()) {
            // There are supposed to be 10 sounds total, but I couldn't get the 10th to play via /playsound, so you only get 9, sorry
            SoundEvent selectedSound = switch (rand.nextInt(9)) {
                case 0 -> ModSounds.CAVENOISE_1.get();
                case 1 -> ModSounds.CAVENOISE_2.get();
                case 2 -> ModSounds.CAVENOISE_3.get();
                case 3 -> ModSounds.CAVENOISE_4.get();
                case 4 -> ModSounds.CAVENOISE_5.get();
                case 5 -> ModSounds.CAVENOISE_6.get();
                case 6 -> ModSounds.CAVENOISE_7.get();
                case 7 -> ModSounds.CAVENOISE_8.get();
                default -> ModSounds.CAVENOISE_9.get();
            };
            player.connection.send(new net.minecraft.network.protocol.game.ClientboundSoundPacket(
                    net.minecraft.core.registries.BuiltInRegistries.SOUND_EVENT.wrapAsHolder(selectedSound),
                    net.minecraft.sounds.SoundSource.AMBIENT,
                    player.getX(), player.getY(), player.getZ(),
                    vol, 1.0F,
                    player.level().getRandom().nextLong()
            ));

            this.resetNoiseTimer();
        }

        return true;
    }


    private void resetNoiseTimer() {
        Random rand = new Random();
        this.noiseTimer = this.ticksNoiseResetMin + rand.nextInt(this.ticksNoiseResetMax);
    }

    public boolean playVanillaCaveSoundToSpelunkers(ServerPlayer player) {
        float vol = 1.0F;
        if (this.checkIfPlayerIsSpelunker(player) && !player.isSpectator() && !player.isCreative()) {
            player.connection.send(new net.minecraft.network.protocol.game.ClientboundSoundPacket(
                    net.minecraft.core.registries.BuiltInRegistries.SOUND_EVENT.wrapAsHolder(net.minecraft.sounds.SoundEvents.AMBIENT_CAVE.value()),
                    net.minecraft.sounds.SoundSource.AMBIENT,
                    player.getX(), player.getY(), player.getZ(),
                    vol, 1.0F,
                    player.level().getRandom().nextLong()
            ));
            this.resetVanillaNoiseTimer();
        }

        return true;
    }

    private void resetVanillaNoiseTimer() {
        int vanillaCaveNoiseEndBuild = 2000;
        float a = (float)((this.calmTimer - vanillaCaveNoiseEndBuild) / (this.vanillaCaveNoiseStartBuild - vanillaCaveNoiseEndBuild));
        a = Math.max(0.0F, a);
        a = Math.min(1.0F, a);
        float b = 1.0F - a;
        int vanillaCaveNoiseStartMinTime = 8000;
        int vanillaCaveNoiseEndMinTime = 4000;
        int newMin = Math.round((float)(vanillaCaveNoiseEndMinTime - vanillaCaveNoiseStartMinTime) * b + (float) vanillaCaveNoiseStartMinTime);
        int vanillaCaveNoiseStartMaxTime = 10000;
        int vanillaCaveNoiseEndMaxTime = 6000;
        int newMax = Math.round((float)(vanillaCaveNoiseEndMaxTime - vanillaCaveNoiseStartMaxTime) * b + (float) vanillaCaveNoiseStartMaxTime);
        Random rand = new Random();
        this.vanillaNoiseTimer = rand.nextInt(newMax - newMin) + newMin;
    }

    private boolean playStalkSoundToSpelunkers(ServerPlayer player) {
        Random rand = new Random();
        double targetX = player.getX() + (double)(-25 + rand.nextInt(50));
        double targetY = player.getY();
        double targetZ = player.getZ() + (double)(-25 + rand.nextInt(50));
        if (this.checkIfPlayerIsSpelunker(player) && !player.isSpectator() && !player.isCreative()) {
            SoundEvent selectedSound = switch (rand.nextInt(5)) {
                case 0 -> ModSounds.DWELLER_STALK_1.get();
                case 1 -> ModSounds.DWELLER_STALK_2.get();
                case 2 -> ModSounds.DWELLER_STALK_3.get();
                case 3 -> ModSounds.DWELLER_STALK_4.get();
                default -> ModSounds.DWELLER_STALK_5.get();
            };
            player.connection.send(new net.minecraft.network.protocol.game.ClientboundSoundPacket(
                    net.minecraft.core.registries.BuiltInRegistries.SOUND_EVENT.wrapAsHolder(selectedSound),
                    net.minecraft.sounds.SoundSource.AMBIENT,
                    targetX, targetY, targetZ,
                    2.0F, 1.0F,
                    player.level().getRandom().nextLong()
            ));
            this.resetStalkNoiseTimer();
        }
        return true;
    }


    private void resetStalkNoiseTimer() {
        Random rand = new Random();
        int stalkNoiseMinTime = 800;
        int stalkNoiseMaxTime = 1000;
        this.stalkNoiseTimer = stalkNoiseMinTime + rand.nextInt(stalkNoiseMaxTime - stalkNoiseMinTime);
    }

    public boolean checkIfPlayerIsSpelunker(Player player) {
        if (player == null) {
            return false;
        } else {
            Level level = player.level();
            BlockPos playerBlockPos = BlockPos.containing(player.position());
            return player.position().y < 1.0D && !level.canSeeSky(playerBlockPos);
        }
    }

    private void resetCalmTimer() {
        Random rand = new Random();
        this.calmTimer = this.ticksCalmResetMin + rand.nextInt(this.ticksCalmResetMax);
        // Default: 0.4
        double chanceToCooldown = 0.6;
        if (rand.nextDouble() <= chanceToCooldown) {
            this.calmTimer = this.ticksCalmResetCooldown + rand.nextInt(this.ticksCalmResetCooldown);
        }

    }
}
