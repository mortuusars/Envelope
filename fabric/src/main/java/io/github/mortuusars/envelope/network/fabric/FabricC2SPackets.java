package io.github.mortuusars.envelope.network.fabric;

import io.github.mortuusars.envelope.network.packet.C2SPackets;
import io.github.mortuusars.envelope.network.packet.CommonPackets;
import io.github.mortuusars.envelope.network.packet.Packet;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public class FabricC2SPackets {
    @SuppressWarnings("unchecked")
    public static void register() {
        // This monstrosity is to avoid having to define packets for forge and fabric separately.
        for (var definition : C2SPackets.getDefinitions()) {
            PayloadTypeRegistry.playC2S().register(
                    (CustomPacketPayload.Type<CustomPacketPayload>) definition.type(),
                    (StreamCodec<FriendlyByteBuf, CustomPacketPayload>) definition.codec().cast());
            ServerPlayNetworking.registerGlobalReceiver((CustomPacketPayload.Type<Packet>) definition.type(), FabricC2SPackets::handleServerboundPacket);
        }

        for (var definition : CommonPackets.getDefinitions()) {
            PayloadTypeRegistry.playC2S().register(
                    (CustomPacketPayload.Type<CustomPacketPayload>) definition.type(),
                    (StreamCodec<FriendlyByteBuf, CustomPacketPayload>) definition.codec().cast());
            ServerPlayNetworking.registerGlobalReceiver((CustomPacketPayload.Type<Packet>) definition.type(), FabricC2SPackets::handleServerboundPacket);
        }
    }

    private static <T extends Packet> void handleServerboundPacket(T payload, ServerPlayNetworking.Context context) {
        payload.handle(PacketFlow.SERVERBOUND, context.player());
    }

    public static void sendToServer(Packet packet) {
        ClientPlayNetworking.send(packet);
    }
}