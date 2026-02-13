package io.github.mortuusars.envelope.world.item.crafting;

import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.world.item.component.PackageContents;
import io.github.mortuusars.envelope.world.mail.address.type.EntityAddress;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface MailRecipe extends Recipe<PackageRecipeInput> {
    EntityAddress getEntityAddress();
    int getIngredientsCount();
    List<ItemStack> getIngredientStacks(int index);
    List<ItemStack> consumeOnce(PackageRecipeInput input);

    @Override
    default @NotNull RecipeType<?> getType() {
        return Envelope.RecipeTypes.MAILING.get();
    }

    @Override
    default boolean canCraftInDimensions(int width, int height) {
        return width * height >= PackageContents.SLOTS;
    }
}
