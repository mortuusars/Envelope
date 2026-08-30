package io.github.mortuusars.envelope.network.packet.serverbound;

import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.mortaar.network.packet.Packet;
import io.github.mortuusars.envelope.world.inventory.PackingMenu;
import io.github.mortuusars.envelope.world.mail.address.Address;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public record ServerboundPackingMenuPresetAddressPacket(Optional<Address> address) implements Packet {
    public static final ResourceLocation ID = Envelope.resource("packing_menu_preset_address");
    public static final Type<ServerboundPackingMenuPresetAddressPacket> TYPE = new Type<>(ID);

    public static final StreamCodec<RegistryFriendlyByteBuf, ServerboundPackingMenuPresetAddressPacket> STREAM_CODEC = StreamCodec.composite(
          ByteBufCodecs.optional(Address.STREAM_CODEC), ServerboundPackingMenuPresetAddressPacket::address,
          ServerboundPackingMenuPresetAddressPacket::new
    );

    @Override
    public @NotNull CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    @Override
    public boolean handle(PacketFlow direction, Player player) {
        if (!(player.containerMenu instanceof PackingMenu packingMenu)) {
            Envelope.LOGGER.error("Cannot handle {} packet: player {} does not have PackingMenu open.",
                  ID, player.getScoreboardName());
            return false;
        }

        packingMenu.presetAddress(address.orElse(null));

        return false;
    }
}
