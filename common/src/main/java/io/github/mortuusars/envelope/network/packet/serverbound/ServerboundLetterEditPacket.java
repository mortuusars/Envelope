package io.github.mortuusars.envelope.network.packet.serverbound;

import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.mortaar.network.packet.Packet;
import io.github.mortuusars.envelope.world.item.LetterAndQuillItem;
import io.github.mortuusars.envelope.world.item.component.LetterAndQuillContent;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public record ServerboundLetterEditPacket(int slot, String text, boolean fold) implements Packet {
    public static final ResourceLocation ID = Envelope.resource("letter_edit");
    public static final Type<ServerboundLetterEditPacket> TYPE = new Type<>(ID);

    public static final StreamCodec<RegistryFriendlyByteBuf, ServerboundLetterEditPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, ServerboundLetterEditPacket::slot,
            ByteBufCodecs.stringUtf8(LetterAndQuillContent.MAX_LENGTH), ServerboundLetterEditPacket::text,
            ByteBufCodecs.BOOL, ServerboundLetterEditPacket::fold,
            ServerboundLetterEditPacket::new
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

        ItemStack letterAndQuill = player.getInventory().getItem(slot);
        if (!(letterAndQuill.getItem() instanceof LetterAndQuillItem)) {
            Envelope.LOGGER.error("Cannot handle {} packet: item in slot {} is not a LetterAndQuillItem.", ID, slot);
            return false;
        }

        LetterAndQuillContent content = new LetterAndQuillContent(text);

        if (fold) {
            ItemStack letter = new ItemStack(Envelope.Items.LETTER.get());
            if (!content.isEmpty()) {
                letter.set(Envelope.DataComponents.LETTER_CONTENT, content.toWritten());
            }

            player.level().playSound(null, player, Envelope.SoundEvents.PAPER_CRACKLE.get(), SoundSource.PLAYERS, 1, 1);
            player.getInventory().setItem(slot, letter);
            player.awardStat(Envelope.Stats.LETTERS_FOLDED.get());
        } else {
            if (!content.isEmpty()) {
                letterAndQuill.set(Envelope.DataComponents.LETTER_AND_QUILL_CONTENT, content);
            } else {
                letterAndQuill.remove(Envelope.DataComponents.LETTER_AND_QUILL_CONTENT);
            }
        }

        return true;
    }
}
