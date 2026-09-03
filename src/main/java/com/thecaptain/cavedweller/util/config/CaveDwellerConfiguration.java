package com.thecaptain.cavedweller.util.config;

import blue.endless.jankson.Jankson;
import io.wispforest.owo.config.ConfigWrapper;
import io.wispforest.owo.config.Option;
import java.util.function.Consumer;

public class CaveDwellerConfiguration extends ConfigWrapper<ModConfigModel> {
    public final Keys keys = new Keys();
    private final Option<Boolean> GIVE_DARKNESS;
    private final Option<Boolean> ALLOW_RIDING_IN;
    private final Option<Boolean> CAN_CLIMB;
    private final Option<Boolean> TARGET_INVISIBLE;
    private final Option<Boolean> DISAPPEAR;
    private final Option<Boolean> DEBUG;
    private final Option<Double> CHANCE_TO_SPAWN_STALKING;
    private final Option<Integer> RESET_CALM_MIN;
    private final Option<Integer> RESET_CALM_MAX;
    private final Option<Double> RESET_CALM_COOLDOWN_CHANCE;
    private final Option<Integer> RESET_VANILLA_CAVE_NOISE_START_MIN;
    private final Option<Integer> RESET_DWELLER_CAVE_NOISE_MIN;
    private final Option<Integer> RESET_STALK_NOISE_MIN;
    private final Option<Integer> SPAWN_HEIGHT;
    private final Option<Boolean> ALLOW_SURFACE_SPAWN;
    private final Option<Integer> SKY_LIGHT_LEVEL;
    private final Option<Integer> BLOCK_LIGHT_LEVEL;
    private final Option<Integer> GRACE_PERIOD_BEFORE_RESET;
    private final Option<Integer> SPOTTING_RANGE;
    private final Option<Integer> TIME_UNTIL_LEAVE;
    private final Option<Integer> TIME_UNTIL_LEAVE_CHASE;
    private final Option<Double> MAX_HEALTH;
    private final Option<Double> MOVEMENT_SPEED;
    private final Option<Double> ATTACK_DAMAGE;
    private final Option<Integer> ATTACK_RATE;
    private final Option<Double> KNOCKBACK_RESISTANCE;
    private final Option<Double> ARMOR;

    private CaveDwellerConfiguration() {
        super(ModConfigModel.class);
        this.GIVE_DARKNESS = this.optionForKey(this.keys.GIVE_DARKNESS);
        this.DISAPPEAR = this.optionForKey(this.keys.DISAPPEAR);
        this.DEBUG = this.optionForKey(this.keys.DEBUG);
        this.CHANCE_TO_SPAWN_STALKING = this.optionForKey(this.keys.CHANCE_TO_SPAWN_STALKING);
        this.RESET_CALM_MIN = this.optionForKey(this.keys.RESET_CALM_MIN);
        this.RESET_CALM_MAX = this.optionForKey(this.keys.RESET_CALM_MAX);
        this.RESET_CALM_COOLDOWN_CHANCE = this.optionForKey(this.keys.RESET_CALM_COOLDOWN_CHANCE);
        this.RESET_DWELLER_CAVE_NOISE_MIN = this.optionForKey(this.keys.RESET_DWELLER_CAVE_NOISE_MIN);
        this.RESET_STALK_NOISE_MIN = this.optionForKey(this.keys.RESET_STALK_NOISE_MIN);
        this.RESET_VANILLA_CAVE_NOISE_START_MIN = this.optionForKey(this.keys.RESET_VANILLA_CAVE_NOISE_START_MIN);
        this.SPAWN_HEIGHT = this.optionForKey(this.keys.SPAWN_HEIGHT);
        this.ALLOW_SURFACE_SPAWN = this.optionForKey(this.keys.ALLOW_SURFACE_SPAWN);
        this.SKY_LIGHT_LEVEL = this.optionForKey(this.keys.SKY_LIGHT_LEVEL);
        this.BLOCK_LIGHT_LEVEL = this.optionForKey(this.keys.BLOCK_LIGHT_LEVEL);
        this.GRACE_PERIOD_BEFORE_RESET = this.optionForKey(this.keys.GRACE_PERIOD_BEFORE_RESET);
        this.SPOTTING_RANGE = this.optionForKey(this.keys.SPOTTING_RANGE);
        this.TIME_UNTIL_LEAVE = this.optionForKey(this.keys.TIME_UNTIL_LEAVE);
        this.TIME_UNTIL_LEAVE_CHASE = this.optionForKey(this.keys.TIME_UNTIL_LEAVE_CHASE);
        this.CAN_CLIMB = this.optionForKey(this.keys.CAN_CLIMB);
        this.ALLOW_RIDING_IN = this.optionForKey(this.keys.ALLOW_RIDING_IN);
        this.TARGET_INVISIBLE = this.optionForKey(this.keys.TARGET_INVISIBLE);
        this.MAX_HEALTH = this.optionForKey(this.keys.MAX_HEALTH);
        this.ATTACK_DAMAGE = this.optionForKey(this.keys.ATTACK_DAMAGE);
        this.ATTACK_RATE = this.optionForKey(this.keys.ATTACK_RATE);
        this.MOVEMENT_SPEED = this.optionForKey(this.keys.MOVEMENT_SPEED);
        this.KNOCKBACK_RESISTANCE = this.optionForKey(this.keys.KNOCKBACK_RESISTANCE);
        this.ARMOR = this.optionForKey(this.keys.ARMOR);
    }

