package io.github.mortuusars.envelope.client.gui.tooltip;

import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.mail.AddressDisplay;
import io.github.mortuusars.envelope.mail.log.MailDeliveryLog;
import io.github.mortuusars.envelope.mail.log.TravelingRecord;
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

            @Nullable MutableComponent senderToRecipient = AddressDisplay.createSenderToRecipient(
                    stack.get(Envelope.DataComponents.MAIL_SENDER),
                    stack.get(Envelope.DataComponents.MAIL_RECIPIENT));
            if (senderToRecipient != null) {
                consumer.accept(senderToRecipient);
            }

            @Nullable MailDeliveryLog log = stack.get(Envelope.DataComponents.MAIL_DELIVERY_LOG);
            if (log != null && !log.isEmpty()) {
                if (Screen.hasShiftDown()) {
                    consumer.accept(Component.translatable("gui.envelope.mail.log"));
                    for (TravelingRecord record : log.records()) {
                        consumer.accept(record.translate());
                    }
                } else {
                    consumer.accept(Component.translatable("gui.envelope.mail.log.show_tooltip"));
                }
            }
        }
    }
}
