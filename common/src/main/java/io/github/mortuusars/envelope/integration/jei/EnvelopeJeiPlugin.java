package io.github.mortuusars.envelope.integration.jei;

import io.github.mortuusars.envelope.Config;
import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.client.gui.screen.MailboxScreen;
import io.github.mortuusars.envelope.client.gui.screen.PackingScreen;
import io.github.mortuusars.envelope.client.gui.screen.PaybackTagScreen;
import io.github.mortuusars.envelope.integration.jei.extensions.SealStampDyeingRecipeRecipeExtension;
import io.github.mortuusars.envelope.integration.jei.util.LetterMeaningSubtypeInterpreter;
import io.github.mortuusars.envelope.integration.jei.util.PackingRecipeTransferInfo;
import io.github.mortuusars.envelope.world.item.crafting.SealStampDyeingRecipe;
import io.github.mortuusars.envelope.world.mail.service.cloud_depository.CloudDepository;
import io.github.mortuusars.mortaar.client.Minecrft;
import io.github.mortuusars.envelope.integration.jei.category.MailingRecipeCategory;
import io.github.mortuusars.envelope.integration.jei.extensions.AddressTagApplicationRecipeExtension;
import io.github.mortuusars.envelope.integration.jei.extensions.LetterCloningRecipeExtension;
import io.github.mortuusars.envelope.integration.jei.extensions.PaybackTagApplicationRecipeExtension;
import io.github.mortuusars.envelope.integration.jei.ingredient.ServiceAddressIngredientHelper;
import io.github.mortuusars.envelope.integration.jei.ingredient.ServiceAddressIngredientRenderer;
import io.github.mortuusars.envelope.integration.jei.util.PaybackTagGhostIngredientHandler;
import io.github.mortuusars.envelope.world.item.crafting.AddressTagApplicationRecipe;
import io.github.mortuusars.envelope.world.item.crafting.LetterCloningRecipe;
import io.github.mortuusars.envelope.world.item.crafting.mail.MailRecipe;
import io.github.mortuusars.envelope.world.item.crafting.PaybackTagApplicationRecipe;
import io.github.mortuusars.envelope.world.mail.address.type.ServiceAddress;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.ingredients.IIngredientType;
import mezz.jei.api.registration.*;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@JeiPlugin
public class EnvelopeJeiPlugin implements IModPlugin {
    private static final ResourceLocation ID = Envelope.resource("jei_plugin");

