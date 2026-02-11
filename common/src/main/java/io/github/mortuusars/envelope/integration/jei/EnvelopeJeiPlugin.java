package io.github.mortuusars.envelope.integration.jei;

import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.client.gui.screen.PackingScreen;
import io.github.mortuusars.envelope.client.util.Minecrft;
import io.github.mortuusars.envelope.integration.jei.category.MailDeliveryRecipeCategory;
import io.github.mortuusars.envelope.integration.jei.util.InHandRecipeTransferInfo;
import io.github.mortuusars.envelope.world.inventory.PackingMenu;
import io.github.mortuusars.envelope.world.inventory.recipe.MailDeliveryRecipe;
import io.github.mortuusars.envelope.world.item.component.PackageContents;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@JeiPlugin
public class EnvelopeJeiPlugin implements IModPlugin {
    private static final ResourceLocation ID = Envelope.resource("jei_plugin");

    @Override
    public @NotNull ResourceLocation getPluginUid() {
        return ID;
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new MailDeliveryRecipeCategory(registration.getJeiHelpers()));
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(new ItemStack(Envelope.Items.PAPER_BOX.get()), EnvelopeJeiRecipeTypes.MAIL_DELIVERY_RECIPE_TYPE);
        registration.addRecipeCatalyst(new ItemStack(Envelope.Items.PACKAGE.get()), EnvelopeJeiRecipeTypes.MAIL_DELIVERY_RECIPE_TYPE);
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        List<RecipeHolder<MailDeliveryRecipe>> deliveryRecipes = Minecrft.level()
              .getRecipeManager()
              .getAllRecipesFor(Envelope.RecipeTypes.MAIL_DELIVERY.get());

        registration.addRecipes(EnvelopeJeiRecipeTypes.MAIL_DELIVERY_RECIPE_TYPE, deliveryRecipes);
    }

    @Override
    public void registerRecipeTransferHandlers(IRecipeTransferRegistration registration) {
        var transferInfo = new InHandRecipeTransferInfo<>(PackingMenu.class, EnvelopeJeiRecipeTypes.MAIL_DELIVERY_RECIPE_TYPE,
              0, PackageContents.SLOTS, PackageContents.SLOTS, 36);
        registration.addRecipeTransferHandler(transferInfo);
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        registration.addRecipeClickArea(PackingScreen.class, 61, 17, 54, 15, EnvelopeJeiRecipeTypes.MAIL_DELIVERY_RECIPE_TYPE);
        registration.addRecipeClickArea(PackingScreen.class, 45, 32, 16, 36, EnvelopeJeiRecipeTypes.MAIL_DELIVERY_RECIPE_TYPE);
        registration.addRecipeClickArea(PackingScreen.class, 61, 68, 54, 13, EnvelopeJeiRecipeTypes.MAIL_DELIVERY_RECIPE_TYPE);
        registration.addRecipeClickArea(PackingScreen.class, 115, 32, 11, 36, EnvelopeJeiRecipeTypes.MAIL_DELIVERY_RECIPE_TYPE);
    }
}