package io.github.mortuusars.envelope.world.mail.dropoff;

import com.mojang.logging.LogUtils;
import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.world.item.component.PackageContents;
import io.github.mortuusars.envelope.world.item.component.mail.log.DeliveryRecord;
import io.github.mortuusars.envelope.world.item.crafting.CraftingResult;
import io.github.mortuusars.envelope.world.item.crafting.MailRecipe;
import io.github.mortuusars.envelope.world.item.crafting.PackageRecipeInput;
import io.github.mortuusars.envelope.world.item.mail.Mail;
import io.github.mortuusars.envelope.world.mail.MailService;
import io.github.mortuusars.envelope.world.mail.address.Address;
import io.github.mortuusars.envelope.world.mail.address.type.EntityAddress;
import io.github.mortuusars.envelope.world.mail.delivery.Delivery;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.List;
import java.util.stream.Collectors;

public class CraftingDropOffHandler implements MailDropOffHandler {
    private static final Logger LOGGER = LogUtils.getLogger();

    @Override
    public MailDropOffResult handle(MailDropOffContext context) {
        if (!(context.getTarget() instanceof EntityAddress address)) {
            return MailDropOffResult.PASS;
        }

        ItemStack mail = context.getMail();

        if (mail.isEmpty()) {
            return MailDropOffResult.CONSUME;
        }

        if (context.isReturned()) {
            LOGGER.info("Crafting '{}' received returned mail. Delivery: '{}'. Voiding", address, context.getDelivery());
            return MailDropOffResult.CONSUME;
        }

        Address sender = context.getDelivery().getSender();
        if (sender.equals(address)) {
            LOGGER.info("Mail Entity Crafting Handler cannot handle mail when sender is it's own address '{}'. Voiding.", context.getDelivery());
            return MailDropOffResult.CONSUME;
        }

        PackageContents packageContents = PackageContents.from(mail);
        if (packageContents.isEmpty()) {
            return MailDropOffResult.PASS;
        }

        PackageRecipeInput input = PackageRecipeInput.of(packageContents);

        @Nullable MailRecipe recipe = findMatchingRecipe(context.getLevel(), address, input);
        if (recipe == null) {
            return MailDropOffResult.PASS;
        }

        CraftingResult result = craft(context.getLevel(), recipe, input);

        if (result.output().isEmpty()) {
            LOGGER.warn("No results from the mail crafting.");
            return MailDropOffResult.returned(mail, DeliveryRecord.Message.CRAFTING_UNABLE_TO_PROCESS);
        }

        List<ItemStack> packages = Mail.createPackages(result.output(),
              pkg -> pkg.recipient(sender));

        // It's not ideal that all experience is on one package.
        // But implementing it per package would be more complicated, and usually there's only one anyway.
        packages.stream().findFirst().ifPresent(pkg ->
              pkg.set(Envelope.DataComponents.PACKAGE_EXPERIENCE, MailRecipe.calculateExperiencePoints(result.experience())));

        return sendResults(context.getService(), address, mail, result.remainingInput(), packages, sender);
    }

    public List<RecipeHolder<MailRecipe>> getAllRecipes(ServerLevel level, EntityAddress address) {
        return level.getRecipeManager().getAllRecipesFor(Envelope.RecipeTypes.MAILING.get()).stream()
              .filter(recipeHolder -> recipeHolder.value().getEntityAddress().equals(address))
              .collect(Collectors.toList());
    }

    public @Nullable MailRecipe findMatchingRecipe(ServerLevel level, EntityAddress address, PackageRecipeInput input) {
        for (RecipeHolder<MailRecipe> recipeHolder : getAllRecipes(level, address)) {
            MailRecipe recipe = recipeHolder.value();
            if (recipe.matches(input, level)) {
                return recipe;
            }
        }
        return null;
    }

    public CraftingResult craft(ServerLevel level, MailRecipe recipe, PackageRecipeInput input) {
        SimpleContainer outputContainer = new SimpleContainer(PackageContents.SLOTS);
        float experience = 0;

        do {
            ItemStack result = recipe.assemble(input, level.registryAccess());
            if (!outputContainer.addItem(result).isEmpty()) {
                break;
            }

            result = recipe.assembleFinal(input, level);
            result.onCraftedBySystem(level);
            experience += recipe.getExperience();

            input = PackageRecipeInput.of(recipe.consumeOnce(input));

            if (!PackageContents.canHold(result)) {
                // If package cannot hold a result, then it's likely another package.
                // Stop the crafting to return only one package.
                break;
            }
        }
        while (recipe.matches(input, level));

        return new CraftingResult(input, outputContainer.removeAllItems(), experience);
    }

    protected MailDropOffResult sendResults(MailService service, EntityAddress address, ItemStack mail, PackageRecipeInput remainingInput, List<ItemStack> packages, Address destination) {
        boolean hasRemainder = false;

        if (!remainingInput.isEmpty()) {
            ItemStack remainderPackage = mail.copy();
            PackageContents remainderContents = new PackageContents(remainingInput.getItems().toList());
            remainderPackage.set(Envelope.DataComponents.PACKAGE_CONTENTS, remainderContents);
            packages.addFirst(remainderPackage);
            hasRemainder = true;
        }

        if (packages.isEmpty()) {
            return MailDropOffResult.PASS;
        }

        packages.stream()
              .skip(1)
              .forEach(pkg -> {
                  service.getDeliveryManager()
                        .startService(Delivery.draft()
                              .deliver(pkg)
                              .from(address)
                              .to(destination));
              });

        if (hasRemainder) {
            return MailDropOffResult.returned(packages.getFirst(), DeliveryRecord.Message.CRAFTING_UNPROCESSED_ITEMS);
        } else {
            return MailDropOffResult.reply(Mail.of(packages.getFirst()).get());
        }
    }
}