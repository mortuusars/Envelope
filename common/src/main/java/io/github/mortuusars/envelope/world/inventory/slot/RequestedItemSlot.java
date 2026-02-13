package io.github.mortuusars.envelope.world.inventory.slot;

import io.github.mortuusars.envelope.world.inventory.StackIngredient;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class RequestedItemSlot extends Slot {
    private final StackIngredient ingredient;
//    private ItemStack preview = ItemStack.EMPTY;
//    private long previewLastChange = 0;
//    private int previewTagIndex = 0;

    public RequestedItemSlot(Container container, int slot, int x, int y, StackIngredient ingredient) {
        super(container, slot, x, y);
        this.ingredient = ingredient;
    }

    public StackIngredient getIngredient() {
        return ingredient;
    }

    public ItemStack getRequestedItemPreview() {
        return getIngredient().getRollingDisplayedStack();

//        if (Util.getMillis() - previewLastChange > 1500) {
//            Item item = getRequestedItem().item().map(
//                  tag -> {
//                      List<Item> items = BuiltInRegistries.ITEM.getTag(tag)
//                            .map(named -> named.stream()
//                                  .map(Holder::value)
//                                  .toList())
//                            .orElse(List.of(Items.BARRIER));
//
//                      if (items.isEmpty()) {
//                          items = List.of(Items.BARRIER);
//                      }
//
//                      if (previewTagIndex >= items.size()) {
//                          previewTagIndex = 0;
//                      }
//
//                      return items.get(previewTagIndex++);
//                  },
//                  Holder::value
//            );
//
//            previewLastChange = Util.getMillis();
//
//            preview = new ItemStack(item, sizeableIngredient.count());
//            preview.applyComponents(sizeableIngredient.components().asPatch());
//        }
//
//        return preview;
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return getIngredient().testWithoutCount(stack); // Ignore count to allow placing in parts.
    }

    @Override
    public int getMaxStackSize() {
        return getIngredient().count();
    }
}
