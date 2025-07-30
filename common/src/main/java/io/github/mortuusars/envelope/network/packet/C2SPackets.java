package io.github.mortuusars.envelope.network.packet;

import io.github.mortuusars.envelope.network.packet.serverbound.EditLetterC2SP;
import io.github.mortuusars.envelope.network.packet.serverbound.MailboxMenuMailActionC2SP;
import io.github.mortuusars.envelope.network.packet.serverbound.UpdateMailboxAddressC2SP;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.List;

public class C2SPackets {
    public static List<CustomPacketPayload.TypeAndCodec<? extends FriendlyByteBuf, ? extends CustomPacketPayload>> getDefinitions() {
        return List.of(
                 new CustomPacketPayload.TypeAndCodec<>(MailboxMenuMailActionC2SP.TYPE, MailboxMenuMailActionC2SP.STREAM_CODEC),
                 new CustomPacketPayload.TypeAndCodec<>(EditLetterC2SP.TYPE, EditLetterC2SP.STREAM_CODEC),
                 new CustomPacketPayload.TypeAndCodec<>(UpdateMailboxAddressC2SP.TYPE, UpdateMailboxAddressC2SP.STREAM_CODEC)
        );
    }
}