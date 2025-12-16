package io.github.mortuusars.envelope.client.gui;

import io.github.mortuusars.envelope.world.inventory.RequestedItem;
import net.minecraft.Util;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class RequestedItemDisplay {
    private final RequestedItem requestedItem;

    public RequestedItemDisplay(RequestedItem requestedItem) {
        this.requestedItem = requestedItem;
    }

    public ItemStack getDisplayedItem() {
        List<Item> items = requestedItem.items();
        int index = (int)(Util.getMillis() / 1000) % items.size();

        Item item = items.get(index);
        ItemStack stack = new ItemStack(item, requestedItem.count());
        stack.applyComponents(requestedItem.components().asPatch());
        return stack;
    }
}
