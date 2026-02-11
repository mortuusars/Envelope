package io.github.mortuusars.envelope.integration.jei.category;

import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.client.util.Minecrft;
import io.github.mortuusars.envelope.integration.jei.EnvelopeJeiRecipeTypes;
import io.github.mortuusars.envelope.world.inventory.recipe.MailDeliveryRecipe;
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
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;

public class MailDeliveryRecipeCategory extends AbstractRecipeCategory<RecipeHolder<MailDeliveryRecipe>> {
    private final IDrawable background;

    public MailDeliveryRecipeCategory(IJeiHelpers helper) {
        super(EnvelopeJeiRecipeTypes.MAIL_DELIVERY_RECIPE_TYPE,
              Component.translatable("envelope.jei.category.mail_delivery"),
              helper.getGuiHelper().createDrawableItemLike(Envelope.Items.PACKAGE.get()), 148, 74);
        background = helper.getGuiHelper().createDrawable(
              Envelope.resource("textures/gui/jei/category_mail_delivery.png"), 0, 0, getWidth(), getHeight());
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<MailDeliveryRecipe> recipeHolder, IFocusGroup focuses) {
        MailDeliveryRecipe recipe = recipeHolder.value();
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
    public void createRecipeExtras(IRecipeExtrasBuilder builder, RecipeHolder<MailDeliveryRecipe> recipeHolder, IRecipeSlotsView recipeSlotsView, IFocusGroup focuses) {
        builder.addDrawable(background, 0, 0);

        Component component = recipeHolder.value().getEntityAddress()
              .format()
              .withIcon()
              .toComponent();

        builder.addText(component,
                    0,
                    0, getWidth(),
                    12)
              .alignHorizontalCenter()
              .setColor(0xFF808080);
    }
}