package io.github.mortuusars.envelope.client.gui.tooltip;

import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.world.mail.address.AddressFormatter;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public class Tooltip {
    public static class Mail {
        public static void mailable(ItemStack stack, Consumer<Component> consumer) {
            if (!stack.is(Envelope.Tags.Items.MAILABLE)) {
                return;
            }

            @Nullable Component senderToRecipient = AddressFormatter.senderToRecipient(
                  stack.get(Envelope.DataComponents.MAIL_SENDER),
                  stack.get(Envelope.DataComponents.MAIL_RECIPIENT));

            if (senderToRecipient != null) {
                consumer.accept(senderToRecipient);
            }
        }
    }
}
