package io.github.mortuusars.envelope.client.gui.tooltip;

import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.world.item.component.Payback;
import io.github.mortuusars.envelope.world.mail.address.Address;
import io.github.mortuusars.envelope.world.mail.address.AddressFormatter;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public class Tooltip {
    public static class Mail {
        public static void mailable(ItemStack stack, Consumer<Component> consumer) {
            if (!stack.is(Envelope.Tags.Items.MAILABLE)) {
                return;
            }

            @Nullable Address sender = stack.get(Envelope.DataComponents.MAIL_SENDER);
            @Nullable Address recipient = stack.get(Envelope.DataComponents.MAIL_RECIPIENT);

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

            @Nullable Payback payback = stack.get(Envelope.DataComponents.PAYBACK);
            if (payback != null) {
                consumer.accept(Component.literal("Payback:").withStyle(ChatFormatting.RED));
                payback.ingredients().forEach(i -> {
                    ItemStack[] items = i.getItems();
                    if (items.length > 0) {
                        int index = (int) (Util.getMillis() / 1000) % items.length;
                        ItemStack item = items[index];
                        MutableComponent name = Component.literal(" ").append(item.getHoverName());
                        if (item.getCount() > 1) {
                            name.append(" " + item.getCount() + "x");
                        }
                        consumer.accept(name.withStyle(ChatFormatting.DARK_RED));
                    }
                });
            }
        }
    }
}
