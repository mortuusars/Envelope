package io.github.mortuusars.envelope.network.packet;

import io.github.mortuusars.envelope.network.packet.serverbound.AddressTagApplyC2SP;
import io.github.mortuusars.envelope.network.packet.serverbound.LetterEditC2SP;
import io.github.mortuusars.envelope.network.packet.serverbound.PigeonholeAddressTagApplyC2SP;
import io.github.mortuusars.envelope.network.packet.serverbound.PigeonholeMenuMailActionC2SP;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.List;

public class C2SPackets {
    public static List<CustomPacketPayload.TypeAndCodec<? extends FriendlyByteBuf, ? extends CustomPacketPayload>> getDefinitions() {
        return List.of(
                 new CustomPacketPayload.TypeAndCodec<>(PigeonholeMenuMailActionC2SP.TYPE, PigeonholeMenuMailActionC2SP.STREAM_CODEC),
                 new CustomPacketPayload.TypeAndCodec<>(LetterEditC2SP.TYPE, LetterEditC2SP.STREAM_CODEC),
                 new CustomPacketPayload.TypeAndCodec<>(AddressTagApplyC2SP.TYPE, AddressTagApplyC2SP.STREAM_CODEC),
                 new CustomPacketPayload.TypeAndCodec<>(PigeonholeAddressTagApplyC2SP.TYPE, PigeonholeAddressTagApplyC2SP.STREAM_CODEC)
        );
    }
}