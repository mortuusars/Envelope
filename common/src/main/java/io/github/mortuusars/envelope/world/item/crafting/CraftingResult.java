package io.github.mortuusars.envelope.world.item.crafting;

import net.minecraft.world.item.ItemStack;

import java.util.List;

public record CraftingResult(PackageRecipeInput remainingInput, List<ItemStack> output) {
    public boolean hasRemainder() {
        return !remainingInput().isEmpty();
    }
}