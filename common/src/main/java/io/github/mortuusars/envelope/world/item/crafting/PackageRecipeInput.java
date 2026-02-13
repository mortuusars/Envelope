package io.github.mortuusars.envelope.world.item.crafting;

import io.github.mortuusars.envelope.world.item.component.PackageContents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public interface PackageRecipeInput extends RecipeInput {
    default Stream<ItemStack> getItems() {
        return IntStream.range(0, size()).mapToObj(this::getItem);
    }

    static PackageRecipeInput of(List<ItemStack> items) {
        return new Simple(items);
    }

    static PackageRecipeInput of(PackageContents contents) {
        return of(contents.getItemsForReading());
    }

    record Simple(List<ItemStack> items) implements PackageRecipeInput {
        public Stream<ItemStack> getItems() {
            return items.stream();
        }

        @Override
        public @NotNull ItemStack getItem(int index) {
            return items.get(index);
        }

        @Override
        public int size() {
            return items.size();
        }
    }
}