    private CaveDwellerConfiguration(Consumer<Jankson.Builder> janksonBuilder) {
        super(ModConfigModel.class, janksonBuilder);
        this.GIVE_DARKNESS = this.optionForKey(this.keys.GIVE_DARKNESS);
        this.DISAPPEAR = this.optionForKey(this.keys.DISAPPEAR);
        this.DEBUG = this.optionForKey(this.keys.DEBUG);
        this.CHANCE_TO_SPAWN_STALKING = this.optionForKey(this.keys.CHANCE_TO_SPAWN_STALKING);
        this.RESET_CALM_MIN = this.optionForKey(this.keys.RESET_CALM_MIN);
        this.RESET_CALM_MAX = this.optionForKey(this.keys.RESET_CALM_MAX);
        this.RESET_CALM_COOLDOWN_CHANCE = this.optionForKey(this.keys.RESET_CALM_COOLDOWN_CHANCE);
        this.RESET_DWELLER_CAVE_NOISE_MIN = this.optionForKey(this.keys.RESET_DWELLER_CAVE_NOISE_MIN);
        this.RESET_STALK_NOISE_MIN = this.optionForKey(this.keys.RESET_STALK_NOISE_MIN);
        this.RESET_VANILLA_CAVE_NOISE_START_MIN = this.optionForKey(this.keys.RESET_VANILLA_CAVE_NOISE_START_MIN);
        this.SPAWN_HEIGHT = this.optionForKey(this.keys.SPAWN_HEIGHT);
        this.ALLOW_SURFACE_SPAWN = this.optionForKey(this.keys.ALLOW_SURFACE_SPAWN);
        this.SKY_LIGHT_LEVEL = this.optionForKey(this.keys.SKY_LIGHT_LEVEL);
        this.BLOCK_LIGHT_LEVEL = this.optionForKey(this.keys.BLOCK_LIGHT_LEVEL);
        this.GRACE_PERIOD_BEFORE_RESET = this.optionForKey(this.keys.GRACE_PERIOD_BEFORE_RESET);
        this.SPOTTING_RANGE = this.optionForKey(this.keys.SPOTTING_RANGE);
        this.TIME_UNTIL_LEAVE = this.optionForKey(this.keys.TIME_UNTIL_LEAVE);
        this.TIME_UNTIL_LEAVE_CHASE = this.optionForKey(this.keys.TIME_UNTIL_LEAVE_CHASE);
        this.CAN_CLIMB = this.optionForKey(this.keys.CAN_CLIMB);
        this.ALLOW_RIDING_IN = this.optionForKey(this.keys.ALLOW_RIDING_IN);
        this.TARGET_INVISIBLE = this.optionForKey(this.keys.TARGET_INVISIBLE);
        this.MAX_HEALTH = this.optionForKey(this.keys.MAX_HEALTH);
        this.ATTACK_DAMAGE = this.optionForKey(this.keys.ATTACK_DAMAGE);
        this.ATTACK_RATE = this.optionForKey(this.keys.ATTACK_RATE);
        this.MOVEMENT_SPEED = this.optionForKey(this.keys.MOVEMENT_SPEED);
        this.KNOCKBACK_RESISTANCE = this.optionForKey(this.keys.KNOCKBACK_RESISTANCE);
        this.ARMOR = this.optionForKey(this.keys.ARMOR);
    }

    public static CaveDwellerConfiguration createAndLoad() {
        CaveDwellerConfiguration wrapper = new CaveDwellerConfiguration();
        wrapper.load();
        return wrapper;
    }

    public static CaveDwellerConfiguration createAndLoad(Consumer<Jankson.Builder> janksonBuilder) {
        CaveDwellerConfiguration wrapper = new CaveDwellerConfiguration(janksonBuilder);
        wrapper.load();
        return wrapper;
    }

    public boolean GIVE_DARKNESS() {
        return (Boolean)this.GIVE_DARKNESS.value();
    }

    public void GIVE_DARKNESS(boolean value) {
        this.GIVE_DARKNESS.set(value);
    }

    public boolean DISAPPEAR() {
        return (Boolean)this.DISAPPEAR.value();
    }

