package com.thecaptain.cavedweller.registry;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;

public class ModSounds {
    public static final String MOD_ID = "cavedweller";

    public static final SoundEvent CAVEDWELLER_AMBIENT = SoundEvent.createVariableRangeEvent(new ResourceLocation("cavedweller", "cavedweller_ambient"));
    public static final SoundEvent CAVEDWELLER_STALK = SoundEvent.createVariableRangeEvent(new ResourceLocation("cavedweller", "cavedweller_stalk"));
    public static final SoundEvent CAVEDWELLER_STEP = SoundEvent.createVariableRangeEvent(new ResourceLocation("cavedweller", "cavedweller_step"));
    public static final SoundEvent CAVEDWELLER_TRANSITION = SoundEvent.createVariableRangeEvent(new ResourceLocation("cavedweller", "cavedweller_transition"));
    public static final SoundEvent CAVEDWELLER_CHASE = SoundEvent.createVariableRangeEvent(new ResourceLocation("cavedweller", "cavedweller_chase"));
    public static final SoundEvent CAVEDWELLER_HEARTBEAT = SoundEvent.createVariableRangeEvent(new ResourceLocation("cavedweller", "cavedweller_heartbeat"));
    public static final SoundEvent CAVEDWELLER_IDLE = SoundEvent.createVariableRangeEvent(new ResourceLocation("cavedweller", "cavedweller_idle"));
    public static final SoundEvent CAVEDWELLER_BREATHING = SoundEvent.createVariableRangeEvent(new ResourceLocation("cavedweller", "cavedweller_breathing"));
    public static final SoundEvent CAVEDWELLER_SPOTTED = SoundEvent.createVariableRangeEvent(new ResourceLocation("cavedweller", "cavedweller_spotted"));
    public static final SoundEvent CAVEDWELLER_SPOTTED_DISTANT = SoundEvent.createVariableRangeEvent(new ResourceLocation("cavedweller", "cavedweller_spotted_distant"));
    public static final SoundEvent CAVEDWELLER_DISAPPEAR = SoundEvent.createVariableRangeEvent(new ResourceLocation("cavedweller", "cavedweller_disappear"));
    public static final SoundEvent CAVEDWELLER_HURT = SoundEvent.createVariableRangeEvent(new ResourceLocation("cavedweller", "cavedweller_hurt"));
    public static final SoundEvent CAVEDWELLER_DEATH = SoundEvent.createVariableRangeEvent(new ResourceLocation("cavedweller", "cavedweller_death"));
    public static final SoundEvent CAVEDWELLER_CLIMB = SoundEvent.createVariableRangeEvent(new ResourceLocation("cavedweller", "cavedweller_climb"));

    private static SoundEvent register(String name) {
        ResourceLocation id = new ResourceLocation(MOD_ID, name);
        return Registry.register(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id));
    }

    public static void registerSounds() {
    }
}
