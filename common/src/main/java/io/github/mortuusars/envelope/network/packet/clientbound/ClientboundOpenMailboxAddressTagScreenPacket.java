package io.github.mortuusars.envelope.network.packet.clientbound;

import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.world.mail.address.AllAddresses;
import io.github.mortuusars.envelope.network.handler.ClientboundPacketsHandler;
import io.github.mortuusars.mortaar.network.packet.Packet;
import io.github.mortuusars.envelope.world.mail.address.type.BlockAddress;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;

public record ClientboundOpenMailboxAddressTagScreenPacket(InteractionHand hand,
                                                           AllAddresses knownAddresses,
                                                           BlockPos pos,
                                                           BlockAddress currentAddress) implements Packet {
    public static final ResourceLocation ID = Envelope.resource("open_mailbox_address_tag_screen");
    public static final Type<ClientboundOpenMailboxAddressTagScreenPacket> TYPE = new Type<>(ID);

    public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundOpenMailboxAddressTagScreenPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT.map(i -> InteractionHand.values()[i], InteractionHand::ordinal), ClientboundOpenMailboxAddressTagScreenPacket::hand,
            AllAddresses.STREAM_CODEC, ClientboundOpenMailboxAddressTagScreenPacket::knownAddresses,
            BlockPos.STREAM_CODEC, ClientboundOpenMailboxAddressTagScreenPacket::pos,
            BlockAddress.STREAM_CODEC, ClientboundOpenMailboxAddressTagScreenPacket::currentAddress,
            ClientboundOpenMailboxAddressTagScreenPacket::new
    );

    @Override
    public @NotNull CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    @Override
    public boolean handle(PacketFlow direction, Player player) {
        ClientboundPacketsHandler.openMailboxAddressTagScreen(this);
        return true;
    }
}
