package io.github.mortuusars.envelope.network.packet.serverbound;

import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.network.packet.Packet;
import io.github.mortuusars.envelope.world.item.LetterItem;
import io.github.mortuusars.envelope.world.item.component.LetterContent;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public record LetterViewScreenClosedS2CP(int slot) implements Packet {
    public static final ResourceLocation ID = Envelope.resource("letter_view_screen_closed");
    public static final Type<LetterViewScreenClosedS2CP> TYPE = new Type<>(ID);

    public static final StreamCodec<RegistryFriendlyByteBuf, LetterViewScreenClosedS2CP> STREAM_CODEC = StreamCodec.composite(
          ByteBufCodecs.VAR_INT, LetterViewScreenClosedS2CP::slot,
          LetterViewScreenClosedS2CP::new
    );

    @Override
    public @NotNull CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    @Override
    public boolean handle(PacketFlow direction, Player player) {
        if (slot == -1) {
            return true;
        }

        ItemStack stack = player.getInventory().getItem(slot);

        if (stack.getItem() instanceof LetterItem) {
            LetterContent content = stack.getOrDefault(Envelope.DataComponents.LETTER_CONTENT, LetterContent.EMPTY)
                  .withUnfolded(false);

            if (content.isEmpty()) {
                stack.remove(Envelope.DataComponents.LETTER_CONTENT);
            } else {
                stack.set(Envelope.DataComponents.LETTER_CONTENT, content);
            }

            player.getCooldowns().addCooldown(stack.getItem(), 5);

            return true;
        } else {
            return false;
        }
    }
}