    public void DISAPPEAR(boolean value) {
        this.DISAPPEAR.set(value);
    }
    
    public boolean DEBUG() {
        return (Boolean) this.DEBUG.value();
    }

    public void DEBUG(boolean value) {
        this.DEBUG.set(value);
    }

    public double CHANCE_TO_SPAWN_STALKING() {
        return (Double)this.CHANCE_TO_SPAWN_STALKING.value();
    }

    public void CHANCE_TO_SPAWN_STALKING(double value) {
        this.CHANCE_TO_SPAWN_STALKING.set(value);
    }

    public int RESET_CALM_MIN() {
        return (Integer)this.RESET_CALM_MIN.value();
    }

    public void RESET_CALM_MIN(int value) {
        this.RESET_CALM_MIN.set(value);
    }

    public int RESET_CALM_MAX() {
        return (Integer)this.RESET_CALM_MAX.value();
    }

    public void RESET_CALM_MAX(int value) {
        this.RESET_CALM_MAX.set(value);
    }

    public double RESET_CALM_COOLDOWN_CHANCE() {
        return (Double)this.RESET_CALM_COOLDOWN_CHANCE.value();
    }

    public void RESET_CALM_COOLDOWN_CHANCE(double value) {
        this.RESET_CALM_COOLDOWN_CHANCE.set(value);
    }

    public int RESET_DWELLER_CAVE_NOISE_MIN() {
        return (Integer)this.RESET_DWELLER_CAVE_NOISE_MIN.value();
    }

    public void RESET_DWELLER_CAVE_NOISE_MIN(int value) {
        this.RESET_DWELLER_CAVE_NOISE_MIN.set(value);
    }

    public int RESET_STALK_NOISE_MIN() {
        return (Integer)this.RESET_STALK_NOISE_MIN.value();
    }

    public void RESET_STALK_NOISE_MIN(int value) {
        this.RESET_STALK_NOISE_MIN.set(value);
    }

    public int RESET_VANILLA_CAVE_NOISE_START_MIN() {
        return (Integer)this.RESET_VANILLA_CAVE_NOISE_START_MIN.value();
    }

    public void RESET_VANILLA_CAVE_NOISE_START_MIN(int value) {
        this.RESET_VANILLA_CAVE_NOISE_START_MIN.set(value);
    }

    public int SPAWN_HEIGHT() {
        return (Integer)this.SPAWN_HEIGHT.value();
    }

    public void SPAWN_HEIGHT(int value) {
        this.SPAWN_HEIGHT.set(value);
    }

    public boolean ALLOW_SURFACE_SPAWN() {
        return (Boolean)this.ALLOW_SURFACE_SPAWN.value();
    }

    public void ALLOW_SURFACE_SPAWN(boolean value) {
        this.ALLOW_SURFACE_SPAWN.set(value);
    }

    public int SKY_LIGHT_LEVEL() {
        return (Integer)this.SKY_LIGHT_LEVEL.value();
    }

    public void SKY_LIGHT_LEVEL(int value) {
        this.SKY_LIGHT_LEVEL.set(value);
    }

    public int BLOCK_LIGHT_LEVEL() {
        return (Integer)this.BLOCK_LIGHT_LEVEL.value();
    }

    public void BLOCK_LIGHT_LEVEL(int value) {
        this.BLOCK_LIGHT_LEVEL.set(value);
    }

    public int GRACE_PERIOD_BEFORE_RESET() {
        return (Integer)this.GRACE_PERIOD_BEFORE_RESET.value();
    }

    public void GRACE_PERIOD_BEFORE_RESET(int value) {
        this.GRACE_PERIOD_BEFORE_RESET.set(value);
    }

    public int SPOTTING_RANGE() {
        return (Integer)this.SPOTTING_RANGE.value();
    }

    public void SPOTTING_RANGE(int value) {
        this.SPOTTING_RANGE.set(value);
    }

    public int TIME_UNTIL_LEAVE() {
        return (Integer)this.TIME_UNTIL_LEAVE.value();
    }

    public void TIME_UNTIL_LEAVE(int value) {
        this.TIME_UNTIL_LEAVE.set(value);
    }

    public int TIME_UNTIL_LEAVE_CHASE() {
        return (Integer)this.TIME_UNTIL_LEAVE_CHASE.value();
    }

    public void TIME_UNTIL_LEAVE_CHASE(int value) {
        this.TIME_UNTIL_LEAVE_CHASE.set(value);
    }

    public boolean CAN_CLIMB() {
        return (Boolean)this.CAN_CLIMB.value();
    }

    public void CAN_CLIMB(boolean value) {
        this.CAN_CLIMB.set(value);
    }

    public boolean ALLOW_RIDING_IN() {
        return (Boolean)this.ALLOW_RIDING_IN.value();
    }

