package io.github.mortuusars.envelope.world.inventory.slot;

import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.function.Predicate;

public class GhostSlot extends Slot {
    private final Predicate<ItemStack> filter;

    public GhostSlot(Container container, int slot, int x, int y, Predicate<ItemStack> filter) {
        super(container, slot, x, y);
        this.filter = filter;
    }

    /**
     * Due to other mods using {@link #mayPlace} to check if they can insert items into the slot - we check if the item is valid separately.
     */
    public boolean canContain(ItemStack stack) {
        return filter.test(stack);
    }

    @Override
    public boolean isFake() {
        return true;
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return false;
    }

    @Override
    public boolean mayPickup(Player player) {
        return false;
    }

    @Override
    public void onQuickCraft(ItemStack oldStack, ItemStack newStack) {
    }

    @Override
    public @NotNull ItemStack remove(int amount) {
        super.remove(amount);
        return ItemStack.EMPTY; // Returning original will dupe items in some cases
    }

    public void setCount(int count) {
        count = Mth.clamp(count, 1, getMaxStackSize(getItem()));
        if (getItem().getCount() != count) {
            setByPlayer(getItem().copyWithCount(count));
        }
    }
}
