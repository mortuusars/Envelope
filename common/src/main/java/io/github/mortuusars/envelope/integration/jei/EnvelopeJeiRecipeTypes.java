package io.github.mortuusars.envelope.integration.jei;

import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.world.item.crafting.mail.MailingRecipe;
import mezz.jei.api.recipe.RecipeType;
import net.minecraft.world.item.crafting.RecipeHolder;

public class EnvelopeJeiRecipeTypes {
    public static final RecipeType<RecipeHolder<MailingRecipe>> MAILING_RECIPE_TYPE =
          RecipeType.createFromVanilla(Envelope.RecipeTypes.MAILING.get());
}
