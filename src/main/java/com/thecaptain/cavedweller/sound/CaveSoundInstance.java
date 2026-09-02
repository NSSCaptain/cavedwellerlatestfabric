package com.thecaptain.cavedweller.sound;

import net.minecraft.client.resources.sounds.AbstractSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

public class CaveSoundInstance extends AbstractSoundInstance {
    public CaveSoundInstance(SoundEvent pSoundEvent, float pVol, BlockPos pPos) {
        super(pSoundEvent, SoundSource.AMBIENT, RandomSource.create());
        this.volume = pVol;
        this.x = (double)pPos.getX();
        this.y = (double)pPos.getY();
        this.z = (double)pPos.getZ();
    }

    @Override
    public boolean isRelative() {
        return true;
    }
}
