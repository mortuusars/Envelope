package io.github.mortuusars.envelope.network.packet.serverbound;

import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.mortaar.network.packet.Packet;
import io.github.mortuusars.envelope.world.block.mailbox.MailboxBlock;
import io.github.mortuusars.envelope.world.item.AddressTagItem;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

public record ServerboundMailboxAddressTagApplyPacket(InteractionHand hand, String addressId, BlockPos pos) implements Packet {
    public static final ResourceLocation ID = Envelope.resource("mailbox_address_tag_apply");
    public static final Type<ServerboundMailboxAddressTagApplyPacket> TYPE = new Type<>(ID);

    public static final StreamCodec<RegistryFriendlyByteBuf, ServerboundMailboxAddressTagApplyPacket> STREAM_CODEC = StreamCodec.composite(
          ByteBufCodecs.VAR_INT.map(i -> InteractionHand.values()[i], InteractionHand::ordinal), ServerboundMailboxAddressTagApplyPacket::hand,
          ByteBufCodecs.STRING_UTF8, ServerboundMailboxAddressTagApplyPacket::addressId,
          BlockPos.STREAM_CODEC, ServerboundMailboxAddressTagApplyPacket::pos,
          ServerboundMailboxAddressTagApplyPacket::new
    );

    @Override
    public @NotNull CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    @Override
    public boolean handle(PacketFlow direction, Player player) {
        ItemStack tag = player.getItemInHand(hand);

        if (!(tag.getItem() instanceof AddressTagItem)) {
            Envelope.LOGGER.error("Cannot handle {} packet: item in hand is not an AddressTagItem, but {}.", ID, tag);
            return false;
        }

        BlockState state = player.level().getBlockState(pos);
        if (!(state.getBlock() instanceof MailboxBlock block)) {
            Envelope.LOGGER.error("Cannot handle {} packet: block at pos [{}] is not a MailboxBlock", ID, pos);
            return false;
        }

        block.changeAddress(player, pos, hand, addressId);
        return true;
    }
}
