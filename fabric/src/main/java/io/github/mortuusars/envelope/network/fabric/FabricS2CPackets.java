package io.github.mortuusars.envelope.network.fabric;

import io.github.mortuusars.envelope.network.packet.CommonPackets;
import io.github.mortuusars.envelope.network.packet.S2CPackets;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public class FabricS2CPackets {
    @SuppressWarnings("unchecked")
    public static void register() {
        for (var definition : S2CPackets.getDefinitions()) {
            PayloadTypeRegistry.playS2C().register(
                    (CustomPacketPayload.Type<CustomPacketPayload>) definition.type(),
                    (StreamCodec<FriendlyByteBuf, CustomPacketPayload>) definition.codec());
        }

        for (var definition : CommonPackets.getDefinitions()) {
            PayloadTypeRegistry.playS2C().register(
                    (CustomPacketPayload.Type<CustomPacketPayload>) definition.type(),
                    (StreamCodec<FriendlyByteBuf, CustomPacketPayload>) definition.codec());
        }
    }
}
