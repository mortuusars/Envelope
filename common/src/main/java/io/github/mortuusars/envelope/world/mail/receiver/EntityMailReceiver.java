package io.github.mortuusars.envelope.world.mail.receiver;

import com.mojang.logging.LogUtils;
import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.world.delivery.Delivery;
import io.github.mortuusars.envelope.world.inventory.recipe.MailRecipe;
import io.github.mortuusars.envelope.world.item.PackageItem;
import io.github.mortuusars.envelope.world.item.component.PackageContents;
import io.github.mortuusars.envelope.world.item.component.mail.log.DeliveryRecord;
import io.github.mortuusars.envelope.world.item.mail.Mail;
import io.github.mortuusars.envelope.world.mail.address.Address;
import io.github.mortuusars.envelope.world.mail.MailService;
import io.github.mortuusars.envelope.world.mail.address.type.EntityAddress;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class EntityMailReceiver implements MailReceiver {
    private static final Logger LOGGER = LogUtils.getLogger();

    private final EntityAddress address;

    public EntityMailReceiver(EntityAddress address) {
        this.address = address;
    }

    @Override
    public ItemStack receiveMail(ServerLevel level, Address sender, ItemStack mail) {
        if (Mail.isReturned(mail)) {
            LOGGER.info("Mail Entity received returned mail [{}] from '{}'. Voiding.", mail, sender);
            return ItemStack.EMPTY;
        }

        ItemStack craftingResult = tryHandleCrafting(level, sender, mail);
        if (!craftingResult.isEmpty()) {
            return craftingResult;
        }

        return MailService.of(level).getMailEntities().byAddress(address)
              .map(entity -> entity.receiveMail(level, sender, mail))
              .orElseGet(() -> returned(mail, DeliveryRecord.Message.RECIPIENT_NOT_FOUND));
    }

    public List<RecipeHolder<MailRecipe>> getRecipesByAddress(ServerLevel level, EntityAddress address) {
        return level.getRecipeManager().getAllRecipesFor(Envelope.RecipeTypes.MAIL.get()).stream()
              .filter(recipeHolder -> recipeHolder.value().getAddress().equals(address))
              .collect(Collectors.toList());
    }

    protected ItemStack tryHandleCrafting(ServerLevel level, Address sender, ItemStack mail) {
        if (sender.isUnknown()) {
            return returned(mail, DeliveryRecord.Message.NO_RETURN_ADDRESS);
        }

        if (!(mail.getItem() instanceof PackageItem)) {
            return ItemStack.EMPTY;
        }

        PackageContents packageContents = PackageContents.from(mail);
        if (packageContents.isEmpty()) {
            return returned(mail, DeliveryRecord.Message.REJECTED);
        }

        List<RecipeHolder<MailRecipe>> recipes = getRecipesByAddress(level, address);
        if (recipes.isEmpty()) {
            return ItemStack.EMPTY;
        }

        List<ItemStack> inputItems = new ArrayList<>(packageContents.copyItems());
        CraftingInput input = CraftingInput.of(3, 2, inputItems);

        @Nullable MailRecipe recipe = recipes.stream()
              .filter(holder -> holder.value().matches(input, level))
              .findFirst()
              .map(RecipeHolder::value)
              .orElse(null);

        if (recipe == null) {
            return ItemStack.EMPTY;
        }

        List<ItemStack> outputItemsItems = craft(level, recipe, inputItems);

        if (outputItemsItems.isEmpty()) {
            LOGGER.warn("No results from the mail crafting.");
            return returned(mail, Component.translatable("gui.envelope.delivery_log.message.returned_unable_to_process"));
        }

        List<ItemStack> packages = createResultPackages(mail, inputItems, outputItemsItems, sender);
        return sendResultPackages(level, packages, sender);
    }

    protected List<ItemStack> craft(ServerLevel level, MailRecipe recipe, List<ItemStack> inputItems) {
        SimpleContainer resultContainer = new SimpleContainer(36);

        CraftingInput input = CraftingInput.of(3, 2, inputItems);

        do {
            ItemStack result = recipe.assemble(input, level.registryAccess());

            if (result.isEmpty()) {
                LOGGER.warn("Assembled result of the MailRecipe is empty.");
                break;
            }

            inputItems.forEach(inputStack -> inputStack.shrink(1));

            result.onCraftedBySystem(level);

            if (!resultContainer.addItem(result).isEmpty()) {
                break;
            }

            input = CraftingInput.of(3, 2, inputItems);
        }
        while (recipe.matches(input, level));

        return resultContainer.removeAllItems();
    }

    protected List<ItemStack> createResultPackages(ItemStack mail, List<ItemStack> remainingItems, List<ItemStack> results, Address sender) {
        List<ItemStack> packages = new ArrayList<>();

        if (!remainingItems.stream().allMatch(ItemStack::isEmpty)) {
            ItemStack remainderPackage = mail.copy();
            PackageContents remainderContents = PackageContents.createFrom(new SimpleContainer(remainingItems.toArray(ItemStack[]::new)));
            remainderPackage.set(Envelope.DataComponents.PACKAGE_CONTENTS, remainderContents);
            packages.add(remainderPackage);
        }

        for (int i = 0; i < results.size(); i += PackageContents.SLOTS) {
            PackageContents contents = new PackageContents(results.stream().skip(i).limit(PackageContents.SLOTS).toList());

            ItemStack resultPackage = Mail.createPackage(contents)
                  .recipient(sender)
                  .get();

            packages.add(resultPackage);
        }

        return packages;
    }

    protected ItemStack sendResultPackages(ServerLevel level, List<ItemStack> packages, Address sender) {
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

        if (packages.size() > 1) {
            return returned(packages.getFirst(), Component.literal("Unprocessed Items"));
        } else {
            return Mail.of(packages.getFirst())
                  .sender(address)
                  .writeToLog(DeliveryRecord.sentFrom(address, level.getGameTime()))
                  .get();
        }
    }
}