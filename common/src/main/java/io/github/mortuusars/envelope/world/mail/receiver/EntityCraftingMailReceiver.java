package io.github.mortuusars.envelope.world.mail.receiver;

import com.mojang.logging.LogUtils;
import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.world.delivery.Delivery;
import io.github.mortuusars.envelope.world.item.component.PackageContents;
import io.github.mortuusars.envelope.world.item.component.mail.log.DeliveryRecord;
import io.github.mortuusars.envelope.world.item.crafting.CraftingResult;
import io.github.mortuusars.envelope.world.item.crafting.MailRecipe;
import io.github.mortuusars.envelope.world.item.crafting.PackageRecipeInput;
import io.github.mortuusars.envelope.world.item.mail.Mail;
import io.github.mortuusars.envelope.world.mail.MailService;
import io.github.mortuusars.envelope.world.mail.address.Address;
import io.github.mortuusars.envelope.world.mail.address.type.EntityAddress;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.List;
import java.util.stream.Collectors;

public class EntityCraftingMailReceiver implements MailReceiver {
    private static final Logger LOGGER = LogUtils.getLogger();

    private final EntityAddress address;

    public EntityCraftingMailReceiver(EntityAddress address) {
        this.address = address;
    }

    @Override
    public ItemStack receiveMail(ServerLevel level, Address sender, ItemStack mail) {
        PackageContents packageContents = PackageContents.from(mail);
        if (packageContents.isEmpty()) {
            return ItemStack.EMPTY;
        }

        PackageRecipeInput input = PackageRecipeInput.of(packageContents);

        @Nullable MailRecipe recipe = findMatchingRecipe(level, input);
        if (recipe == null) {
            return ItemStack.EMPTY;
        }

        CraftingResult result = craft(level, recipe, input);

        if (result.output().isEmpty()) {
            LOGGER.warn("No results from the mail crafting.");
            return returned(mail, DeliveryRecord.Message.CRAFTING_UNABLE_TO_PROCESS);
        }

        List<ItemStack> packages = Mail.createPackages(result.output(), pkg -> pkg.recipient(sender));
        return sendResults(level, mail, result.remainingInput(), packages, sender);
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

        do {
            ItemStack result = recipe.assemble(input, level.registryAccess());
            result.onCraftedBySystem(level);

            if (!outputContainer.addItem(result).isEmpty()) {
                break;
            }

            input = PackageRecipeInput.of(recipe.consumeOnce(input));

            if (!PackageContents.canHold(result)) {
                break;
            }
        }
        while (recipe.matches(input, level));

        return new CraftingResult(input, outputContainer.removeAllItems());
    }

    protected ItemStack sendResults(ServerLevel level, ItemStack mail, PackageRecipeInput remainingInput, List<ItemStack> packages, Address sender) {
        boolean hasRemainder = false;

        if (!remainingInput.isEmpty()) {
            ItemStack remainderPackage = mail.copy();
            PackageContents remainderContents = new PackageContents(remainingInput.getItems().toList());
            remainderPackage.set(Envelope.DataComponents.PACKAGE_CONTENTS, remainderContents);
            packages.addFirst(remainderPackage);
            hasRemainder = true;
        }

        if (packages.isEmpty()) {
            return ItemStack.EMPTY;
        }

        packages.stream()
              .skip(1)
              .forEach(pkg -> {
                  MailService.of(level).getDeliveryManager()
                        .startService(Delivery.draft()
                              .deliver(pkg)
                              .from(address)
                              .to(sender));
              });

        if (hasRemainder) {
            return returned(packages.getFirst(), DeliveryRecord.Message.CRAFTING_UNPROCESSED_ITEMS);
        }

        return Mail.of(packages.getFirst())
              .sender(address)
              .writeToLog(DeliveryRecord.sentFrom(address, level.getGameTime()))
              .get();
    }
}
