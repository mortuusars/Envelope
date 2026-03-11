package io.github.mortuusars.envelope.world.inventory;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class ContainerUtils {
    public static Container compact(Container container, int maxSlots) {
        SimpleContainer compactedContainer = new SimpleContainer(maxSlots);
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            compactedContainer.addItem(container.getItem(slot));
        }
        return compactedContainer;
    }

    public static Container compact(Container container) {
        return compact(container, container.getContainerSize());
    }

    public static Container compact(List<ItemStack> items, int maxSlots) {
        SimpleContainer compactedContainer = new SimpleContainer(maxSlots);
        for (ItemStack item : items) {
            compactedContainer.addItem(item);
        }
        return compactedContainer;
    }

    public static Container compact(List<ItemStack> items) {
        SimpleContainer compactedContainer = new SimpleContainer(items.size());
        for (ItemStack item : items) {
            compactedContainer.addItem(item);
        }
        return compactedContainer;
    }

    public static List<Container> split(Container container, int slots) {
        List<Container> containers = new ArrayList<>();

        SimpleContainer currentContainer = new SimpleContainer(slots);

        for (int i = 0; i < container.getContainerSize(); i += slots) {
            for (int slot = 0; slot < slots; slot++) {
                currentContainer.setItem(slot, container.getItem(i + slot));
            }

            if (!currentContainer.isEmpty()) {
                containers.add(currentContainer);
                currentContainer = new SimpleContainer(slots);
            }
        }

        return containers;
    }

    public static List<ItemStack> toList(Container container, int maxSlots) {
        List<ItemStack> items = new ArrayList<>();
        for (int i = 0; i < Math.min(container.getContainerSize(), maxSlots); i++) {
            items.add(container.getItem(i));
        }
        return items;
    }
}
