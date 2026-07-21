package io.github.mortuusars.envelope.network.packet;

import io.github.mortuusars.envelope.network.packet.clientbound.*;
import io.github.mortuusars.envelope.util.bugger.network.BuggerDataS2CP;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.List;

public class S2CPackets {
    public static List<CustomPacketPayload.TypeAndCodec<? extends FriendlyByteBuf, ? extends CustomPacketPayload>> getDefinitions() {
        return List.of(
                new CustomPacketPayload.TypeAndCodec<>(MailboxHasNewMailS2CP.TYPE, MailboxHasNewMailS2CP.STREAM_CODEC),
                new CustomPacketPayload.TypeAndCodec<>(MailboxMenuSetMailS2CP.TYPE, MailboxMenuSetMailS2CP.STREAM_CODEC),
                new CustomPacketPayload.TypeAndCodec<>(MailboxMenuMailRemovedS2CP.TYPE, MailboxMenuMailRemovedS2CP.STREAM_CODEC),
                new CustomPacketPayload.TypeAndCodec<>(OpenLetterEditScreenS2CP.TYPE, OpenLetterEditScreenS2CP.STREAM_CODEC),
                new CustomPacketPayload.TypeAndCodec<>(OpenLetterBlockViewScreenS2CP.TYPE, OpenLetterBlockViewScreenS2CP.STREAM_CODEC),
                new CustomPacketPayload.TypeAndCodec<>(OpenLetterViewScreenS2CP.TYPE, OpenLetterViewScreenS2CP.STREAM_CODEC),
                new CustomPacketPayload.TypeAndCodec<>(OpenAddressTagScreenS2CP.TYPE, OpenAddressTagScreenS2CP.STREAM_CODEC),
                new CustomPacketPayload.TypeAndCodec<>(OpenMailboxAddressTagScreenS2CP.TYPE, OpenMailboxAddressTagScreenS2CP.STREAM_CODEC),
                new CustomPacketPayload.TypeAndCodec<>(OpenMailboxPlacingScreenS2CP.TYPE, OpenMailboxPlacingScreenS2CP.STREAM_CODEC),

                new CustomPacketPayload.TypeAndCodec<>(BuggerDataS2CP.TYPE, BuggerDataS2CP.STREAM_CODEC)
        );
    }
}