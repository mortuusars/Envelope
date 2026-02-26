package io.github.mortuusars.envelope.util.bugger.test.cases;

import io.github.mortuusars.envelope.util.bugger.test.BuggerTests;
import io.github.mortuusars.envelope.util.bugger.test.Test;
import io.github.mortuusars.envelope.world.inventory.StackIngredient;
import io.github.mortuusars.envelope.world.item.crafting.CraftingResult;
import io.github.mortuusars.envelope.world.item.crafting.MailCraftingRecipe;
import io.github.mortuusars.envelope.world.item.crafting.PackageRecipeInput;
import io.github.mortuusars.envelope.world.mail.MailService;
import io.github.mortuusars.envelope.world.mail.handler.CraftingMailHandler;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Arrays;

public class MailCraftingTests extends BuggerTests {
    public static final int RECIPE_EXPERIENCE = 3;
    private final MinecraftServer server;

    public MailCraftingTests(MinecraftServer server) {
        this.server = server;

        CraftingMailHandler craftingReceiver = new CraftingMailHandler(MailService.of(server.overworld()).getAddress());

        add("MailCrafting_CraftsWithoutRemainder", Test.isTrue(() -> {
            CraftingResult result = craftingReceiver.craft(server.overworld(),
                  recipe(
                        new StackIngredient(Items.EMERALD, 12),
                        new StackIngredient(Items.DIAMOND, 6),
                        new StackIngredient(Items.DIAMOND, 6)
                  ),
                  input(
                        new ItemStack(Items.EMERALD, 12),
                        new ItemStack(Items.DIAMOND, 6),
                        new ItemStack(Items.DIAMOND, 6)
                  ));

            return !result.hasRemainder()
                  && result.output().size() == 1;
        }));

        add("MailCrafting_CraftsWithRemainder", Test.isTrue(() -> {
            CraftingResult result = craftingReceiver.craft(server.overworld(),
                  recipe(
                        new StackIngredient(Items.EMERALD, 12),
                        new StackIngredient(Items.DIAMOND, 6),
                        new StackIngredient(Items.DIAMOND, 6)
                  ),
                  input(
                        new ItemStack(Items.EMERALD, 16),
                        new ItemStack(Items.DIAMOND, 6),
                        new ItemStack(Items.DIAMOND, 6)
                  ));

            return result.hasRemainder()
                  && result.remainingInput().getItems().filter(stack -> !stack.isEmpty()).toList().getFirst().getCount() == 4
                  && result.output().size() == 1;
        }));

        add("MailCrafting_CraftsMultiple", Test.isTrue(() -> {
            CraftingResult result = craftingReceiver.craft(server.overworld(),
                  recipe(
                        new StackIngredient(Items.EMERALD, 12),
                        new StackIngredient(Items.DIAMOND, 6),
                        new StackIngredient(Items.DIAMOND, 6)
                  ),
                  input(
                        new ItemStack(Items.EMERALD, 36),
                        new ItemStack(Items.DIAMOND, 18),
                        new ItemStack(Items.DIAMOND, 18)
                  ));

            return !result.hasRemainder() && result.output().getFirst().getCount() == 3;
        }));

        add("MailCrafting_CraftingReturnsCorrectTotalExperience", Test.isTrue(() -> {
            CraftingResult result = craftingReceiver.craft(server.overworld(),
                  recipe(
                        new StackIngredient(Items.EMERALD, 12),
                        new StackIngredient(Items.DIAMOND, 6),
                        new StackIngredient(Items.DIAMOND, 6)
                  ),
                  input(
                        new ItemStack(Items.EMERALD, 36),
                        new ItemStack(Items.DIAMOND, 18),
                        new ItemStack(Items.DIAMOND, 18)
                  ));

            return !result.hasRemainder() && result.experience() == 9;
        }));
    }

    // --

    private MailCraftingRecipe recipe(StackIngredient... ingredients) {
        return new MailCraftingRecipe(
              MailService.of(server.overworld()).getAddress(),
              Arrays.stream(ingredients).toList(),
              new ItemStack(Items.BARRIER),
              RECIPE_EXPERIENCE);
    }

    private PackageRecipeInput input(ItemStack... items) {
        return PackageRecipeInput.of(Arrays.stream(items).toList());
    }

    private boolean matches(MailCraftingRecipe recipe, PackageRecipeInput input) {
        return recipe.matches(input, server.overworld());
    }
}
