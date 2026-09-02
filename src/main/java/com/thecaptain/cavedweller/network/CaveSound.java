package com.thecaptain.cavedweller.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public class CaveSound {
    public final ResourceLocation soundResource;
    public final BlockPos playerPosition;
    public final float volume;
    public final float pitch;

    public CaveSound(ResourceLocation soundResource, BlockPos playerPosition, float volume, float pitch) {
        this.soundResource = soundResource;
        this.playerPosition = playerPosition;
        this.volume = volume;
        this.pitch = pitch;
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeResourceLocation(this.soundResource);
        buffer.writeBlockPos(this.playerPosition);
        buffer.writeFloat(this.volume);
        buffer.writeFloat(this.pitch);
    }

    public static CaveSound decode(FriendlyByteBuf buffer) {
        return new CaveSound(buffer.readResourceLocation(), buffer.readBlockPos(), buffer.readFloat(), buffer.readFloat());
    }
}