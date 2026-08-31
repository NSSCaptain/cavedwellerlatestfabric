package com.gargin.cavenoise.registry;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;

public class ModSounds {
    public static final String MOD_ID = "cavenoise";

    public static final SoundEvent CAVENOISE_1 = register("cavenoise_1");
    public static final SoundEvent CAVENOISE_2 = register("cavenoise_2");
    public static final SoundEvent CAVENOISE_3 = register("cavenoise_3");
    public static final SoundEvent CAVENOISE_4 = register("cavenoise_4");
    public static final SoundEvent CAVENOISE_5 = register("cavenoise_5");
    public static final SoundEvent CAVENOISE_6 = register("cavenoise_6");
    public static final SoundEvent CAVENOISE_7 = register("cavenoise_7");
    public static final SoundEvent CAVENOISE_8 = register("cavenoise_8");
    public static final SoundEvent CAVENOISE_9 = register("cavenoise_9");

    public static final SoundEvent STALK_1 = register("stalk_1");
    public static final SoundEvent STALK_2 = register("stalk_2");
    public static final SoundEvent STALK_3 = register("stalk_3");
    public static final SoundEvent STALK_4 = register("stalk_4");
    public static final SoundEvent STALK_5 = register("stalk_5");

    public static final SoundEvent STEP_1 = register("step_1");
    public static final SoundEvent STEP_2 = register("step_2");
    public static final SoundEvent STEP_3 = register("step_3");
    public static final SoundEvent STEP_4 = register("step_4");

    public static final SoundEvent TRANSITION_1 = register("transition_1");
    public static final SoundEvent TRANSITION_2 = register("transition_2");
    public static final SoundEvent TRANSITION_3 = register("transition_3");
    public static final SoundEvent TRANSITION_4 = register("transition_4");

    public static final SoundEvent CHASE_1 = register("chase_1");
    public static final SoundEvent CHASE_2 = register("chase_2");
    public static final SoundEvent CHASE_3 = register("chase_3");
    public static final SoundEvent CHASE_4 = register("chase_4");

    public static final SoundEvent HEARTBEAT = register("heartbeat");

    public static final SoundEvent IDLE_1 = register("idle_1");
    public static final SoundEvent IDLE_2 = register("idle_2");
    public static final SoundEvent BREATHING_1 = register("breathing_1");
    public static final SoundEvent BREATHING_2 = register("breathing_2");
    public static final SoundEvent BREATHING_3 = register("breathing_3");

    public static final SoundEvent SPOTTED_1 = register("spotted_1");
    public static final SoundEvent SPOTTED_2 = register("spotted_2");
    public static final SoundEvent SPOTTED_3 = register("spotted_3");
    public static final SoundEvent SPOTTED_DISTANT = register("spotted_distant");

    public static final SoundEvent DISAPPEAR = register("disappear");

    public static final SoundEvent HURT_1 = register("hurt_1");
    public static final SoundEvent HURT_2 = register("hurt_2");
    public static final SoundEvent HURT_3 = register("hurt_3");
    public static final SoundEvent HURT_4 = register("hurt_4");

    public static final SoundEvent DWELLER_DEATH_1 = register("dweller_death_1");
    public static final SoundEvent DWELLER_DEATH_2 = register("dweller_death_2");
    public static final SoundEvent DWELLER_DEATH_3 = register("dweller_death_3");
    public static final SoundEvent DWELLER_DEATH_4 = register("dweller_death_4");

    public static final SoundEvent CLIMB_1 = register("climb_1");
    public static final SoundEvent CLIMB_2 = register("climb_2");
    public static final SoundEvent CLIMB_3 = register("climb_3");
    public static final SoundEvent CLIMB_4 = register("climb_4");
    public static final SoundEvent CLIMB_5 = register("climb_5");
    public static final SoundEvent CLIMB_6 = register("climb_6");
    public static final SoundEvent CLIMB_7 = register("climb_7");
    public static final SoundEvent CLIMB_8 = register("climb_8");

    private static SoundEvent register(String name) {
        ResourceLocation id = new ResourceLocation(MOD_ID, name);
        return Registry.register(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id));
    }

    public static void registerSounds() {
    }
}
