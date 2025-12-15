package io.github.mortuusars.envelope.world.inventory.slot;

import io.github.mortuusars.envelope.world.inventory.RequestedItem;
import net.minecraft.Util;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

public class RequestedItemSlot extends Slot {
    private final RequestedItem requestedItem;
    private ItemStack preview = ItemStack.EMPTY;
    private long previewLastChange = 0;
    private int previewTagIndex = 0;

    public RequestedItemSlot(Container container, int slot, int x, int y, RequestedItem requestedItem) {
        super(container, slot, x, y);
        this.requestedItem = requestedItem;
    }

    public RequestedItem getRequestedItem() {
        return requestedItem;
    }

    public ItemStack getRequestedItemPreview() {
        if (Util.getMillis() - previewLastChange > 1500) {
            Item item = getRequestedItem().item().map(
                  tag -> {
                      List<Item> items = BuiltInRegistries.ITEM.getTag(tag)
                            .map(named -> named.stream()
                                  .map(Holder::value)
                                  .toList())
                            .orElse(List.of(Items.BARRIER));

                      if (items.isEmpty()) {
                          items = List.of(Items.BARRIER);
                      }

                      if (previewTagIndex >= items.size()) {
                          previewTagIndex = 0;
                      }

                      return items.get(previewTagIndex++);
                  },
                  Holder::value
            );

            previewLastChange = Util.getMillis();

            preview = new ItemStack(item, requestedItem.count());
            preview.applyComponents(requestedItem.components().asPatch());
        }

        return preview;
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return requestedItem.typeMatches(stack) && requestedItem.componentsMatch(stack);
    }

    @Override
    public int getMaxStackSize() {
        return requestedItem.count();
    }
}
