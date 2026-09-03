package com.thecaptain.cavedweller.util.config;

import io.wispforest.owo.config.annotation.Config;
import io.wispforest.owo.config.annotation.Modmenu;
import io.wispforest.owo.config.annotation.RangeConstraint;
import io.wispforest.owo.config.annotation.RestartRequired;

@Modmenu(
        modId = "cave_dweller"
)
@Config(
        name = "cave_dweller_config",
        wrapperName = "CaveDwellerConfiguration"
)
// config numbers are in seconds
// ticks / 20 = seconds
// seconds * 20 = ticks
public class ModConfigModel {
    /// Misc.
    // Give player darkness during chase?
    public boolean GIVE_DARKNESS = false; // default: false
    // Allow riding(?)
    // TODO: test
    public boolean ALLOW_RIDING_IN = false; // default: false
    // Can climb walls?
    public boolean CAN_CLIMB = true; // default: true
    // Can see + attack invisible players?
    public boolean TARGET_INVISIBLE = false; // default: false
    // Disappear if target is no longer valid?
    // Used only once, I think
    public boolean DISAPPEAR = false; // default: false
    // Debug stuff
    @RestartRequired
    public boolean DEBUG = false; // default: false

    /// Chances
    //Chance to spawn stalking
    @RangeConstraint(
            min = (double)0.0,
            max = (double)1.0
    )
    public double CHANCE_TO_SPAWN_STALKING = 0.4; // default: 0.4

    /// Timers
    // Calm timer
    @RangeConstraint(
       min = (double)120,
       max = (double)1000
    )
    public int RESET_CALM_MIN = 500; // default: 500
    @RangeConstraint(
            min = (double)240,
            max = (double)1000
    )
    public int RESET_CALM_MAX = 600; //default: 600
    @RangeConstraint(
            min = (double)0.0,
            max = (double)1.0
    )
    public double RESET_CALM_COOLDOWN_CHANCE = 0.1; // default: 0.2

    // Vanilla cave noise timer
    @RangeConstraint(
            min = (double)30,
            max = (double)1000
    )
    public int RESET_VANILLA_CAVE_NOISE_START_MIN = 60; // default: 60

    // Cave Dweller noise timer
    @RangeConstraint(
            min = (double)30,
            max = (double)1000
    )
    public int RESET_DWELLER_CAVE_NOISE_MIN = 60; // default: 60

    // Stalk noise timer
    @RangeConstraint(
            min = (double)10,
            max = (double)500
    )
    public int RESET_STALK_NOISE_MIN = 30; // default: 30

    // Grace period
    @RangeConstraint(
            min = (double)1,
            max = (double)1000
    )
    public int GRACE_PERIOD_BEFORE_RESET = 240; // default: 240

    /// Spawning
    // Removed range constraint to account for worlds with custom heights
    // Extra spawn check added in CaveDwellerEntity to prevent issues
    public int SPAWN_HEIGHT = 0; // default: 0
    public boolean ALLOW_SURFACE_SPAWN = false; // default: false
    @RangeConstraint(
            min = (double)0,
            max = (double)15
    )
    public int SKY_LIGHT_LEVEL = 0; // default: 0
    @RangeConstraint(
            min = (double)0,
            max = (double)15
    )
    public int BLOCK_LIGHT_LEVEL = 3; // default: 3

    /// Spotted range
    @RestartRequired
    @RangeConstraint(
            min = (double)2,
            max = (double)1000
    )
    public int SPOTTING_RANGE = 200; // default: 200

    /// Time until leaving
    // Time existing under normal circumstances
    @RangeConstraint(
            min = (double)30,
            max = (double)1000
    )
    public int TIME_UNTIL_LEAVE = 300; // default: 300
    // Time existing when chasing
    @RangeConstraint(
            min = (double)30,
            max = (double)1000
    )
    public int TIME_UNTIL_LEAVE_CHASE = 60; // default: 60

    /// Stats
    @RestartRequired
    // Max health
    // Health in Bedrock is 500. Might be because Bedrock doesn't ues the new combat, thus the health needed to account for that. I set it to 500 for parity reasons.
    public double MAX_HEALTH = (double)500.0F; // default: 500
    @RestartRequired
    // Movement speed
    @RangeConstraint(
            min = (double)0.1,
            max = (double)10
    )
    public double MOVEMENT_SPEED = (double)0.32D; // default: 0.64
    @RestartRequired
    // Attack damage (in half hearts)
    @RangeConstraint(
            min = (double)1.0,
            max = (double)333
    )
    public double ATTACK_DAMAGE = (double)6; // default: 6.0
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