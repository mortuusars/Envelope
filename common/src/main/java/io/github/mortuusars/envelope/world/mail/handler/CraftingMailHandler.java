package io.github.mortuusars.envelope.world.mail.handler;

import com.mojang.logging.LogUtils;
import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.world.mail.delivery.Delivery;
import io.github.mortuusars.envelope.world.item.component.PackageContents;
import io.github.mortuusars.envelope.world.item.component.mail.log.DeliveryRecord;
import io.github.mortuusars.envelope.world.item.crafting.CraftingResult;
import io.github.mortuusars.envelope.world.item.crafting.MailRecipe;
import io.github.mortuusars.envelope.world.item.crafting.PackageRecipeInput;
import io.github.mortuusars.envelope.world.item.mail.Mail;
import io.github.mortuusars.envelope.world.mail.MailService;
import io.github.mortuusars.envelope.world.mail.address.Address;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.List;
import java.util.stream.Collectors;

public class CraftingMailHandler implements MailHandler {
    private static final Logger LOGGER = LogUtils.getLogger();

    private final Address address;

    public CraftingMailHandler(Address address) {
        this.address = address;
    }

    @Override
    public MailHandlingResult handle(MailService service, Delivery delivery) {
        ItemStack mail = delivery.getMail();

        if (mail.isEmpty()) {
            return MailHandlingResult.CONSUME;
        }

        PackageContents packageContents = PackageContents.from(mail);
        if (packageContents.isEmpty()) {
            return MailHandlingResult.PASS;
        }

        PackageRecipeInput input = PackageRecipeInput.of(packageContents);

        @Nullable MailRecipe recipe = findMatchingRecipe(service.getLevel(), input);
        if (recipe == null) {
            return MailHandlingResult.PASS;
        }

        CraftingResult result = craft(service.getLevel(), recipe, input);

        if (result.output().isEmpty()) {
            LOGGER.warn("No results from the mail crafting.");
            return MailHandlingResult.returned(mail, DeliveryRecord.Message.CRAFTING_UNABLE_TO_PROCESS);
        }

        List<ItemStack> packages = Mail.createPackages(result.output(),
              pkg -> pkg.recipient(delivery.getSender()));

        // It's not ideal that all experience is on one package.
        // But implementing it per package would be more complicated, and usually there's only one anyway.
        packages.stream().findFirst().ifPresent(pkg ->
              pkg.set(Envelope.DataComponents.PACKAGE_EXPERIENCE, MailRecipe.calculateExperiencePoints(result.experience())));

        return sendResults(service, mail, result.remainingInput(), packages, delivery.getSender());
    }

    public List<RecipeHolder<MailRecipe>> getAllRecipes(ServerLevel level) {
        return level.getRecipeManager().getAllRecipesFor(Envelope.RecipeTypes.MAILING.get()).stream()
              .filter(recipeHolder -> recipeHolder.value().getEntityAddress().equals(address))
              .collect(Collectors.toList());
    }

    public @Nullable MailRecipe findMatchingRecipe(ServerLevel level, PackageRecipeInput input) {
        for (RecipeHolder<MailRecipe> recipeHolder : getAllRecipes(level)) {
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
            result.onCraftedBySystem(level);
            experience += recipe.getExperience();

            if (!outputContainer.addItem(result).isEmpty()) {
                break;
            }

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

    protected MailHandlingResult sendResults(MailService service, ItemStack mail, PackageRecipeInput remainingInput, List<ItemStack> packages, Address destination) {
        boolean hasRemainder = false;

        if (!remainingInput.isEmpty()) {
            ItemStack remainderPackage = mail.copy();
            PackageContents remainderContents = new PackageContents(remainingInput.getItems().toList());
            remainderPackage.set(Envelope.DataComponents.PACKAGE_CONTENTS, remainderContents);
            packages.addFirst(remainderPackage);
            hasRemainder = true;
        }

        if (packages.isEmpty()) {
            return MailHandlingResult.PASS;
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
            return MailHandlingResult.returned(packages.getFirst(), DeliveryRecord.Message.CRAFTING_UNPROCESSED_ITEMS);
        } else {
            return MailHandlingResult.reply(Mail.of(packages.getFirst())
                  .sender(address)
                  .writeToLog(DeliveryRecord.sentFrom(address))
                  .get());
        }
    }
}