package io.github.mortuusars.envelope.network.packet.clientbound;

import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.network.packet.Packet;
import io.github.mortuusars.envelope.world.inventory.PigeonholeMenu;
import io.github.mortuusars.envelope.world.item.component.MailId;
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
public record PigeonholeMenuMailRemovedS2CP(MailId mailId) implements Packet {
    public static final ResourceLocation ID = Envelope.resource("pigeonhole_menu_mail_removed");
    public static final Type<PigeonholeMenuMailRemovedS2CP> TYPE = new Type<>(ID);

    public static final StreamCodec<RegistryFriendlyByteBuf, PigeonholeMenuMailRemovedS2CP> STREAM_CODEC = StreamCodec.composite(
            MailId.STREAM_CODEC, PigeonholeMenuMailRemovedS2CP::mailId,
            PigeonholeMenuMailRemovedS2CP::new
    );

    @Override
    public @NotNull CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    @Override
    public boolean handle(PacketFlow direction, Player player) {
        if (!(player.containerMenu instanceof PigeonholeMenu pigeonholeMenu)) {
            Envelope.LOGGER.error("Cannot handle '{}' packet: Player '{}' does not have PigeonholeMenu open.", ID, player);
            return false;
        }

        pigeonholeMenu.getMail().removeIf(mailId::matches);
        return true;
    }
}
