package io.github.mortuusars.envelope.client.gui.tooltip;

import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.core.address.AddressDisplay;
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
        }
    }
}
