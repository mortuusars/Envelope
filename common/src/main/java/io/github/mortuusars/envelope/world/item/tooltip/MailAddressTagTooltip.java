package io.github.mortuusars.envelope.world.item.tooltip;

import io.github.mortuusars.envelope.world.mail.address.Address;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import org.jetbrains.annotations.NotNull;

public record MailAddressTagTooltip(@NotNull Address address) implements TooltipComponent {
}
