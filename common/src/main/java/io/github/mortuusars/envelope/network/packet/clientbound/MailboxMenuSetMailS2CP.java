package io.github.mortuusars.envelope.network.packet.clientbound;

import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.network.packet.Packet;
import io.github.mortuusars.envelope.world.inventory.MailboxMenu;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public record MailboxMenuSetMailS2CP(List<ItemStack> mail) implements Packet {
    public static final ResourceLocation ID = Envelope.resource("mailbox_menu_set_mail");
    public static final Type<MailboxMenuSetMailS2CP> TYPE = new Type<>(ID);

    public static final StreamCodec<RegistryFriendlyByteBuf, MailboxMenuSetMailS2CP> STREAM_CODEC = StreamCodec.composite(
            ItemStack.LIST_STREAM_CODEC, MailboxMenuSetMailS2CP::mail,
            MailboxMenuSetMailS2CP::new
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

        menu.setMail(mail);

        return true;
    }
}
