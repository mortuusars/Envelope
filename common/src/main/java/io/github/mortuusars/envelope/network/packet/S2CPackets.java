package io.github.mortuusars.envelope.network.packet;

import io.github.mortuusars.envelope.network.packet.clientbound.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.List;

public class S2CPackets {
    public static List<CustomPacketPayload.TypeAndCodec<? extends FriendlyByteBuf, ? extends CustomPacketPayload>> getDefinitions() {
        return List.of(
                new CustomPacketPayload.TypeAndCodec<>(PigeonholeGuiSyncBlockDataS2CP.TYPE, PigeonholeGuiSyncBlockDataS2CP.STREAM_CODEC),
                new CustomPacketPayload.TypeAndCodec<>(PigeonholeHasNewMailS2CP.TYPE, PigeonholeHasNewMailS2CP.STREAM_CODEC),
                new CustomPacketPayload.TypeAndCodec<>(PigeonholeMenuMailS2CP.TYPE, PigeonholeMenuMailS2CP.STREAM_CODEC),
                new CustomPacketPayload.TypeAndCodec<>(PigeonholeMenuMailRemovedS2CP.TYPE, PigeonholeMenuMailRemovedS2CP.STREAM_CODEC),
                new CustomPacketPayload.TypeAndCodec<>(OpenLetterEditScreenS2CP.TYPE, OpenLetterEditScreenS2CP.STREAM_CODEC),
                new CustomPacketPayload.TypeAndCodec<>(OpenAddressTagScreenS2CP.TYPE, OpenAddressTagScreenS2CP.STREAM_CODEC),
                new CustomPacketPayload.TypeAndCodec<>(OpenPigeonholeAddressTagScreenS2CP.TYPE, OpenPigeonholeAddressTagScreenS2CP.STREAM_CODEC),

                new CustomPacketPayload.TypeAndCodec<>(BuggerPigeonDeliveryS2CP.TYPE, BuggerPigeonDeliveryS2CP.STREAM_CODEC)
        );
    }
}