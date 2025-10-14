package io.github.mortuusars.envelope.network.packet.clientbound;

import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.network.handler.ClientPacketsHandler;
import io.github.mortuusars.envelope.network.packet.Packet;
import io.github.mortuusars.envelope.world.block.PigeonholeBlockEntity;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public record PigeonholeGuiSyncBlockDataS2CP(List<PigeonholeBlockEntity.Occupant> occupants) implements Packet {
    public static final ResourceLocation ID = Envelope.resource("pigeonhole_gui_sync_block_data");
    public static final Type<PigeonholeGuiSyncBlockDataS2CP> TYPE = new Type<>(ID);

    public static final StreamCodec<RegistryFriendlyByteBuf, PigeonholeGuiSyncBlockDataS2CP> STREAM_CODEC = StreamCodec.composite(
            PigeonholeBlockEntity.Occupant.STREAM_CODEC.apply(ByteBufCodecs.list(3)), PigeonholeGuiSyncBlockDataS2CP::occupants,
            PigeonholeGuiSyncBlockDataS2CP::new
    );

    @Override
    public @NotNull CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    @Override
    public boolean handle(PacketFlow direction, Player player) {
        ClientPacketsHandler.syncPigeonholeGuiBlockData(this);
        return true;
    }
}
