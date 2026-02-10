package io.github.mortuusars.envelope.integration.jei.util;

import io.github.mortuusars.envelope.world.inventory.AbstractInHandContainerMenu;
import io.github.mortuusars.envelope.world.inventory.slot.DisabledSlot;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.transfer.IRecipeTransferInfo;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Extended RecipeTransferInfo to skip disabled slots in player's inventory. JEI doesn't like unmodifiable slots by default.
 */
public class InHandRecipeTransferInfo<C extends AbstractInHandContainerMenu, R> implements IRecipeTransferInfo<C, R> {
    private final Class<? extends C> containerClass;
    private final RecipeType<R> recipeType;
    private final int recipeSlotStart;
    private final int recipeSlotCount;
    private final int inventorySlotStart;
    private final int inventorySlotCount;

    public InHandRecipeTransferInfo(Class<? extends C> containerClass, RecipeType<R> recipeType,
                                    int recipeSlotStart, int recipeSlotCount, int inventorySlotStart, int inventorySlotCount) {
        this.containerClass = containerClass;
        this.recipeType = recipeType;
        this.recipeSlotStart = recipeSlotStart;
        this.recipeSlotCount = recipeSlotCount;
        this.inventorySlotStart = inventorySlotStart;
        this.inventorySlotCount = inventorySlotCount;
    }

    @Override
    public @NotNull Class<? extends C> getContainerClass() {
        return containerClass;
    }

    @Override
    public @NotNull Optional<MenuType<C>> getMenuType() {
        return Optional.empty();
    }

    @Override
    public @NotNull RecipeType<R> getRecipeType() {
        return recipeType;
    }

    @Override
    public boolean canHandle(C container, R recipe) {
        return true;
    }

    @Override
    public @NotNull List<Slot> getRecipeSlots(C container, R recipe) {
        List<Slot> slots = new ArrayList<>();
        for (int i = recipeSlotStart; i < recipeSlotStart + recipeSlotCount; i++) {
            Slot slot = container.getSlot(i);
            slots.add(slot);
        }
        return slots;
    }

    @Override
    public @NotNull List<Slot> getInventorySlots(C container, R recipe) {
        List<Slot> slots = new ArrayList<>();
        for (int i = inventorySlotStart; i < inventorySlotStart + inventorySlotCount; i++) {
            Slot slot = container.getSlot(i);
            if (!(slot instanceof DisabledSlot)) { // Skip disabled slot to prevent the error.
                slots.add(slot);
            }
        }
        return slots;
    }
}
