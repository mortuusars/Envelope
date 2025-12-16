package io.github.mortuusars.envelope.world.item.tooltip;

import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.world.mail.address.Address;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public record MailAddressTooltip(@Nullable Address sender, @Nullable Address recipient) implements TooltipComponent {
    public static MailAddressTooltip of(ItemStack stack) {
        Address sender = stack.get(Envelope.DataComponents.SENDER);
        Address recipient = stack.get(Envelope.DataComponents.RECIPIENT);
        if (sender == null && recipient == null) return null;
        return new MailAddressTooltip(sender, recipient);
    }
}
