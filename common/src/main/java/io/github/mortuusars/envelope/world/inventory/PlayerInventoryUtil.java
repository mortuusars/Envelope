package io.github.mortuusars.envelope.world.inventory;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class PlayerInventoryUtil {
    public static boolean tryAddWholeStack(Player player, ItemStack stack) {
        Inventory inventory = player.getInventory();
        ItemStack insertedStack = stack.copy();

        for (int i = 0; i < inventory.items.size(); i++) {
            ItemStack slotStack = inventory.getItem(i);

            if (slotStack.isEmpty()) {
                int maxInsert = Math.min(inventory.getMaxStackSize(), insertedStack.getMaxStackSize());
                int insertedAmount = Math.min(insertedStack.getCount(), maxInsert);
                insertedStack.shrink(insertedAmount);
            } else if (ItemStack.isSameItemSameComponents(slotStack, insertedStack)) {
                int space = Math.min(inventory.getMaxStackSize(), slotStack.getMaxStackSize()) - slotStack.getCount();
                if (space > 0) {
                    int insertedAmount = Math.min(space, insertedStack.getCount());
                    insertedStack.shrink(insertedAmount);
                }
            }

            if (insertedStack.isEmpty()) { // Whole stack fits
                player.getInventory().add(stack);
                return true;
            }
        }

        return false; // Cannot fit whole stack
    }
}
