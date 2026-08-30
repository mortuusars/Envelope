package io.github.mortuusars.envelope.network.packet.serverbound;

import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.mortaar.network.packet.Packet;
import io.github.mortuusars.envelope.world.block.mailbox.MailboxBlock;
import io.github.mortuusars.envelope.world.item.MailboxBlockItem;
import io.github.mortuusars.mortaar.network.codec.StreamCodecs;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;

public record ServerboundMailboxPlacePacket(InteractionHand hand, String address, BlockHitResult hitResult) implements Packet {
    public static final ResourceLocation ID = Envelope.resource("mailbox_place");
    public static final Type<ServerboundMailboxPlacePacket> TYPE = new Type<>(ID);

    public static final StreamCodec<FriendlyByteBuf, ServerboundMailboxPlacePacket> STREAM_CODEC = StreamCodec.composite(
          ByteBufCodecs.VAR_INT.map(i -> InteractionHand.values()[i], InteractionHand::ordinal), ServerboundMailboxPlacePacket::hand,
          ByteBufCodecs.STRING_UTF8, ServerboundMailboxPlacePacket::address,
          StreamCodecs.BLOCK_HIT_RESULT, ServerboundMailboxPlacePacket::hitResult,
          ServerboundMailboxPlacePacket::new
    );

    @Override
    public @NotNull CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    @Override
    public boolean handle(PacketFlow direction, Player player) {
        ItemStack mailbox = player.getItemInHand(hand);

        if (mailbox.isEmpty() || !(mailbox.getItem() instanceof MailboxBlockItem)) {
            Envelope.LOGGER.error("Cannot handle {} packet: {} is not a valid mailbox item.", ID, mailbox);
            return false;
        }

        BlockPlaceContext context = new BlockPlaceContext(player, hand, mailbox, hitResult);
        MailboxBlock.placeBlockWithAddress(player, hand, context, address);
        return true;
    }
}
