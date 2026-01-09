package io.github.mortuusars.envelope.network.packet.clientbound;

import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.network.packet.Packet;
import io.github.mortuusars.envelope.world.block.mailbox.Inbox;
import io.github.mortuusars.envelope.world.inventory.PigeonholeMenu;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;

public record PigeonholeMenuSetInboxS2CP(Inbox inbox) implements Packet {
    public static final ResourceLocation ID = Envelope.resource("pigeonhole_menu_set_inbox");
    public static final Type<PigeonholeMenuSetInboxS2CP> TYPE = new Type<>(ID);

    public static final StreamCodec<RegistryFriendlyByteBuf, PigeonholeMenuSetInboxS2CP> STREAM_CODEC = StreamCodec.composite(
            Inbox.STREAM_CODEC, PigeonholeMenuSetInboxS2CP::inbox,
            PigeonholeMenuSetInboxS2CP::new
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

        pigeonholeMenu.setInbox(inbox);

        return true;
    }
}
