package io.github.mortuusars.envelope.network.packet;

import io.github.mortuusars.envelope.network.packet.clientbound.MailboxHasNewMailS2CP;
import io.github.mortuusars.envelope.network.packet.clientbound.MailboxMenuMailS2CP;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.List;

public class S2CPackets {
    public static List<CustomPacketPayload.TypeAndCodec<? extends FriendlyByteBuf, ? extends CustomPacketPayload>> getDefinitions() {
        return List.of(
                new CustomPacketPayload.TypeAndCodec<>(MailboxHasNewMailS2CP.TYPE, MailboxHasNewMailS2CP.STREAM_CODEC),
                new CustomPacketPayload.TypeAndCodec<>(MailboxMenuMailS2CP.TYPE, MailboxMenuMailS2CP.STREAM_CODEC)
        );
    }
}