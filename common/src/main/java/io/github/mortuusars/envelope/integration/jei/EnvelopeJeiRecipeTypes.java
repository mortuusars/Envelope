package io.github.mortuusars.envelope.integration.jei;

import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.world.inventory.recipe.DeliveryRecipe;
import mezz.jei.api.recipe.RecipeType;
import net.minecraft.world.item.crafting.RecipeHolder;

public class EnvelopeJeiRecipeTypes {
    public static final RecipeType<RecipeHolder<DeliveryRecipe>> DELIVERY_RECIPE_TYPE =
          RecipeType.createFromVanilla(Envelope.RecipeTypes.DELIVERY.get());
}
