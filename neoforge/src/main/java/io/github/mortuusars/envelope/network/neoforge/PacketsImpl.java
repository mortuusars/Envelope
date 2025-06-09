package io.github.mortuusars.envelope.network.neoforge;

import io.github.mortuusars.envelope.network.packet.Packet;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class PacketsImpl {
    public static void handle(Packet packet, IPayloadContext context) {
        packet.handle(context.flow(), context.player());
    }

    public static void sendToServer(Packet packet) {
        PacketDistributor.sendToServer(packet);
    }

    public static void sendToClient(Packet packet, ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, packet);
    }
}