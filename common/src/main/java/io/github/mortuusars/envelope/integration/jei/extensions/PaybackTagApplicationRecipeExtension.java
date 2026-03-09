package io.github.mortuusars.envelope.integration.jei.extensions;

import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.world.inventory.StackIngredient;
import io.github.mortuusars.envelope.world.item.component.PaybackRequest;
import io.github.mortuusars.envelope.world.item.crafting.PaybackTagApplicationRecipe;
import io.github.mortuusars.envelope.world.item.mail.Mail;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.ICraftingGridHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.category.extensions.vanilla.crafting.ICraftingCategoryExtension;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.List;

public class PaybackTagApplicationRecipeExtension implements ICraftingCategoryExtension<PaybackTagApplicationRecipe> {
    @Override
    public void setRecipe(RecipeHolder<PaybackTagApplicationRecipe> holder, IRecipeLayoutBuilder builder, ICraftingGridHelper craftingGridHelper, IFocusGroup focuses) {
        List<ItemStack> mailableItems = BuiltInRegistries.ITEM.getTag(Envelope.Tags.Items.MAILABLE)
              .map(named -> named.stream().map(ItemStack::new).toList())
              .orElse(List.of());

        ItemStack tag = new ItemStack(Envelope.Items.PAYBACK_TAG.get());
        tag.set(Envelope.DataComponents.PAYBACK_TAG_CONTENTS, new PaybackRequest(List.of(StackIngredient.createDefault())));
        List<ItemStack> tagItems = List.of(tag);

        List<ItemStack> resultItems = mailableItems.stream()
              .map(stack -> {
                  ItemStack result = stack.copy();
                  Mail.setPaybackRequest(result, tag.get(Envelope.DataComponents.PAYBACK_TAG_CONTENTS));
                  return result;
              })
              .toList();

        craftingGridHelper.createAndSetInputs(builder, VanillaTypes.ITEM_STACK, List.of(mailableItems, tagItems), 0, 0);
        craftingGridHelper.createAndSetOutputs(builder, VanillaTypes.ITEM_STACK, resultItems);
    }
}
