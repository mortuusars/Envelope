package io.github.mortuusars.envelope.network.packet.clientbound;

import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.network.handler.ClientboundPacketsHandler;
import io.github.mortuusars.mortaar.network.packet.Packet;
import io.github.mortuusars.envelope.world.mail.address.AllAddresses;
import io.github.mortuusars.mortaar.network.codec.StreamCodecs;
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

public record ClientboundOpenMailboxPlacingScreenPacket(InteractionHand hand,
                                                        BlockHitResult hitResult,
                                                        AllAddresses knownAddresses) implements Packet {
    public static final ResourceLocation ID = Envelope.resource("open_mailbox_placing_screen");
    public static final Type<ClientboundOpenMailboxPlacingScreenPacket> TYPE = new Type<>(ID);

    public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundOpenMailboxPlacingScreenPacket> STREAM_CODEC = StreamCodec.composite(
          ByteBufCodecs.VAR_INT.map(i -> InteractionHand.values()[i], InteractionHand::ordinal), ClientboundOpenMailboxPlacingScreenPacket::hand,
          StreamCodecs.BLOCK_HIT_RESULT, ClientboundOpenMailboxPlacingScreenPacket::hitResult,
          AllAddresses.STREAM_CODEC, ClientboundOpenMailboxPlacingScreenPacket::knownAddresses,
          ClientboundOpenMailboxPlacingScreenPacket::new
    );

    @Override
    public @NotNull CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    @Override
    public boolean handle(PacketFlow direction, Player player) {
        ClientboundPacketsHandler.openMailboxPlacingScreen(this);
        return true;
    }
}
