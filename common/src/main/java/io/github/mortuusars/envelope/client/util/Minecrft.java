package io.github.mortuusars.envelope.client.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.RegistryAccess;

import java.util.Objects;

public class Minecrft {
    public static Minecraft get() {
        return Minecraft.getInstance();
    }

    public static LocalPlayer player() {
        return Objects.requireNonNull(get().player, "Player is not available.");
    }

    public static ClientLevel level() {
        return Objects.requireNonNull(get().level, "Level is not available.");
    }

    public static RegistryAccess registryAccess() {
        return level().registryAccess();
    }

    public static MultiPlayerGameMode gameMode() {
        return Objects.requireNonNull(get().gameMode, "GameMode is not available.");
    }

    public static Options options() {
        return get().options;
    }

    public static void execute(Runnable runnable) {
        get().execute(runnable);
    }

    public static void releaseUseButton() {
        options().keyUse.setDown(false);
    }
}
