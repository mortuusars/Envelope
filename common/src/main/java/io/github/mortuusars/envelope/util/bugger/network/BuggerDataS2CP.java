package io.github.mortuusars.envelope.util.bugger.network;

import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.network.packet.Packet;
import io.github.mortuusars.envelope.util.bugger.BuggerData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;

public record BuggerDataS2CP(ResourceLocation id, CompoundTag data) implements Packet {
    public static final ResourceLocation ID = Envelope.resource("bugger_data");
    public static final Type<BuggerDataS2CP> TYPE = new Type<>(ID);

    public static final StreamCodec<RegistryFriendlyByteBuf, BuggerDataS2CP> STREAM_CODEC = StreamCodec.composite(
          ResourceLocation.STREAM_CODEC, BuggerDataS2CP::id,
          ByteBufCodecs.COMPOUND_TAG, BuggerDataS2CP::data,
          BuggerDataS2CP::new
    );

    @Override
    public @NotNull CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    @Override
    public boolean handle(PacketFlow direction, Player player) {
        BuggerData.receive(id, data, player.registryAccess());
        return true;
    }
}
