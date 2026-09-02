package com.thecaptain.cavedweller.util;

import com.thecaptain.cavedweller.CaveDweller;
import com.thecaptain.cavedweller.entities.CaveDwellerEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;

public class Utils {
    public Utils() {
    }

    public static int ticksToSeconds(int ticks) {
        return ticks / 20;
    }

    public static int secondsToTicks(int seconds) {
        return seconds * 20;
    }

    public static int secondsToMinutes(int seconds) {
        return seconds / 60;
    }

    public static int minutesToTicks(int minutes) {
        return secondsToTicks(minutes * 60);
    }

    public static String ticksToMinutesAndSeconds(int ticks) {
        if (ticks <= 0) {
            return "00m00s";
        }

        // Convert ticks to seconds, rounding UP so 1-19 ticks display as 1 second
        int totalSeconds = (int) Math.ceil((double) ticks / 20.0);

        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;

        // %02d ensures the numbers always print as two digits (05 instead of 5)
        return String.format("%02dm%02ds", minutes, seconds);
    }

    public static String getTextureAppend() {
        return "";
    }

    public static boolean isValidPlayer(Entity entity) {
        if (!(entity instanceof Player player)) {
            return false;
        } else if (!player.isAlive()) {
            return false;
        } else if (!CaveDweller.CONFIG.TARGET_INVISIBLE() && player.isInvisible()) {
            return false;
        } else {
            return !player.isSpectator() && !player.isCreative();
        }
    }

    public static LivingEntity getValidTarget(@NotNull CaveDwellerEntity cavedweller) {
        return cavedweller.level().getNearestPlayer(cavedweller.getX(), cavedweller.getY(), cavedweller.getZ(), 200.0D, Utils::isValidPlayer);
    }

}
