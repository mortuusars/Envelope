package io.github.mortuusars.envelope.network.packet.clientbound;

import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.network.packet.Packet;
import io.github.mortuusars.envelope.world.inventory.MailboxMenu;
import io.github.mortuusars.envelope.world.item.component.Id;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Updates the client when mail is removed from the storage by hopper or something.
 */
public record MailboxMenuMailRemovedS2CP(Id id) implements Packet {
    public static final ResourceLocation ID = Envelope.resource("mailbox_menu_mail_removed");
    public static final Type<MailboxMenuMailRemovedS2CP> TYPE = new Type<>(ID);

    public static final StreamCodec<RegistryFriendlyByteBuf, MailboxMenuMailRemovedS2CP> STREAM_CODEC = StreamCodec.composite(
          Id.STREAM_CODEC, MailboxMenuMailRemovedS2CP::id,
          MailboxMenuMailRemovedS2CP::new
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

        menu.onMailRemoved(id);
        return true;
    }
}
