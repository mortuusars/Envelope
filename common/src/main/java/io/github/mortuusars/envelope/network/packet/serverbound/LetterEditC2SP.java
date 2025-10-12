package io.github.mortuusars.envelope.network.packet.serverbound;

import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.network.packet.Packet;
import io.github.mortuusars.envelope.world.item.LetterItem;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public record LetterEditC2SP(int slot, String subject, String message) implements Packet {
    public static final ResourceLocation ID = Envelope.resource("letter_edit");
    public static final Type<LetterEditC2SP> TYPE = new Type<>(ID);

    public static final StreamCodec<RegistryFriendlyByteBuf, LetterEditC2SP> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, LetterEditC2SP::slot,
            ByteBufCodecs.stringUtf8(512), LetterEditC2SP::subject,
            ByteBufCodecs.stringUtf8(4096), LetterEditC2SP::message,
            LetterEditC2SP::new
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
        if (!(letter.getItem() instanceof LetterItem)) {
            Envelope.LOGGER.error("Cannot handle {} packet: item in slot {} is not a LetterItem.", ID, slot);
            return false;
        }

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
