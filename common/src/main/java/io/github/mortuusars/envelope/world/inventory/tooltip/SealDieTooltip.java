package io.github.mortuusars.envelope.world.inventory.tooltip;

import io.github.mortuusars.envelope.world.item.component.seal.SealSymbol;
import net.minecraft.core.Holder;
import net.minecraft.world.inventory.tooltip.TooltipComponent;

import java.util.Optional;

public record SealDieTooltip(Optional<Holder<SealSymbol>> impression) implements TooltipComponent {
}
