package io.github.mortuusars.envelope.client.gui.tooltip;

import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.world.mail.address.Address;
import io.github.mortuusars.envelope.world.mail.address.AddressFormatter;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.function.Consumer;

public class Tooltip {
    public static class Mail {
        public static void mailable(ItemStack stack, Consumer<Component> consumer) {
            if (!stack.is(Envelope.Tags.Items.MAILABLE)) {
                return;
            }

            @Nullable Address sender = stack.get(Envelope.DataComponents.SENDER);
            @Nullable Address recipient = stack.get(Envelope.DataComponents.RECIPIENT);

            if (Screen.hasShiftDown()) {
                if (sender != null) {
                    consumer.accept(Component.translatable("gui.envelope.mail.sender").withStyle(ChatFormatting.GRAY)
                          .append(": ").withStyle(ChatFormatting.GRAY)
                          .append(sender.format().asSender().toComponent()));
                }
                if (recipient != null) {
                    consumer.accept(Component.translatable("gui.envelope.mail.recipient").withStyle(ChatFormatting.GRAY)
                          .append(": ").withStyle(ChatFormatting.GRAY)
                          .append(recipient.format().asRecipient().toComponent()));
                }
            } else {
                @Nullable Component senderToRecipient = AddressFormatter.senderToRecipient(sender, recipient);
                if (senderToRecipient != null) {
                    consumer.accept(senderToRecipient);
                }
            }
        }

        public static Optional<TooltipComponent> appendComponents(ItemStack stack, Optional<TooltipComponent> original) {
            if (!stack.is(Envelope.Tags.Items.MAILABLE)) {
                return original;
            }

            return CompositeTooltipComponent.combine(
                  original,
                  Optional.ofNullable(stack.get(Envelope.DataComponents.PAYBACK))
            );
        }
    }
}
