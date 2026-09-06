package io.github.mortuusars.envelope.world.inventory.tooltip;

import io.github.mortuusars.envelope.world.item.component.seal.SealSymbol;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.EitherHolder;

import java.util.Optional;

public record SealDieTooltip(Optional<EitherHolder<SealSymbol>> die) implements TooltipComponent {
}
