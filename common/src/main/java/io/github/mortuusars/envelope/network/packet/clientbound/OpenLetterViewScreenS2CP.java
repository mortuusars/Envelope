package io.github.mortuusars.envelope.network.packet.clientbound;

import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.network.handler.ClientPacketsHandler;
import io.github.mortuusars.envelope.network.packet.Packet;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;

public record OpenLetterViewScreenS2CP(InteractionHand hand) implements Packet {
    public static final ResourceLocation ID = Envelope.resource("open_letter_view_screen");
    public static final Type<OpenLetterViewScreenS2CP> TYPE = new Type<>(ID);

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenLetterViewScreenS2CP> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT.map(i -> InteractionHand.values()[i], InteractionHand::ordinal), OpenLetterViewScreenS2CP::hand,
            OpenLetterViewScreenS2CP::new
    );

    @Override
    public @NotNull CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    @Override
    public boolean handle(PacketFlow direction, Player player) {
        ClientPacketsHandler.openLetterViewScreen(this);
        return true;
    }
}
