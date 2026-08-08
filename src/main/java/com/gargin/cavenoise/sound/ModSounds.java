package com.gargin.cavenoise.sound;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import java.util.function.Supplier;

public class ModSounds {
    public static final Supplier<SoundEvent> CAVENOISE_1 = registerSoundEvent("cavenoise_1");
    public static final Supplier<SoundEvent> CAVENOISE_2 = registerSoundEvent("cavenoise_2");
    public static final Supplier<SoundEvent> CAVENOISE_3 = registerSoundEvent("cavenoise_3");
    public static final Supplier<SoundEvent> CAVENOISE_4 = registerSoundEvent("cavenoise_4");
    public static final Supplier<SoundEvent> CHASE_STEP_1 = registerSoundEvent("chase_step_1");
    public static final Supplier<SoundEvent> CHASE_STEP_2 = registerSoundEvent("chase_step_2");
    public static final Supplier<SoundEvent> CHASE_STEP_3 = registerSoundEvent("chase_step_3");
    public static final Supplier<SoundEvent> CHASE_STEP_4 = registerSoundEvent("chase_step_4");
    public static final Supplier<SoundEvent> CHASE_1 = registerSoundEvent("chase_1");
    public static final Supplier<SoundEvent> CHASE_2 = registerSoundEvent("chase_2");
    public static final Supplier<SoundEvent> CHASE_3 = registerSoundEvent("chase_3");
    public static final Supplier<SoundEvent> CHASE_4 = registerSoundEvent("chase_4");
    public static final Supplier<SoundEvent> FLEE_1 = registerSoundEvent("flee_1");
    public static final Supplier<SoundEvent> FLEE_2 = registerSoundEvent("flee_2");
    public static final Supplier<SoundEvent> SPOTTED = registerSoundEvent("spotted");
    public static final Supplier<SoundEvent> DISAPPEAR = registerSoundEvent("disappear");
    public static final Supplier<SoundEvent> DWELLER_HURT_1 = registerSoundEvent("dweller_hurt_1");
    public static final Supplier<SoundEvent> DWELLER_HURT_2 = registerSoundEvent("dweller_hurt_2");
    public static final Supplier<SoundEvent> DWELLER_HURT_3 = registerSoundEvent("dweller_hurt_3");
    public static final Supplier<SoundEvent> DWELLER_HURT_4 = registerSoundEvent("dweller_hurt_4");
    public static final Supplier<SoundEvent> DWELLER_DEATH = registerSoundEvent("dweller_death");
    public static final Supplier<SoundEvent> DWELLER_STALK_1 = registerSoundEvent("dweller_stalk_1");
    public static final Supplier<SoundEvent> DWELLER_STALK_2 = registerSoundEvent("dweller_stalk_2");
    public static final Supplier<SoundEvent> DWELLER_STALK_3 = registerSoundEvent("dweller_stalk_3");
    public static final Supplier<SoundEvent> DWELLER_STALK_4 = registerSoundEvent("dweller_stalk_4");
    public static final Supplier<SoundEvent> DWELLER_STALK_5 = registerSoundEvent("dweller_stalk_5");
    public static final Supplier<SoundEvent> DWELLER_CLIMB_1 = registerSoundEvent("dweller_climb_1");
    public static final Supplier<SoundEvent> DWELLER_CLIMB_2 = registerSoundEvent("dweller_climb_2");
    public static final Supplier<SoundEvent> DWELLER_CLIMB_3 = registerSoundEvent("dweller_climb_3");
    public static final Supplier<SoundEvent> DWELLER_CLIMB_4 = registerSoundEvent("dweller_climb_4");
    public static final Supplier<SoundEvent> DWELLER_CLIMB_5 = registerSoundEvent("dweller_climb_5");
    public static final Supplier<SoundEvent> DWELLER_CLIMB_6 = registerSoundEvent("dweller_climb_6");
    public static final Supplier<SoundEvent> DWELLER_CLIMB_7 = registerSoundEvent("dweller_climb_7");
    public static final Supplier<SoundEvent> DWELLER_CLIMB_8 = registerSoundEvent("dweller_climb_8");

    private static Supplier<SoundEvent> registerSoundEvent(String name) {
        ResourceLocation id = new ResourceLocation("cavenoise", name);
        SoundEvent event = SoundEvent.createVariableRangeEvent(id);
        Registry.register(BuiltInRegistries.SOUND_EVENT, id, event);
        return () -> event;
    }

    public static void register() {}
}
