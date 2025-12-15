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

import java.util.Optional;

public record OpenPigeonholeAddressTagScreenS2CP(InteractionHand hand,
                                                 AllAddresses knownAddresses,
                                                 BlockPos pos,
                                                 Optional<Address.Block> currentAddress) implements Packet {
    public static final ResourceLocation ID = Envelope.resource("open_pigeonhole_address_tag_screen");
    public static final Type<OpenPigeonholeAddressTagScreenS2CP> TYPE = new Type<>(ID);

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenPigeonholeAddressTagScreenS2CP> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT.map(i -> InteractionHand.values()[i], InteractionHand::ordinal), OpenPigeonholeAddressTagScreenS2CP::hand,
            AllAddresses.STREAM_CODEC, OpenPigeonholeAddressTagScreenS2CP::knownAddresses,
            BlockPos.STREAM_CODEC, OpenPigeonholeAddressTagScreenS2CP::pos,
            ByteBufCodecs.optional(Address.Block.STREAM_CODEC), OpenPigeonholeAddressTagScreenS2CP::currentAddress,
            OpenPigeonholeAddressTagScreenS2CP::new
    );

    @Override
    public @NotNull CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    @Override
    public boolean handle(PacketFlow direction, Player player) {
        ClientPacketsHandler.openPigeonholeAddressTagScreen(this);
        return true;
    }
}
