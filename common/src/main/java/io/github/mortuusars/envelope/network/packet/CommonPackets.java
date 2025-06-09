package io.github.mortuusars.envelope.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.List;

public class CommonPackets {
    public static List<CustomPacketPayload.TypeAndCodec<? extends FriendlyByteBuf, ? extends CustomPacketPayload>> getDefinitions() {
        return List.of();
    }
}
