package io.github.mortuusars.envelope.network.packet.clientbound;

import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.world.mail.address.AllAddresses;
import io.github.mortuusars.envelope.network.handler.ClientboundPacketsHandler;
import io.github.mortuusars.mortaar.network.packet.Packet;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;

public record ClientboundOpenAddressTagScreenPacket(InteractionHand hand, AllAddresses knownAddresses) implements Packet {
    public static final ResourceLocation ID = Envelope.resource("open_address_tag_screen");
    public static final Type<ClientboundOpenAddressTagScreenPacket> TYPE = new Type<>(ID);

    public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundOpenAddressTagScreenPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT.map(i -> InteractionHand.values()[i], InteractionHand::ordinal), ClientboundOpenAddressTagScreenPacket::hand,
            AllAddresses.STREAM_CODEC, ClientboundOpenAddressTagScreenPacket::knownAddresses,
            ClientboundOpenAddressTagScreenPacket::new
    );

    @Override
    public @NotNull CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    @Override
    public boolean handle(PacketFlow direction, Player player) {
        ClientboundPacketsHandler.openAddressTagScreen(this);
        return true;
    }
}
