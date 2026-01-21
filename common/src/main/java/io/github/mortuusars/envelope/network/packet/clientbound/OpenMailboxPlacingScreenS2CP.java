package io.github.mortuusars.envelope.network.packet.clientbound;

import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.network.handler.ClientPacketsHandler;
import io.github.mortuusars.envelope.network.packet.Packet;
import io.github.mortuusars.envelope.util.EnvelopeStreamCodecs;
import io.github.mortuusars.envelope.world.mail.address.AllAddresses;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;

public record OpenMailboxPlacingScreenS2CP(InteractionHand hand,
                                           BlockHitResult hitResult,
                                           AllAddresses knownAddresses) implements Packet {
    public static final ResourceLocation ID = Envelope.resource("open_mailbox_placing_screen");
    public static final Type<OpenMailboxPlacingScreenS2CP> TYPE = new Type<>(ID);

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenMailboxPlacingScreenS2CP> STREAM_CODEC = StreamCodec.composite(
          ByteBufCodecs.VAR_INT.map(i -> InteractionHand.values()[i], InteractionHand::ordinal), OpenMailboxPlacingScreenS2CP::hand,
          EnvelopeStreamCodecs.BLOCK_HIT_RESULT, OpenMailboxPlacingScreenS2CP::hitResult,
          AllAddresses.STREAM_CODEC, OpenMailboxPlacingScreenS2CP::knownAddresses,
          OpenMailboxPlacingScreenS2CP::new
    );

    @Override
    public @NotNull CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    @Override
    public boolean handle(PacketFlow direction, Player player) {
        ClientPacketsHandler.openMailboxPlacingScreen(this);
        return true;
    }
}
