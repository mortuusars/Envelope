package io.github.mortuusars.envelope.world.item.tooltip;

import net.minecraft.world.inventory.tooltip.TooltipComponent;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public record CompositeTooltip(List<TooltipComponent> components) implements TooltipComponent {
    @SafeVarargs
    public static Optional<TooltipComponent> of(Optional<TooltipComponent>... components) {
        if (components.length == 0) {
            return Optional.empty();
        }

        List<TooltipComponent> list = Arrays.stream(components).filter(Optional::isPresent).map(Optional::get).toList();
        if (list.isEmpty()) {
            return Optional.empty();
        }

        if (list.size() == 1) {
            return Optional.of(list.getFirst());
        }

        return Optional.of(new CompositeTooltip(list));
    }
}