    public void ALLOW_RIDING_IN(boolean value) {
        this.ALLOW_RIDING_IN.set(value);
    }

    public boolean TARGET_INVISIBLE() {
        return (Boolean)this.TARGET_INVISIBLE.value();
    }

    public void TARGET_INVISIBLE(boolean value) {
        this.TARGET_INVISIBLE.set(value);
    }

    public double MAX_HEALTH() {
        return (Double)this.MAX_HEALTH.value();
    }

    public void MAX_HEALTH(double value) {
        this.MAX_HEALTH.set(value);
    }

    public double ATTACK_DAMAGE() {
        return (Double)this.ATTACK_DAMAGE.value();
    }

    public void ATTACK_DAMAGE(double value) {
        this.ATTACK_DAMAGE.set(value);
    }

    public double ATTACK_RATE() {
        return (Integer)this.ATTACK_RATE.value();
    }

    public void ATTACK_RATE(int value) {
        this.ATTACK_RATE.set(value);
    }

    public double MOVEMENT_SPEED() {
        return (Double)this.MOVEMENT_SPEED.value();
    }

    public void MOVEMENT_SPEED(double value) {
        this.MOVEMENT_SPEED.set(value);
    }

    public double KNOCKBACK_RESISTANCE() {
        return (Double)this.KNOCKBACK_RESISTANCE.value();
    }

    public void KNOCKBACK_RESISTANCE(double value) {
        this.KNOCKBACK_RESISTANCE.set(value);
    }

    public double ARMOR() {
        return (Double)this.ARMOR.value();
    }

    public void ARMOR(double value) {
        this.ARMOR.set(value);
    }

    public static class Keys {
        public final Option.Key GIVE_DARKNESS = new Option.Key("GIVE_DARKNESS");
        public final Option.Key DISAPPEAR = new Option.Key("DISAPPEAR");
        public final Option.Key DEBUG = new Option.Key("DEBUG");
        public final Option.Key CHANCE_TO_SPAWN_STALKING = new Option.Key("CHANCE_TO_SPAWN_STALKING");
        public final Option.Key RESET_CALM_MIN = new Option.Key("RESET_CALM_MIN");
        public final Option.Key RESET_CALM_MAX = new Option.Key("RESET_CALM_MAX");
        public final Option.Key RESET_CALM_COOLDOWN_CHANCE = new Option.Key("RESET_CALM_COOLDOWN_CHANCE");
        public final Option.Key RESET_DWELLER_CAVE_NOISE_MIN = new Option.Key("RESET_DWELLER_CAVE_NOISE_MIN");
        public final Option.Key RESET_STALK_NOISE_MIN = new Option.Key("RESET_STALK_NOISE_MIN");
        public final Option.Key RESET_VANILLA_CAVE_NOISE_START_MIN = new Option.Key("RESET_VANILLA_CAVE_NOISE_START_MIN");
        public final Option.Key SPAWN_HEIGHT = new Option.Key("SPAWN_HEIGHT");
        public final Option.Key ALLOW_SURFACE_SPAWN = new Option.Key("ALLOW_SURFACE_SPAWN");
        public final Option.Key SKY_LIGHT_LEVEL = new Option.Key("SKY_LIGHT_LEVEL");
        public final Option.Key BLOCK_LIGHT_LEVEL = new Option.Key("BLOCK_LIGHT_LEVEL");
        public final Option.Key GRACE_PERIOD_BEFORE_RESET = new Option.Key("GRACE_PERIOD_BEFORE_RESET");
        public final Option.Key SPOTTING_RANGE = new Option.Key("SPOTTING_RANGE");
        public final Option.Key TIME_UNTIL_LEAVE = new Option.Key("TIME_UNTIL_LEAVE");
        public final Option.Key TIME_UNTIL_LEAVE_CHASE = new Option.Key("TIME_UNTIL_LEAVE_CHASE");
        public final Option.Key CAN_CLIMB = new Option.Key("CAN_CLIMB");
        public final Option.Key ALLOW_RIDING_IN = new Option.Key("ALLOW_RIDING_IN");
        public final Option.Key TARGET_INVISIBLE = new Option.Key("TARGET_INVISIBLE");
        public final Option.Key MAX_HEALTH = new Option.Key("MAX_HEALTH");
        public final Option.Key ATTACK_DAMAGE = new Option.Key("ATTACK_DAMAGE");
        public final Option.Key ATTACK_RATE = new Option.Key("ATTACK_RATE");
        public final Option.Key MOVEMENT_SPEED = new Option.Key("MOVEMENT_SPEED");
        public final Option.Key KNOCKBACK_RESISTANCE = new Option.Key("KNOCKBACK_RESISTANCE");
        public final Option.Key ARMOR = new Option.Key("ARMOR");

        public Keys() {
        }
    }
}