package io.github.mortuusars.envelope.integration.jei;

import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.client.gui.screen.PackingScreen;
import io.github.mortuusars.envelope.client.gui.screen.PaybackTagScreen;
import io.github.mortuusars.envelope.client.util.Minecrft;
import io.github.mortuusars.envelope.integration.jei.category.MailingRecipeCategory;
import io.github.mortuusars.envelope.integration.jei.ingredient.EntityAddressIngredientHelper;
import io.github.mortuusars.envelope.integration.jei.ingredient.EntityAddressIngredientRenderer;
import io.github.mortuusars.envelope.integration.jei.util.InHandRecipeTransferInfo;
import io.github.mortuusars.envelope.integration.jei.util.PaybackTagGhostIngredientHandler;
import io.github.mortuusars.envelope.world.inventory.PackingMenu;
import io.github.mortuusars.envelope.world.item.component.PackageContents;
import io.github.mortuusars.envelope.world.item.crafting.MailRecipe;
import io.github.mortuusars.envelope.world.mail.address.type.EntityAddress;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.ingredients.IIngredientType;
import mezz.jei.api.registration.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@JeiPlugin
public class EnvelopeJeiPlugin implements IModPlugin {
    private static final ResourceLocation ID = Envelope.resource("jei_plugin");

    public static final IIngredientType<EntityAddress> ENTITY_ADDRESS_INGREDIENT = new IIngredientType<>() {
        @Override
        public @NotNull Class<? extends EntityAddress> getIngredientClass() {
            return EntityAddress.class;
        }
    };

    @Override
    public @NotNull ResourceLocation getPluginUid() {
        return ID;
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new MailingRecipeCategory(registration.getJeiHelpers()));
    }

    @Override
    public void registerIngredients(IModIngredientRegistration registration) {
        List<EntityAddress> addressesWithRecipes = Minecrft.level().getRecipeManager().getAllRecipesFor(Envelope.RecipeTypes.MAILING.get())
              .stream()
              .map(recipe -> recipe.value().getEntityAddress())
              .distinct()
              .filter(address -> !address.getEntityHolder().is(Envelope.Tags.MailEntities.HIDDEN))
              .toList();

        registration.register(ENTITY_ADDRESS_INGREDIENT,
              addressesWithRecipes,
              new EntityAddressIngredientHelper(),
              new EntityAddressIngredientRenderer(),
              EntityAddress.CODEC.codec());
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(new ItemStack(Envelope.Items.MAILBOX.get()), EnvelopeJeiRecipeTypes.MAILING_RECIPE_TYPE);
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        List<RecipeHolder<MailRecipe>> deliveryRecipes = Minecrft.level()
              .getRecipeManager()
              .getAllRecipesFor(Envelope.RecipeTypes.MAILING.get())
              .stream()
              .filter(recipe -> !recipe.value().getEntityAddress().getEntityHolder().is(Envelope.Tags.MailEntities.HIDDEN))
              .toList();

        registration.addRecipes(EnvelopeJeiRecipeTypes.MAILING_RECIPE_TYPE, deliveryRecipes);
    }

    @Override
    public void registerRecipeTransferHandlers(IRecipeTransferRegistration registration) {
        var transferInfo = new InHandRecipeTransferInfo<>(PackingMenu.class, EnvelopeJeiRecipeTypes.MAILING_RECIPE_TYPE,
              0, PackageContents.SLOTS, PackageContents.SLOTS, 36);
        registration.addRecipeTransferHandler(transferInfo);
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        registration.addRecipeClickArea(PackingScreen.class, 61, 17, 54, 15, EnvelopeJeiRecipeTypes.MAILING_RECIPE_TYPE);
        registration.addRecipeClickArea(PackingScreen.class, 45, 32, 16, 36, EnvelopeJeiRecipeTypes.MAILING_RECIPE_TYPE);
        registration.addRecipeClickArea(PackingScreen.class, 61, 68, 54, 13, EnvelopeJeiRecipeTypes.MAILING_RECIPE_TYPE);
        registration.addRecipeClickArea(PackingScreen.class, 115, 32, 11, 36, EnvelopeJeiRecipeTypes.MAILING_RECIPE_TYPE);

        registration.addGhostIngredientHandler(PaybackTagScreen.class, new PaybackTagGhostIngredientHandler());
    }
}