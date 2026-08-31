package io.github.mortuusars.envelope.network.packet.serverbound;

import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.world.mail.address.Address;
import io.github.mortuusars.mortaar.network.packet.Packet;
import io.github.mortuusars.envelope.world.item.AddressTagItem;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public record ServerboundAddressTagApplyPacket(int slot, Optional<Address> address) implements Packet {
    public static final ResourceLocation ID = Envelope.resource("address_tag_apply");
    public static final Type<ServerboundAddressTagApplyPacket> TYPE = new Type<>(ID);

    public static final StreamCodec<RegistryFriendlyByteBuf, ServerboundAddressTagApplyPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, ServerboundAddressTagApplyPacket::slot,
            ByteBufCodecs.optional(Address.STREAM_CODEC), ServerboundAddressTagApplyPacket::address,
            ServerboundAddressTagApplyPacket::new
    );

    @Override
    public @NotNull CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    @Override
    public boolean handle(PacketFlow direction, Player player) {
        if (slot < 0 || slot > player.getInventory().items.size()) {
            Envelope.LOGGER.error("Cannot handle {} packet: slot {} is not in valid range (0 - {}).",
                    ID, slot, player.getInventory().items.size());
            return false;
        }

        ItemStack tag = player.getInventory().getItem(slot);

        if (tag.getItem() instanceof AddressTagItem) {
            address.ifPresentOrElse(
                    value -> tag.set(Envelope.DataComponents.ADDRESS_TAG_ADDRESS, value),
                    () -> tag.remove(Envelope.DataComponents.ADDRESS_TAG_ADDRESS));
            return true;
        }

        return false;
    }
}
