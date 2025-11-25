package io.github.mortuusars.envelope.world.inventory.tooltip;

import io.github.mortuusars.envelope.world.item.component.seal.SealImpression;
import net.minecraft.world.inventory.tooltip.TooltipComponent;

import java.util.Optional;

public record SealDieTooltipComponent(Optional<SealImpression> impression) implements TooltipComponent {
}
