package com.gargin.cavenoise.util.config;

import io.wispforest.owo.config.annotation.Config;
import io.wispforest.owo.config.annotation.Modmenu;
import io.wispforest.owo.config.annotation.RangeConstraint;
import io.wispforest.owo.config.annotation.RestartRequired;
import org.jetbrains.annotations.Range;

@Modmenu(
        modId = "cavenoise"
)
@Config(
        name = "cave-dweller-config",
        wrapperName = "CaveDwellerConfiguration"
)
/*
    currentSpeedMod = 5.0F;
    ticksCalmResetMin = 15000;
    ticksCalmResetMax = 18000;
    ticksCalmResetCooldown = 16000;
    dwellerCaveNoiseResetMin = 1600;
    dwellerCaveNoiseResetMax = 2000;
    calmTimer = 25000;
    dwellerCaveNoiseTimer = 4800;
    stalkNoiseMinTime = 800;
    stalkNoiseMaxTime = 1000;
    vanillaCaveNoiseStartMinTime = 8000;
    vanillaCaveNoiseStartMaxTime = 10000;
    vanillaCaveNoiseEndMinTime = 4000;
    vanillaCaveNoiseEndMaxTime = 6000;
    chanceOfSpawningAsStalker = 0.6F;
*/
// above numbers are in ticks
// config numbers are in seconds
// ticks / 20 = seconds
// seconds * 20 = ticks
// TODO: reduce default spawn rate?
public class ModConfigModel {
    /// Misc.
    // Give player darkness during chase?
    public boolean GIVE_DARKNESS = false; // default: false
    // Allow riding?
    // TODO: Doesn't work?
    public boolean ALLOW_RIDING_IN = false; // default: false
    // Can climb walls?
    public boolean CAN_CLIMB = true; // default: true
    // Can see + attack invisible players
    public boolean TARGET_INVISIBLE = false; // default: false
    // Disappear if target is no longer valid?
    public boolean DISAPPEAR = false; // default: false
    // Debug stuff
    public boolean DEBUG = false; // default: false

    /// Spawn chance(s)
    /// Unused
    //public double CHANCE_TO_SPAWN_AS_STALKER = 0.8F; // default: 0.8

    /// Timers
    // Calm timer
    @RangeConstraint(
       min = (double)120,
       max = (double)19998
    )
    public int RESET_CALM_MIN = 500; // default: 500
    @RangeConstraint(
            min = (double)240,
            max = (double)19998
    )
    public int RESET_CALM_MAX = 700; //default: 700
    @RangeConstraint(
            min = (double)0.0,
            max = (double)1.0
    )
    public double RESET_CALM_COOLDOWN_CHANCE = 0.2; // default: 0.1

    // Vanilla cave noise timer
    @RangeConstraint(
            min = (double)30,
            max = (double)19998
    )
    public int RESET_VANILLA_CAVE_NOISE_START_MIN = 60; // default: 400

    // Cave Dweller noise timer
    @RangeConstraint(
            min = (double)30,
            max = (double)19998
    )
    public int RESET_DWELLER_CAVE_NOISE_MIN = 60; // default: 500

    // Stalk noise timer
    @RangeConstraint(
            min = (double)30,
            max = (double)19998
    )
    public int RESET_STALK_NOISE_MIN = 30; // default: 30

    /// Spawning
    // Removed range constraint to account for worlds with custom heights
    // Extra spawn check added in CaveDwellerEntity to prevent issues
    public int SPAWN_HEIGHT = 0; // default: 0
    public boolean ALLOW_SURFACE_SPAWN = false; // default: false
    public int SKY_LIGHT_LEVEL = 0; // default: 0
    public int BLOCK_LIGHT_LEVEL = 0; // default: 0
    public int GRACE_PERIOD_BEFORE_RESET = 120; // default: 120

    /// Spotted range
    @RestartRequired
    public int SPOTTING_RANGE = 200; // default: 200

    /// Time until leaving
    // Time existing under normal circumstances
    public int TIME_UNTIL_LEAVE = 300; // default: 300
    // Time existing when chasing
    public int TIME_UNTIL_LEAVE_CHASE = 60; // default: 60

    /// Stats
    @RestartRequired
    // Max health
    // Health in Bedrock is 500. Might be because Bedrock doesn't ues the new combat, thus the health needed to account for that. I set it to 500 for parity reasons.
    public double MAX_HEALTH = (double)500.0F; // default: 500
    @RestartRequired
    // Movement speed
    // TODO: Speed up when dweller falls behind/hasn't attacked player in a period of time during chase
    public double MOVEMENT_SPEED = (double)0.32D; // default: 0.64
    @RestartRequired
    // Attack damage (in half hearts)
    public double ATTACK_DAMAGE = (double)6.0D; // default: 6.0
    @RestartRequired
    // Attack rate in seconds
    public int ATTACK_RATE = 1; // default: 1
    @RestartRequired
    @RangeConstraint(
            min = (double)0.0F,
            max = (double)1.0F
    )
    // Knockback resistance
    public double KNOCKBACK_RESISTANCE = (double)1.0F; // default: 1.0
    @RestartRequired
    // Armor
    public double ARMOR = (double)3.0F; // default: 3.0

    public ModConfigModel() {
    }
}