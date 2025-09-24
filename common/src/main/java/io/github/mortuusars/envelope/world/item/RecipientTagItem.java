package io.github.mortuusars.envelope.world.item;

import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.mail.Address;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class RecipientTagItem extends Item {
    public RecipientTagItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        @Nullable Address recipient = stack.get(Envelope.DataComponents.MAIL_RECIPIENT);
        if (recipient != null) {
            tooltipComponents.add(Component.literal("✉ ").withStyle(ChatFormatting.GRAY).append(recipient.getDisplayName().withStyle(ChatFormatting.WHITE)));
        }
    }

    @Override
    public boolean overrideStackedOnOther(ItemStack stack, Slot slot, ClickAction action, Player player) {
        if (action != ClickAction.SECONDARY) return false;
        if (!slot.getItem().is(Envelope.Tags.Items.MAILABLE)) return false;
        @Nullable Address recipient = stack.get(Envelope.DataComponents.MAIL_RECIPIENT);
        if (recipient == null) return false;
        if (recipient.equals(slot.getItem().get(Envelope.DataComponents.MAIL_RECIPIENT))) return true;

        slot.getItem().set(Envelope.DataComponents.MAIL_RECIPIENT, recipient);
        stack.shrink(1);
        player.playSound(SoundEvents.ARMOR_EQUIP_GENERIC.value(), 1, 1);
        return true;
    }
}
