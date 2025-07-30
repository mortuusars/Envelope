package io.github.mortuusars.envelope.network.packet.serverbound;

import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.api.mail.Address;
import io.github.mortuusars.envelope.network.packet.Packet;
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

public record EditLetterC2SP(int slot, Optional<Address> recipient, String subject, String message) implements Packet {
    public static final ResourceLocation ID = Envelope.resource("edit_letter_packet");
    public static final Type<EditLetterC2SP> TYPE = new Type<>(ID);

    public static final StreamCodec<RegistryFriendlyByteBuf, EditLetterC2SP> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, EditLetterC2SP::slot,
            ByteBufCodecs.optional(Address.STREAM_CODEC), EditLetterC2SP::recipient,
            ByteBufCodecs.stringUtf8(512), EditLetterC2SP::subject,
            ByteBufCodecs.stringUtf8(4096), EditLetterC2SP::message,
            EditLetterC2SP::new
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

        ItemStack letter = player.getInventory().getItem(slot);

        recipient.ifPresentOrElse(
                value -> letter.set(Envelope.DataComponents.MAIL_RECIPIENT, value),
                () -> letter.remove(Envelope.DataComponents.MAIL_RECIPIENT));

        if (!subject.isBlank()) {
            letter.set(Envelope.DataComponents.LETTER_SUBJECT, subject);
        } else {
            letter.remove(Envelope.DataComponents.LETTER_SUBJECT);
        }

        if (!message.isBlank()) {
            letter.set(Envelope.DataComponents.LETTER_MESSAGE, message);
        } else {
            letter.remove(Envelope.DataComponents.LETTER_MESSAGE);
        }

        return true;
    }
}