    public static IJeiRuntime runtime;

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
    public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
        runtime = jeiRuntime;
    }

    @Override
    public void onRuntimeUnavailable() {
        runtime = null;
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new MailingRecipeCategory(registration.getJeiHelpers()));
    }

    @Override
    public void registerItemSubtypes(ISubtypeRegistration registration) {
        registration.registerSubtypeInterpreter(Envelope.Items.LETTER.get(), new LetterMeaningSubtypeInterpreter());
    }

    @Override
    public void registerIngredients(IModIngredientRegistration registration) {
        if (Config.Client.JEI_SERVICE_ADDRESS_INGREDIENT.get()) {
            List<ServiceAddress> addressesWithRecipes = Minecrft.level().getRecipeManager().getAllRecipesFor(Envelope.RecipeTypes.MAILING.get())
                  .stream()
                  .map(recipe -> recipe.value().getAddress())
                  .distinct()
                  .filter(address -> ServiceAddress.isEnabled(address) && !address.isHidden())
                  .toList();

            registration.register(SERVICE_ADDRESS_INGREDIENT,
                  addressesWithRecipes,
                  new ServiceAddressIngredientHelper(),
                  new ServiceAddressIngredientRenderer(),
                  ServiceAddress.CODEC.codec());
        }
    }

    @Override
    public void registerExtraIngredients(IExtraIngredientRegistration registration) {
        if (Config.Server.SERVICE_CLOUD_DEPOSITORY_ENABLED.get()) {
            registration.addExtraItemStacks(List.of(
                  CloudDepository.createWithdrawalRequestLetter(),
                  CloudDepository.createStatusRequestLetter(),
                  CloudDepository.createExpansionRequestLetter()
            ));

            registration.addExtraIngredients(SERVICE_ADDRESS_INGREDIENT, List.of(
                  ServiceAddress.getOrThrow(Minecrft.registryAccess(), ServiceAddress.CLOUD_DEPOSITORY)));
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
              .filter(recipe -> ServiceAddress.isEnabled(recipe.value().getAddress())
                    && !recipe.value().getAddress().isHidden()
                    && !recipe.value().getIngredients().isEmpty())
              .toList();

        registration.addRecipes(EnvelopeJeiRecipeTypes.MAILING_RECIPE_TYPE, mailingRecipes);

        addInfo(registration);
    }

    private static void addInfo(IRecipeRegistration registration) {
        if (!Config.Client.JEI_INFORMATION.get()) {
            return;
        }

        registration.addIngredientInfo(ServiceAddress.getOrThrow(Minecrft.registryAccess(), ServiceAddress.MAIL_SERVICE), SERVICE_ADDRESS_INGREDIENT,
              Component.translatable("envelope.jei.info.mail_service"));
        registration.addIngredientInfo(ServiceAddress.getOrThrow(Minecrft.registryAccess(), ServiceAddress.AUTOMATED_SUPPLY_SERVICE), SERVICE_ADDRESS_INGREDIENT,
              Component.translatable("envelope.jei.info.automated_supply_service"));

        ServiceAddress cloudDepository = ServiceAddress.getOrThrow(Minecrft.registryAccess(), ServiceAddress.CLOUD_DEPOSITORY);
        if (ServiceAddress.isEnabled(cloudDepository)) {
            MutableComponent cloudDepositoryAddress = cloudDepository.format()
                  .withIcon()
                  .toComponent()
                  .withStyle(ChatFormatting.DARK_BLUE);
            registration.addItemStackInfo(CloudDepository.createWithdrawalRequestLetter(),
                  Component.translatable("envelope.jei.info.cloud_depository.withdrawal_request", cloudDepositoryAddress));
            registration.addItemStackInfo(CloudDepository.createStatusRequestLetter(),
                  Component.translatable("envelope.jei.info.cloud_depository.status_request", cloudDepositoryAddress));
            registration.addItemStackInfo(CloudDepository.createExpansionRequestLetter(),
                  Component.translatable("envelope.jei.info.cloud_depository.expansion_request", cloudDepositoryAddress,
                        Component.literal(Integer.toString(Config.Server.SERVICE_CLOUD_DEPOSITORY_ACCOUNT_STORAGE_CAPACITY_PER_EXPANSION.get())).withStyle(ChatFormatting.DARK_BLUE)));

            registration.addIngredientInfo(cloudDepository, SERVICE_ADDRESS_INGREDIENT,
                  Component.translatable("envelope.jei.info.cloud_depository",
                        Component.translatable("letter.envelope.cloud_depository.withdrawal_request.name").withStyle(Style.EMPTY
                              .withColor(ChatFormatting.DARK_BLUE)
                              .withUnderlined(true)
                              .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_ITEM, new HoverEvent.ItemStackInfo(CloudDepository.createWithdrawalRequestLetter()))))
                  ));
        }
    }

    @Override
    public void registerVanillaCategoryExtensions(IVanillaCategoryExtensionRegistration registration) {
        registration.getCraftingCategory().addExtension(LetterCloningRecipe.class, new LetterCloningRecipeExtension());
        registration.getCraftingCategory().addExtension(AddressTagApplicationRecipe.class, new AddressTagApplicationRecipeExtension());
        registration.getCraftingCategory().addExtension(PaybackTagApplicationRecipe.class, new PaybackTagApplicationRecipeExtension());
        registration.getCraftingCategory().addExtension(SealStampDyeingRecipe.class, new SealStampDyeingRecipeRecipeExtension());
    }

    @Override
    public void registerRecipeTransferHandlers(IRecipeTransferRegistration registration) {
        registration.addRecipeTransferHandler(new PackingRecipeTransferInfo());
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