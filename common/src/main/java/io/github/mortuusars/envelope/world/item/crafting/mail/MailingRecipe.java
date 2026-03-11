package io.github.mortuusars.envelope.world.item.crafting.mail;

import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.world.item.component.PackageContents;
import io.github.mortuusars.envelope.world.mail.address.Address;
import io.github.mortuusars.envelope.world.mail.address.type.EntityAddress;
import net.minecraft.core.HolderLookup;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

public interface MailingRecipe extends Recipe<PackageContents> {
    EntityAddress getAddress();
    CraftingResult craft(PackageContents input, Address sender, ServerLevel level);

    default boolean onlyOneCraftPerDelivery() {
        return false;
    }

    @Override
    default @NotNull RecipeType<?> getType() {
        return Envelope.RecipeTypes.MAILING.get();
    }

    @Override
    default boolean isSpecial() {
        return true;
    }

    @Override
    default @NotNull ItemStack getToastSymbol() {
        return new ItemStack(Envelope.Blocks.MAILBOX.get());
    }

    @Override
    default boolean canCraftInDimensions(int width, int height) {
        return width * height >= PackageContents.SLOTS;
    }

    @Override
    default boolean matches(PackageContents input, Level level) {
        List<ItemStack> inputItems = input.getItems().stream().filter(stack -> !stack.isEmpty()).toList();

        BitSet matchedSlots = new BitSet();

        for (Ingredient ingredient : getIngredients()) {
            for (int slot = 0; slot < inputItems.size(); slot++) {
                if (matchedSlots.get(slot)) {
                    continue;
                }

                ItemStack stack = inputItems.get(slot);

                if (ingredient.test(stack)) {
                    matchedSlots.set(slot);
                    break;
                }
            }
        }

        int matchedCount = matchedSlots.cardinality();
        return inputItems.size() == matchedCount && getIngredients().size() == matchedCount;
    }

    @Override
    default @NotNull ItemStack assemble(PackageContents input, HolderLookup.Provider registries) {
        return getResultItem(registries).copy();
    }

    default PackageContents consumeInput(PackageContents input) {
        BitSet processedSlots = new BitSet();

        List<ItemStack> result = new ArrayList<>();

        for (Ingredient ingredient : getIngredients()) {
            for (int slot = 0; slot < input.size(); slot++) {
                if (processedSlots.get(slot)) {
                    continue;
                }

                ItemStack stack = input.getItem(slot);

                if (ingredient.test(stack)) {
                    result.add(stack.copyWithCount(stack.getCount() - 1));
                    processedSlots.set(slot);
                    break;
                }
            }
        }

        return new PackageContents(result);
    }

    default ItemStack createOutput(PackageContents input, Address sender, ServerLevel level) {
        return assemble(input, level.registryAccess());
    }

    record CraftingResult(PackageContents input, ItemStack output, int experience) {
        public CraftingResult(PackageContents input, ItemStack output) {
            this(input, output, 0);
        }
    }
}
