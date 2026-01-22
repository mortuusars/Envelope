package io.github.mortuusars.envelope.network.packet.clientbound;

import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.world.mail.address.Address;
import io.github.mortuusars.envelope.world.mail.address.AllAddresses;
import io.github.mortuusars.envelope.network.handler.ClientPacketsHandler;
import io.github.mortuusars.envelope.network.packet.Packet;
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

public record OpenMailboxAddressTagScreenS2CP(InteractionHand hand,
                                              AllAddresses knownAddresses,
                                              BlockPos pos,
                                              Address.Block currentAddress) implements Packet {
    public static final ResourceLocation ID = Envelope.resource("open_mailbox_address_tag_screen");
    public static final Type<OpenMailboxAddressTagScreenS2CP> TYPE = new Type<>(ID);

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenMailboxAddressTagScreenS2CP> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT.map(i -> InteractionHand.values()[i], InteractionHand::ordinal), OpenMailboxAddressTagScreenS2CP::hand,
            AllAddresses.STREAM_CODEC, OpenMailboxAddressTagScreenS2CP::knownAddresses,
            BlockPos.STREAM_CODEC, OpenMailboxAddressTagScreenS2CP::pos,
            Address.Block.STREAM_CODEC, OpenMailboxAddressTagScreenS2CP::currentAddress,
            OpenMailboxAddressTagScreenS2CP::new
    );

    @Override
    public @NotNull CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    @Override
    public boolean handle(PacketFlow direction, Player player) {
        ClientPacketsHandler.openMailboxAddressTagScreen(this);
        return true;
    }
}
