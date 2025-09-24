package io.github.mortuusars.envelope.mail;

import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.mail.log.MailDeliveryLog;
import io.github.mortuusars.envelope.mail.log.TravelingRecord;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public class Mail {
    public static void addTooltip(ItemStack stack, Item.TooltipContext tooltipContext,
                                  TooltipFlag tooltipFlag, Player player, Consumer<Component> consumer) {
        if (!stack.is(Envelope.Tags.Items.MAILABLE)) {
            return;
        }

        @Nullable Address sender = stack.get(Envelope.DataComponents.MAIL_SENDER);
        @Nullable Address recipient = stack.get(Envelope.DataComponents.MAIL_RECIPIENT);

        if (sender != null) {
            if (recipient == null) {
                recipient = Address.UNKNOWN;
            }

            consumer.accept(Component.translatable("gui.envelope.mail.sender_and_recipient",
                    sender.getDisplayName(), recipient.getDisplayName()));
        } else if (recipient != null) {
            consumer.accept(Component.translatable("gui.envelope.mail.sender_and_recipient",
                    CommonComponents.EMPTY, recipient.getDisplayName()));
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