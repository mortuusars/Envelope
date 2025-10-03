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

public record OpenLetterEditScreenS2CP(InteractionHand hand) implements Packet {
    public static final ResourceLocation ID = Envelope.resource("open_letter_edit_screen");
    public static final Type<OpenLetterEditScreenS2CP> TYPE = new Type<>(ID);

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenLetterEditScreenS2CP> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT.map(i -> InteractionHand.values()[i], InteractionHand::ordinal), OpenLetterEditScreenS2CP::hand,
            OpenLetterEditScreenS2CP::new
    );

    @Override
    public @NotNull CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    @Override
    public boolean handle(PacketFlow direction, Player player) {
        ClientPacketsHandler.openLetterEditScreen(this);
        return true;
    }
}
