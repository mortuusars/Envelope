package io.github.mortuusars.envelope.network.packet.serverbound;

import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.network.packet.Packet;
import io.github.mortuusars.envelope.world.inventory.MailboxMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;

public record MailboxMenuInboxActionC2SP(int index, MailboxMenu.MailAction action) implements Packet {
    public static final ResourceLocation ID = Envelope.resource("mailbox_menu_inbox_action");
    public static final CustomPacketPayload.Type<MailboxMenuInboxActionC2SP> TYPE = new CustomPacketPayload.Type<>(ID);

    public static final StreamCodec<FriendlyByteBuf, MailboxMenuInboxActionC2SP> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, MailboxMenuInboxActionC2SP::index,
            MailboxMenu.MailAction.STREAM_CODEC, MailboxMenuInboxActionC2SP::action,
            MailboxMenuInboxActionC2SP::new
    );

    @Override
    public @NotNull CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    @Override
    public boolean handle(PacketFlow direction, Player player) {
        if (!(player.containerMenu instanceof MailboxMenu menu)) {
            Envelope.LOGGER.error("Cannot handle '{}' packet: Player '{}' does not have MailboxMenu open.", ID, player);
            return false;
        }

        menu.doMailAction(player, index, action);

        return true;
    }
}
