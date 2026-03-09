package io.github.mortuusars.envelope.integration.jei.extensions;

import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.world.item.crafting.LetterCloningRecipe;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.ICraftingGridHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.category.extensions.vanilla.crafting.ICraftingCategoryExtension;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.List;

public class LetterCloningRecipeExtension implements ICraftingCategoryExtension<LetterCloningRecipe> {
    @Override
    public void setRecipe(RecipeHolder<LetterCloningRecipe> holder, IRecipeLayoutBuilder builder, ICraftingGridHelper craftingGridHelper, IFocusGroup focuses) {
        List<List<ItemStack>> inputs = List.of(
              List.of(new ItemStack(Envelope.Items.LETTER.get())),
              List.of(new ItemStack(Envelope.Items.LETTER_AND_QUILL.get()))
        );

        List<ItemStack> outputs = List.of(new ItemStack(Envelope.Items.LETTER.get()));

        craftingGridHelper.createAndSetInputs(builder, VanillaTypes.ITEM_STACK, inputs, 0, 0);
        craftingGridHelper.createAndSetOutputs(builder, VanillaTypes.ITEM_STACK, outputs);
    }
}
