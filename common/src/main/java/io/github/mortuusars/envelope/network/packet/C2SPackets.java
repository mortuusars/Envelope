package io.github.mortuusars.envelope.network.packet;

import io.github.mortuusars.envelope.network.packet.serverbound.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.List;

public class C2SPackets {
    public static List<CustomPacketPayload.TypeAndCodec<? extends FriendlyByteBuf, ? extends CustomPacketPayload>> getDefinitions() {
        return List.of(
                 new CustomPacketPayload.TypeAndCodec<>(MailboxMenuInboxActionC2SP.TYPE, MailboxMenuInboxActionC2SP.STREAM_CODEC),
                 new CustomPacketPayload.TypeAndCodec<>(LetterEditC2SP.TYPE, LetterEditC2SP.STREAM_CODEC),
                 new CustomPacketPayload.TypeAndCodec<>(LetterViewScreenClosedS2CP.TYPE, LetterViewScreenClosedS2CP.STREAM_CODEC),
                 new CustomPacketPayload.TypeAndCodec<>(AddressTagApplyC2SP.TYPE, AddressTagApplyC2SP.STREAM_CODEC),
                 new CustomPacketPayload.TypeAndCodec<>(MailboxPlaceC2SP.TYPE, MailboxPlaceC2SP.STREAM_CODEC),
                 new CustomPacketPayload.TypeAndCodec<>(MailboxAddressTagApplyC2SP.TYPE, MailboxAddressTagApplyC2SP.STREAM_CODEC)
        );
    }
}