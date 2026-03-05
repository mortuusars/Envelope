package io.github.mortuusars.envelope.world.item.crafting;

import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.world.item.component.PackageContents;
import io.github.mortuusars.envelope.world.mail.address.type.EntityAddress;
import net.minecraft.core.HolderLookup;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface MailRecipe extends Recipe<PackageRecipeInput> {
    EntityAddress getEntityAddress();
    float getExperience();
    int getIngredientsCount();
    List<ItemStack> getIngredientStacks(int index);
    List<ItemStack> consumeOnce(PackageRecipeInput input);

    @Override
    default @NotNull RecipeType<?> getType() {
        return Envelope.RecipeTypes.MAILING.get();
    }

    /**
     * Extended version of {@link Recipe#assemble(RecipeInput, HolderLookup.Provider)} to allow custom logic dependent on a level.
     * <br>
     * This method should only be used one time, to get the actual result of the craft, as implementations can mutate the state.
     */
    default @NotNull ItemStack assembleFinal(PackageRecipeInput input, Level level) {
        return assemble(input, level.registryAccess());
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
