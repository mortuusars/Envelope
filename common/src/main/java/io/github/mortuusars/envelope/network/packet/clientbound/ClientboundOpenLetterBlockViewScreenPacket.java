package io.github.mortuusars.envelope.network.packet.clientbound;

import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.network.handler.ClientboundPacketsHandler;
import io.github.mortuusars.mortaar.network.packet.Packet;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public record ClientboundOpenLetterBlockViewScreenPacket(ItemStack letter, BlockPos pos) implements Packet {
    public static final ResourceLocation ID = Envelope.resource("open_letter_block_view_screen");
    public static final Type<ClientboundOpenLetterBlockViewScreenPacket> TYPE = new Type<>(ID);

    public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundOpenLetterBlockViewScreenPacket> STREAM_CODEC = StreamCodec.composite(
          ItemStack.STREAM_CODEC, ClientboundOpenLetterBlockViewScreenPacket::letter,
          BlockPos.STREAM_CODEC, ClientboundOpenLetterBlockViewScreenPacket::pos,
          ClientboundOpenLetterBlockViewScreenPacket::new
    );

    @Override
    public @NotNull CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    @Override
    public boolean handle(PacketFlow direction, Player player) {
        ClientboundPacketsHandler.openPlacedLetterViewScreen(this);
        return true;
    }
}
