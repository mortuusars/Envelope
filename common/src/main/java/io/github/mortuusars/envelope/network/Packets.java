package io.github.mortuusars.envelope.network;

import dev.architectury.injectables.annotations.ExpectPlatform;
import io.github.mortuusars.envelope.PlatformHelper;
import io.github.mortuusars.envelope.network.packet.Packet;
import net.minecraft.core.RegistryAccess;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;

public class Packets {
    @ExpectPlatform
    public static void sendToServer(Packet packet) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static void sendToClient(Packet packet, ServerPlayer player) {
        throw new AssertionError();
    }

    // --

    public static void sendToAllClients(Packet packet) {
        MinecraftServer server = Objects.requireNonNull(PlatformHelper.getCurrentServer(),
                "Cannot send clientbound payloads on the client");

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            sendToClient(packet, player);
        }
    }

    public static void sendToAllClients(Function<RegistryAccess, Packet> packet) {
        MinecraftServer server = Objects.requireNonNull(PlatformHelper.getCurrentServer(),
              "Cannot send clientbound payloads on the client");

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            sendToClient(packet.apply(server.registryAccess()), player);
        }
    }

    public static void sendToOtherClients(@NotNull ServerPlayer except, Packet packet) {
        except.server.getPlayerList().getPlayers().forEach(player -> {
            if (!player.equals(except)) {
                sendToClient(packet, player);
            }
        });
    }

    public static void sendToClients(Packet packet, Predicate<ServerPlayer> filter) {
        MinecraftServer server = Objects.requireNonNull(PlatformHelper.getCurrentServer(),
                "Cannot send clientbound payloads on the client");

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (filter.test(player)) {
                sendToClient(packet, player);
            }
        }
    }

    public static void sendToPlayersNear(Packet packet, ServerLevel level, @Nullable ServerPlayer excluded,
                                         Entity entity, double radius) {
        sendToPlayersNear(packet, level, excluded, entity.getX(), entity.getY(), entity.getZ(), radius);
    }

    public static void sendToPlayersNear(Packet packet, @NotNull ServerLevel level, @Nullable ServerPlayer excludedPlayer,
                                         double x, double y, double z, double radius) {
        sendToClients(packet, player -> {
            if (player != excludedPlayer && player.level().dimension() == level.dimension()) {
                double d0 = x - player.getX();
                double d1 = y - player.getY();
                double d2 = z - player.getZ();
                return d0 * d0 + d1 * d1 + d2 * d2 < radius * radius;
            }

            return false;
        });
    }
}