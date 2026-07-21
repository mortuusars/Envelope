package io.github.mortuusars.envelope.network.packet.clientbound;

import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.network.handler.ClientPacketsHandler;
import io.github.mortuusars.envelope.network.packet.Packet;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public record OpenLetterBlockViewScreenS2CP(ItemStack letter, BlockPos pos) implements Packet {
    public static final ResourceLocation ID = Envelope.resource("open_letter_block_view_screen");
    public static final Type<OpenLetterBlockViewScreenS2CP> TYPE = new Type<>(ID);

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenLetterBlockViewScreenS2CP> STREAM_CODEC = StreamCodec.composite(
          ItemStack.STREAM_CODEC, OpenLetterBlockViewScreenS2CP::letter,
          BlockPos.STREAM_CODEC, OpenLetterBlockViewScreenS2CP::pos,
          OpenLetterBlockViewScreenS2CP::new
    );

    @Override
    public @NotNull CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    @Override
    public boolean handle(PacketFlow direction, Player player) {
        ClientPacketsHandler.openPlacedLetterViewScreen(this);
        return true;
    }
}
