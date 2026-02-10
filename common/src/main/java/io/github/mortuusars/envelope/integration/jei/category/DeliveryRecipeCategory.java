package io.github.mortuusars.envelope.integration.jei.category;

import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.client.util.Minecrft;
import io.github.mortuusars.envelope.integration.jei.EnvelopeJeiRecipeTypes;
import io.github.mortuusars.envelope.world.inventory.recipe.DeliveryRecipe;
import io.github.mortuusars.envelope.world.mail.address.Address;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IJeiHelpers;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.category.AbstractRecipeCategory;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;

public class DeliveryRecipeCategory extends AbstractRecipeCategory<RecipeHolder<DeliveryRecipe>> {
    private final IDrawable background;

    public DeliveryRecipeCategory(IJeiHelpers helper) {
        super(EnvelopeJeiRecipeTypes.DELIVERY_RECIPE_TYPE,
              Component.translatable("envelope.jei.category.delivery"),
              helper.getGuiHelper().createDrawableItemLike(Envelope.Items.PACKAGE.get()), 148, 74);
        background = helper.getGuiHelper().createDrawable(
              Envelope.resource("textures/gui/jei/delivery_category.png"), 0, 0, getWidth(), getHeight());
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<DeliveryRecipe> recipeHolder, IFocusGroup focuses) {
        DeliveryRecipe recipe = recipeHolder.value();
        NonNullList<Ingredient> ingredients = recipe.getIngredients();

        for (int row = 0; row < 2; row++) {
            for (int column = 0; column < 3; column++) {
                int index = column + row * 3;

                int xPos = 18;
                int yPos = 24;

                if (index >= ingredients.size()) continue;

                builder.addSlot(RecipeIngredientRole.INPUT, xPos + column * 18, yPos + row * 18)
                      .addIngredients(ingredients.get(index));
            }
        }

        builder.addSlot(RecipeIngredientRole.OUTPUT, 126, 33)
              .addItemStack(recipe.getResultItem(Minecrft.registryAccess()));
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, RecipeHolder<DeliveryRecipe> recipeHolder, IRecipeSlotsView recipeSlotsView, IFocusGroup focuses) {
        builder.addDrawable(background, 0, 0);

        Address.Realized address = recipeHolder.value().getAddress().realize(Minecrft.registryAccess());
        MutableComponent component = address.format().withIcon().toComponent();

        builder.addText(component,
                    0,
                    0, getWidth(),
                    12)
              .alignHorizontalCenter()
              .setColor(0xFF808080);
    }
}
