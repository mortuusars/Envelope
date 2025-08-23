package io.github.mortuusars.envelope.world.item;

import io.github.mortuusars.envelope.mail.Address;
import io.github.mortuusars.envelope.mail.log.MailTravelingLog;
import io.github.mortuusars.envelope.mail.log.TravelingRecord;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface MailItem {
    boolean canSend(ItemStack stack);
    @Nullable Address getRecipient(ItemStack stack);
    /**
     * 'envelope:recipient' component can be changed or removed from the stack in the traveling process.
     * Due to this, items that specify a recipient (such as Letter or Package) should update 'envelope:recipient' component with proper value.
     * Otherwise, players would not be able to send the same item more than once without manually updating the recipient through GUI.
     * -
     * This should be done only once (when sending new mail), or it'll mess up traveling process (if mail is returning for example)
     */
    void updateRecipientBeforeNewSendIfNeeded(ItemStack mail);

    // --

    default void appendTravelingLogToTooltip(ItemStack stack, Item.TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        MailTravelingLog log = MailTravelingLog.of(stack);

        if (!log.isEmpty()) {
            if (Screen.hasShiftDown()) {
                tooltipComponents.add(Component.translatable("gui.envelope.mail.log"));
                for (TravelingRecord record : log.records()) {
                    tooltipComponents.add(record.translate());
                }
            } else {
                tooltipComponents.add(Component.translatable("gui.envelope.mail.log.show_tooltip"));
            }

        }
    }
}
