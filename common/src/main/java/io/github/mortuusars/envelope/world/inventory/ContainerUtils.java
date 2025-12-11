package io.github.mortuusars.envelope.world.inventory;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class ContainerUtils {
    public static Container compact(Container container, int maxSlots) {
        SimpleContainer compactedContainer = new SimpleContainer(maxSlots);
        for (int slot = 0; slot < Math.min(maxSlots, container.getContainerSize()); slot++) {
            ItemStack stack = container.getItem(slot);
            compactedContainer.addItem(stack);
        }
        return compactedContainer;
    }

    public static Container compact(Container container) {
        return compact(container, container.getContainerSize());
    }

    public static List<ItemStack> toList(Container container, int maxSlots) {
        List<ItemStack> items = new ArrayList<>();
        for (int i = 0; i < Math.min(container.getContainerSize(), maxSlots); i++) {
            items.add(container.getItem(i));
        }
        return items;
    }
}
