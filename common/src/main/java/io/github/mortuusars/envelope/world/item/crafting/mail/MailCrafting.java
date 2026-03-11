package io.github.mortuusars.envelope.world.item.crafting.mail;

import com.mojang.logging.LogUtils;
import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.world.item.component.PackageContents;
import io.github.mortuusars.envelope.world.mail.address.Address;
import io.github.mortuusars.envelope.world.mail.address.type.EntityAddress;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.slf4j.Logger;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public class MailCrafting {

    public static final Logger LOGGER = LogUtils.getLogger();

    public static List<RecipeHolder<MailingRecipe>> getAllRecipes(ServerLevel level) {
        return level.getRecipeManager().getAllRecipesFor(Envelope.RecipeTypes.MAILING.get());
    }

    public static Stream<RecipeHolder<MailingRecipe>> getAllRecipesOf(EntityAddress address, ServerLevel level) {
        return getAllRecipes(level)
              .stream()
              .filter(recipeHolder -> recipeHolder.value().getAddress().equals(address));
    }

    public static Optional<RecipeHolder<MailingRecipe>> findMatchingRecipe(EntityAddress address, PackageContents input, ServerLevel level) {
        return getAllRecipesOf(address, level)
              .filter(recipeHolder -> recipeHolder.value().matches(input, level))
              .findFirst();
    }

    // --

    public static Result craft(ServerLevel level, MailingRecipe recipe, PackageContents input, Address sender) {
        SimpleContainer outputContainer = new SimpleContainer(PackageContents.SLOTS);
        int experience = 0;

        do {
            ItemStack result = recipe.assemble(input, level.registryAccess());

            if (!outputContainer.canAddItem(result)) {
                break;
            }

            MailingRecipe.CraftingResult craftingResult = recipe.craft(input, sender, level);
            experience += craftingResult.experience();
            input = craftingResult.input();

            if (!outputContainer.addItem(craftingResult.output()).isEmpty()) {
                LOGGER.error("Unable to insert mail crafting result '{}' without a remainder into package container {}. " +
                      "Part of the result will be voided.", craftingResult.output(), outputContainer);
            }

            if (recipe.onlyOneCraftPerDelivery()) {
                break;
            }

            if (!PackageContents.canHold(craftingResult.output())) {
                // If package cannot hold a result, then it's likely another package.
                // Stop the crafting to return only one package.
                break;
            }
        }
        while (recipe.matches(input, level));

        return new Result(input, outputContainer.removeAllItems(), experience);
    }

    // --

    public record Result(PackageContents remainder, List<ItemStack> output, int experience) { }
}
