package io.github.mortuusars.envelope.world.item.crafting;

import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.world.item.component.PackageContents;
import io.github.mortuusars.envelope.world.mail.address.type.EntityAddress;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface MailRecipe extends Recipe<PackageRecipeInput> {
    EntityAddress getAddress();
    float getExperience();
    int getIngredientsCount();
    List<ItemStack> getIngredientStacks(int index);
    List<ItemStack> consumeOnce(PackageRecipeInput input);

    @Override
    default @NotNull RecipeType<?> getType() {
        return Envelope.RecipeTypes.MAILING.get();
    }

    @Override
    default @NotNull ItemStack getToastSymbol() {
        return new ItemStack(Envelope.Blocks.MAILBOX.get());
    }

    @Override
    default boolean canCraftInDimensions(int width, int height) {
        return width * height >= PackageContents.SLOTS;
    }

    static int calculateExperiencePoints(float experience) {
        int points = Mth.floor(experience);
        float remainder = Mth.frac(experience);
        if (remainder != 0.0F && Math.random() < remainder) {
            points++;
        }
        return points;
    }
}
