package io.github.mortuusars.envelope.world.item.crafting;

import io.github.mortuusars.envelope.world.inventory.StackIngredient;
import io.github.mortuusars.envelope.world.mail.address.type.EntityAddress;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Deprecated()
public class MailTransformingRecipe extends MailCraftingRecipe {
    private final StackIngredient base;

    public MailTransformingRecipe(EntityAddress address, StackIngredient base, List<StackIngredient> ingredients, ItemStack result, int experience) {
        super(address, ingredients, result, experience);
        this.base = base;
    }

    public StackIngredient getBaseIngredient() {
        return base;
    }

    @Override
    public int getIngredientsCount() {
        return 1 + super.getIngredientsCount();
    }

    @Override
    public List<ItemStack> getIngredientStacks(int index) {
        if (index == 0) {
            return Arrays.asList(base.stacks());
        }
        return super.getIngredientStacks(index - 1);
    }

    public Optional<ItemStack> getInputMatchingBase(PackageRecipeInput input) {
        return input.getItems()
              .filter(getBaseIngredient()::test)
              .findAny();
    }

    @Override
    public @NotNull ItemStack assemble(PackageRecipeInput input, HolderLookup.Provider registries) {
        return getInputMatchingBase(input)
              .map(stack -> {
                  stack = stack.transmuteCopy(getResult().getItem(), getResult().getCount());
                  stack.applyComponents(getResult().getComponentsPatch());
                  return stack;
              })
              .orElseGet(() -> super.assemble(input, registries));
    }
}