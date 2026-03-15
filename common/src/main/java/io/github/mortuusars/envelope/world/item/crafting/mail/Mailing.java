package io.github.mortuusars.envelope.world.item.crafting.mail;

import com.mojang.logging.LogUtils;
import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.world.item.component.PackageContents;
import io.github.mortuusars.envelope.world.mail.address.type.EntityAddress;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.slf4j.Logger;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public class Mailing {
    private static final Logger LOGGER = LogUtils.getLogger();

    public static List<RecipeHolder<MailRecipe>> getAllRecipes(ServerLevel level) {
        return level.getRecipeManager().getAllRecipesFor(Envelope.RecipeTypes.MAILING.get());
    }

    public static Stream<RecipeHolder<MailRecipe>> getAllRecipesOf(EntityAddress address, ServerLevel level) {
        return getAllRecipes(level)
              .stream()
              .filter(recipeHolder -> recipeHolder.value().getAddress().equals(address));
    }

    public static Optional<RecipeHolder<MailRecipe>> findMatchingRecipe(EntityAddress address, MailRecipeInput input, ServerLevel level) {
        return getAllRecipesOf(address, level)
              .filter(recipeHolder -> recipeHolder.value().matches(input, level))
              .findFirst();
    }

    // --

    public static Result craft(MailRecipe recipe, MailRecipeInput input) {
        SimpleContainer outputContainer = new SimpleContainer(PackageContents.SLOTS);
        int experience = 0;

        do {
            if (!outputContainer.canAddItem(recipe.assemble(input, input.level().registryAccess()))) {
                break;
            }

            ItemStack result = recipe.assembleWithSideEffects(input, input.level().registryAccess());
            consumeInputs(recipe, input);
            experience += recipe.getExperiencePoints();

            if (!outputContainer.addItem(result).isEmpty()) {
                LOGGER.error("Unable to insert mail crafting result '{}' without a remainder into package container {}. " +
                      "Part of the result will be voided.", result, outputContainer);
            }

            if (recipe.isOneCraftPerDelivery()) {
                break;
            }

            if (!PackageContents.canHold(result)) {
                // If package cannot hold a result, then it's likely another package.
                // Stop the crafting to return only one package.
                break;
            }
        }
        while (recipe.matches(input, input.level()));

        return new Result(input, outputContainer.removeAllItems(), experience);
    }

    public static void consumeInputs(MailRecipe recipe, MailRecipeInput input) {
        NonNullList<ItemStack> remainingItems = recipe.getRemainingItems(input);

        for (int slot = 0; slot < input.items().size(); slot++) {
            ItemStack item = input.getItem(slot);
            if (!item.isEmpty()) {
                item.shrink(1);
                input.setItem(slot, item);
            }

            ItemStack remainingItem = remainingItems.get(slot);
            if (!remainingItem.isEmpty()) {
                if (item.isEmpty()) {
                    input.setItem(slot, remainingItem);
                } else if (ItemStack.isSameItemSameComponents(item, remainingItem)) {
                    remainingItem.grow(item.getCount());
                    input.setItem(slot, remainingItem);
                } else {
                    LOGGER.warn("Cannot insert remaining item [{}] into slot [{}] while crafting '{}'. Input: {}",
                          remainingItem, slot, recipe, input);
                }
            }
        }
    }

    // --

    public record Result(MailRecipeInput input, List<ItemStack> output, int experience) {
    }
}
