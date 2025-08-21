package io.github.mortuusars.envelope.network.packet;

import io.github.mortuusars.envelope.network.packet.serverbound.EditLetterC2SP;
import io.github.mortuusars.envelope.network.packet.serverbound.PigeonholeMenuMailActionC2SP;
import io.github.mortuusars.envelope.network.packet.serverbound.UpdatePigeonholeMenuAddressC2SP;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.List;

public class C2SPackets {
    public static List<CustomPacketPayload.TypeAndCodec<? extends FriendlyByteBuf, ? extends CustomPacketPayload>> getDefinitions() {
        return List.of(
                 new CustomPacketPayload.TypeAndCodec<>(PigeonholeMenuMailActionC2SP.TYPE, PigeonholeMenuMailActionC2SP.STREAM_CODEC),
                 new CustomPacketPayload.TypeAndCodec<>(EditLetterC2SP.TYPE, EditLetterC2SP.STREAM_CODEC),
                 new CustomPacketPayload.TypeAndCodec<>(UpdatePigeonholeMenuAddressC2SP.TYPE, UpdatePigeonholeMenuAddressC2SP.STREAM_CODEC)
        );
    }
}