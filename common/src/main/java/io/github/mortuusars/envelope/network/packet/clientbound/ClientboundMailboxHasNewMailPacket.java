package io.github.mortuusars.envelope.network.packet.clientbound;

import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.mortaar.network.packet.Packet;
import io.github.mortuusars.envelope.world.inventory.MailboxMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;

public class ClientboundMailboxHasNewMailPacket implements Packet {
    public static final ClientboundMailboxHasNewMailPacket INSTANCE = new ClientboundMailboxHasNewMailPacket();
    public static final ResourceLocation ID = Envelope.resource("mailbox_has_new_mail");
    public static final Type<ClientboundMailboxHasNewMailPacket> TYPE = new Type<>(ID);
    public static final StreamCodec<FriendlyByteBuf, ClientboundMailboxHasNewMailPacket> STREAM_CODEC = StreamCodec.unit(INSTANCE);
    private ClientboundMailboxHasNewMailPacket() {
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    @Override
    public boolean handle(PacketFlow flow, Player player) {
        if (!(player.containerMenu instanceof MailboxMenu menu)) {
            Envelope.LOGGER.error("Cannot handle '{}' packet: Player '{}' does not have MailboxMenu open.", ID, player);
            return false;
        }

        menu.setHasNewMail(true);
        player.level().playSound(player, player, SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.BLOCKS, 0.75f, 1f);
        return true;
    }
}

