package io.github.mortuusars.envelope.integration.jei;

import io.github.mortuusars.envelope.Config;
import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.client.gui.screen.MailboxScreen;
import io.github.mortuusars.envelope.client.gui.screen.PackingScreen;
import io.github.mortuusars.envelope.client.gui.screen.PaybackTagScreen;
import io.github.mortuusars.mortaar.client.Minecrft;
import io.github.mortuusars.envelope.integration.jei.category.MailingRecipeCategory;
import io.github.mortuusars.envelope.integration.jei.extensions.AddressTagApplicationRecipeExtension;
import io.github.mortuusars.envelope.integration.jei.extensions.LetterCloningRecipeExtension;
import io.github.mortuusars.envelope.integration.jei.extensions.PaybackTagApplicationRecipeExtension;
import io.github.mortuusars.envelope.integration.jei.ingredient.ServiceAddressIngredientHelper;
import io.github.mortuusars.envelope.integration.jei.ingredient.ServiceAddressIngredientRenderer;
import io.github.mortuusars.envelope.integration.jei.util.InHandRecipeTransferInfo;
import io.github.mortuusars.envelope.integration.jei.util.PaybackTagGhostIngredientHandler;
import io.github.mortuusars.envelope.world.inventory.PackingMenu;
import io.github.mortuusars.envelope.world.item.component.PackageContents;
import io.github.mortuusars.envelope.world.item.crafting.AddressTagApplicationRecipe;
import io.github.mortuusars.envelope.world.item.crafting.LetterCloningRecipe;
import io.github.mortuusars.envelope.world.item.crafting.mail.MailRecipe;
import io.github.mortuusars.envelope.world.item.crafting.PaybackTagApplicationRecipe;
import io.github.mortuusars.envelope.world.mail.address.type.ServiceAddress;
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

    public static final IIngredientType<ServiceAddress> SERVICE_ADDRESS_INGREDIENT = new IIngredientType<>() {
        @Override
        public @NotNull Class<? extends ServiceAddress> getIngredientClass() {
            return ServiceAddress.class;
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
        if (Config.Client.JEI_SERVICE_ADDRESS_INGREDIENT.get()) {
            List<ServiceAddress> addressesWithRecipes = Minecrft.level().getRecipeManager().getAllRecipesFor(Envelope.RecipeTypes.MAILING.get())
                  .stream()
                  .map(recipe -> recipe.value().getAddress())
                  .distinct()
                  .filter(address -> !address.getDefinitionHolder().is(Envelope.Tags.ServiceAddresses.HIDDEN))
                  .toList();

            registration.register(SERVICE_ADDRESS_INGREDIENT,
                  addressesWithRecipes,
                  new ServiceAddressIngredientHelper(),
                  new ServiceAddressIngredientRenderer(),
                  ServiceAddress.CODEC.codec());
        }
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(new ItemStack(Envelope.Items.MAILBOX.get()), EnvelopeJeiRecipeTypes.MAILING_RECIPE_TYPE);
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        List<RecipeHolder<MailRecipe>> mailingRecipes = Minecrft.level()
              .getRecipeManager()
              .getAllRecipesFor(Envelope.RecipeTypes.MAILING.get())
              .stream()
              .filter(recipe ->
                    !recipe.value().getAddress().getDefinitionHolder().is(Envelope.Tags.ServiceAddresses.HIDDEN)
                          && !recipe.value().getIngredients().isEmpty())
              .toList();

        registration.addRecipes(EnvelopeJeiRecipeTypes.MAILING_RECIPE_TYPE, mailingRecipes);
    }

    @Override
    public void registerVanillaCategoryExtensions(IVanillaCategoryExtensionRegistration registration) {
        registration.getCraftingCategory().addExtension(LetterCloningRecipe.class, new LetterCloningRecipeExtension());
        registration.getCraftingCategory().addExtension(AddressTagApplicationRecipe.class, new AddressTagApplicationRecipeExtension());
        registration.getCraftingCategory().addExtension(PaybackTagApplicationRecipe.class, new PaybackTagApplicationRecipeExtension());
    }

    @Override
    public void registerRecipeTransferHandlers(IRecipeTransferRegistration registration) {
        var transferInfo = new InHandRecipeTransferInfo<>(PackingMenu.class, EnvelopeJeiRecipeTypes.MAILING_RECIPE_TYPE,
              0, PackageContents.SLOTS, PackageContents.SLOTS, 36);
        registration.addRecipeTransferHandler(transferInfo);
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        registration.addRecipeClickArea(MailboxScreen.class, 246, 36, 22, 18, EnvelopeJeiRecipeTypes.MAILING_RECIPE_TYPE);

        registration.addRecipeClickArea(PackingScreen.class, 61, 17, 54, 15, EnvelopeJeiRecipeTypes.MAILING_RECIPE_TYPE);
        registration.addRecipeClickArea(PackingScreen.class, 45, 32, 16, 36, EnvelopeJeiRecipeTypes.MAILING_RECIPE_TYPE);
        registration.addRecipeClickArea(PackingScreen.class, 61, 68, 54, 13, EnvelopeJeiRecipeTypes.MAILING_RECIPE_TYPE);
        registration.addRecipeClickArea(PackingScreen.class, 115, 32, 11, 36, EnvelopeJeiRecipeTypes.MAILING_RECIPE_TYPE);

        registration.addGhostIngredientHandler(PaybackTagScreen.class, new PaybackTagGhostIngredientHandler());
    }
}