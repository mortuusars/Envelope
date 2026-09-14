package io.github.mortuusars.envelope.integration.jei.category;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.mortaar.client.Minecrft;
import io.github.mortuusars.envelope.integration.jei.EnvelopeJeiPlugin;
import io.github.mortuusars.envelope.integration.jei.EnvelopeJeiRecipeTypes;
import io.github.mortuusars.envelope.world.item.SealedItem;
import io.github.mortuusars.envelope.world.item.component.PackageContents;
import io.github.mortuusars.envelope.world.item.crafting.mail.MailRecipe;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.gui.inputs.IJeiInputHandler;
import mezz.jei.api.gui.inputs.IJeiUserInput;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IJeiHelpers;
import mezz.jei.api.recipe.IFocus;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.category.AbstractRecipeCategory;
import mezz.jei.common.gui.elements.HighResolutionDrawable;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;

public class MailingRecipeCategory extends AbstractRecipeCategory<RecipeHolder<MailRecipe>> {
    private final IDrawable background;
    private final IDrawable infoIcon;

    public MailingRecipeCategory(IJeiHelpers helpers) {
        super(EnvelopeJeiRecipeTypes.MAILING_RECIPE_TYPE,
              Component.translatable("envelope.jei.category.mailing"),
              helpers.getGuiHelper().createDrawableItemLike(Envelope.Items.PACKAGE.get()), 146, 74);
        background = helpers.getGuiHelper().createDrawable(
              Envelope.resource("textures/gui/jei/category_mailing.png"), 0, 0, getWidth(), getHeight());
        infoIcon = new HighResolutionDrawable(
              helpers.getGuiHelper().drawableBuilder(Envelope.resource("textures/gui/jei/info_icon.png"), 0, 0, 32, 32)
                    .setTextureSize(32, 32)
                    .build(), 4);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<MailRecipe> recipeHolder, IFocusGroup focuses) {
        MailRecipe recipe = recipeHolder.value();

        builder.addInvisibleIngredients(RecipeIngredientRole.CATALYST)
              .addItemLike(Envelope.Items.PAPER_BOX.get())
              .addItemLike(Envelope.Items.PACKAGE.get());

        builder.addInvisibleIngredients(RecipeIngredientRole.INPUT)
              .addIngredient(EnvelopeJeiPlugin.SERVICE_ADDRESS_INGREDIENT, recipe.getAddress());
        builder.addInvisibleIngredients(RecipeIngredientRole.OUTPUT)
              .addIngredient(EnvelopeJeiPlugin.SERVICE_ADDRESS_INGREDIENT, recipe.getAddress());

        for (int row = 0; row < 2; row++) {
            for (int column = 0; column < 3; column++) {
                int index = column + row * 3;

                int xPos = 18;
                int yPos = 24;

                if (index >= recipe.getIngredients().size()) continue;

                builder.addInputSlot(xPos + column * 18, yPos + row * 18)
                      .addIngredients(recipe.getIngredients().get(index));
            }
        }

        builder.addOutputSlot(122, 33)
              .addItemStack(recipe.getResultItem(Minecrft.registryAccess()));

        ItemStack resultItem = recipe.getResultItem(Minecrft.registryAccess());
        if (!(resultItem.getItem() instanceof SealedItem)) { // If not sealed
            PackageContents resultContents = PackageContents.of(resultItem);
            if (!resultContents.isEmpty()) {
                // Makes contents "known" to jei usages lookup:
                builder.addInvisibleIngredients(RecipeIngredientRole.OUTPUT)
                      .addItemStacks(resultContents.getItems());
            }
        }

        builder.setShapeless(getWidth() - 9, getHeight() - 9);
    }

    @Override
    public void draw(RecipeHolder<MailRecipe> recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        background.draw(guiGraphics, 0, 0);
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, RecipeHolder<MailRecipe> recipeHolder, IFocusGroup focuses) {
        builder.addRecipeArrow().setPosition(90, 32);

        builder.addWidget(new ServiceAddressRecipeWidget(new ScreenRectangle(0, 0, getWidth(), 9), recipeHolder.value().getAddress()));
        recipeHolder.value().getInfo().ifPresent(info ->
              builder.addWidget(new MailingRecipeInfoWidget(infoIcon, 126, 20, info)));

        builder.addInputHandler(new IJeiInputHandler() {
            @Override
            public ScreenRectangle getArea() {
                return new ScreenRectangle(0, 0, getWidth(), 9);
            }

            @Override
            public boolean handleInput(double mouseX, double mouseY, IJeiUserInput input) {
                if (EnvelopeJeiPlugin.runtime == null) {
                    return false;
                }

                if (input.getKey().getValue() != InputConstants.MOUSE_BUTTON_LEFT
                      && input.getKey().getValue() != InputConstants.MOUSE_BUTTON_RIGHT) {
                    return false;
                }

                if (input.isSimulate()) {
                    return true;
                }

                IFocus<?> focus = EnvelopeJeiPlugin.runtime
                      .getJeiHelpers()
                      .getFocusFactory()
                      .createFocus(
                            RecipeIngredientRole.INPUT,
                            EnvelopeJeiPlugin.SERVICE_ADDRESS_INGREDIENT,
                            recipeHolder.value().getAddress()
                      );

                EnvelopeJeiPlugin.runtime.getRecipesGui().show(focus);
                return true;
            }
        });
    }
}