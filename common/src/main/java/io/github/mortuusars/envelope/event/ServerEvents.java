package io.github.mortuusars.envelope.event;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public class ServerEvents {
    public static void serverStarted(MinecraftServer server) {
    }

    public static void serverTick(MinecraftServer server) {
    }

    public static void playerLogin(ServerPlayer player) {
        player.serverLevel().getEnvelopePlayerInformation().knownNames().add(player);
    }
}
