package io.github.mortuusars.envelope.util.bugger.test.cases;

import io.github.mortuusars.envelope.util.bugger.test.BuggerTests;
import io.github.mortuusars.envelope.util.bugger.test.Test;
import io.github.mortuusars.envelope.world.item.component.PackageContents;
import io.github.mortuusars.envelope.world.item.crafting.mail.MailCraftingRecipe;
import io.github.mortuusars.envelope.world.mail.MailService;
import net.minecraft.core.NonNullList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.Arrays;

public class MailCraftingRecipeTests extends BuggerTests {
    private final MinecraftServer server;

    public MailCraftingRecipeTests(MinecraftServer server) {
        this.server = server;
        matching();
        consuming();
    }

    private void matching() {
        add("MailCraftingRecipe_MatchesIfCorrect", Test.isTrue(() -> matches(
              recipe(
                    Ingredient.of(Items.EMERALD),
                    Ingredient.of(Items.DIAMOND)
              ),
              input(
                    new ItemStack(Items.EMERALD, 2),
                    new ItemStack(Items.DIAMOND, 6)
              ))));

        add("MailCraftingRecipe_MatchesIfCorrectMultipleOfSame", Test.isTrue(() -> matches(
              recipe(
                    Ingredient.of(Items.EMERALD),
                    Ingredient.of(Items.DIAMOND),
                    Ingredient.of(Items.DIAMOND),
                    Ingredient.of(Items.COPPER_INGOT)
              ),
              input(
                    new ItemStack(Items.EMERALD, 2),
                    new ItemStack(Items.DIAMOND, 6),
                    new ItemStack(Items.DIAMOND, 6),
                    new ItemStack(Items.COPPER_INGOT, 6)
              ))));

        add("MailCraftingRecipe_MatchesIfMore", Test.isTrue(() -> matches(
              recipe(
                    Ingredient.of(Items.EMERALD),
                    Ingredient.of(Items.DIAMOND)
              ),
              input(
                    new ItemStack(Items.EMERALD, 30),
                    new ItemStack(Items.DIAMOND, 30)
              )
        )));

        add("MailCraftingRecipe_MatchesIfMoreMultipleOfSame", Test.isTrue(() -> matches(
              recipe(
                    Ingredient.of(Items.EMERALD),
                    Ingredient.of(Items.DIAMOND),
                    Ingredient.of(Items.DIAMOND),
                    Ingredient.of(Items.COPPER_INGOT)
              ),
              input(
                    new ItemStack(Items.EMERALD, 30),
                    new ItemStack(Items.DIAMOND, 30),
                    new ItemStack(Items.DIAMOND, 30),
                    new ItemStack(Items.COPPER_INGOT, 30)
              ))));

        add("MailCraftingRecipe_MatchesWhenOutOfOrder", Test.isTrue(() -> matches(
              recipe(
                    Ingredient.of(Items.EMERALD),
                    Ingredient.of(Items.DIAMOND),
                    Ingredient.of(Items.DIAMOND),
                    Ingredient.of(Items.COPPER_INGOT)
              ),
              input(
                    new ItemStack(Items.COPPER_INGOT, 30),
                    new ItemStack(Items.DIAMOND, 30),
                    new ItemStack(Items.EMERALD, 30),
                    new ItemStack(Items.DIAMOND, 30)
              ))));

        add("MailCraftingRecipe_MatchesWithEmptyItems", Test.isTrue(() -> matches(
              recipe(
                    Ingredient.of(Items.EMERALD),
                    Ingredient.of(Items.DIAMOND)
              ),
              input(
                    ItemStack.EMPTY,
                    ItemStack.EMPTY,
                    new ItemStack(Items.DIAMOND, 6),
                    new ItemStack(Items.EMERALD, 2),
                    ItemStack.EMPTY
              )
        )));

        // --

        add("MailCraftingRecipe_Matching_FailsIfInputEmpty", Test.isFalse(() -> matches(
              recipe(
                    Ingredient.of(Items.EMERALD),
                    Ingredient.of(Items.DIAMOND)
              ),
              input(
                    ItemStack.EMPTY,
                    ItemStack.EMPTY
              )
        )));

        add("MailCraftingRecipe_Matching_FailsIfNotEnough", Test.isFalse(() -> matches(
              recipe(
                    Ingredient.of(Items.EMERALD),
                    Ingredient.of(Items.DIAMOND)
              ),
              input(
                    new ItemStack(Items.EMERALD, 2)
              )
        )));

        add("MailCraftingRecipe_Matching_FailsIfLessInputs", Test.isFalse(() -> matches(
              recipe(
                    Ingredient.of(Items.EMERALD),
                    Ingredient.of(Items.DIAMOND),
                    Ingredient.of(Items.ACACIA_BOAT)
              ),
              input(
                    new ItemStack(Items.EMERALD, 2),
                    new ItemStack(Items.DIAMOND, 1)
              )
        )));

        add("MailCraftingRecipe_Matching_FailsIfMoreInputs", Test.isFalse(() -> matches(
              recipe(
                    Ingredient.of(Items.EMERALD),
                    Ingredient.of(Items.DIAMOND)
              ),
              input(
                    new ItemStack(Items.EMERALD, 2),
                    new ItemStack(Items.DIAMOND, 1),
                    new ItemStack(Items.ACACIA_BOAT)
              )
        )));

        add("MailCraftingRecipe_Matching_FailsIfMoreInputsOfCorrectItem", Test.isFalse(() ->
              matches(
                    recipe(
                          Ingredient.of(Items.EMERALD),
                          Ingredient.of(Items.DIAMOND)
                    ),
                    input(
                          new ItemStack(Items.EMERALD, 2),
                          new ItemStack(Items.DIAMOND, 6),
                          new ItemStack(Items.DIAMOND, 3)
                    )
              )));
    }

    private void consuming() {
        add("MailCraftingRecipe_ConsumesCorrectly", Test.isTrue(() -> {
            MailCraftingRecipe recipe = recipe(
                  Ingredient.of(Items.EMERALD),
                  Ingredient.of(Items.DIAMOND)
            );

            PackageContents input = input(
                  new ItemStack(Items.EMERALD),
                  new ItemStack(Items.DIAMOND)
            );

            return recipe.consumeInput(input).isEmpty();
        }));

        add("MailCraftingRecipe_ConsumesAndReturnsRemainder", Test.isTrue(() -> {
            MailCraftingRecipe recipe = recipe(
                  Ingredient.of(Items.EMERALD),
                  Ingredient.of(Items.DIAMOND)
            );

            PackageContents input = input(
                  new ItemStack(Items.EMERALD),
                  new ItemStack(Items.DIAMOND, 3)
            );

            return recipe.consumeInput(input).getItems()
                  .stream()
                  .filter(stack -> !stack.isEmpty())
                  .findAny()
                  .map(stack -> stack.getItem() == Items.DIAMOND && stack.getCount() == 2)
                  .orElse(false);
        }));

        add("MailCraftingRecipe_ConsumesOnlyForOneOperation", Test.isTrue(() -> {
            MailCraftingRecipe recipe = recipe(
                  Ingredient.of(Items.EMERALD),
                  Ingredient.of(Items.DIAMOND)
            );

            PackageContents input = input(
                  new ItemStack(Items.EMERALD, 6),
                  new ItemStack(Items.DIAMOND, 4)
            );

            PackageContents remainder = recipe.consumeInput(input);

            return remainder.getItems().getFirst().getCount() == 5 && remainder.getItems().get(1).getCount() == 3;
        }));
    }

    // --

    private MailCraftingRecipe recipe(Ingredient... ingredients) {
        return new MailCraftingRecipe(
              MailService.of(server.overworld()).getAddress(),
              NonNullList.of(Ingredient.EMPTY, ingredients),
              new ItemStack(Items.BARRIER),
              0);
    }

    private PackageContents input(ItemStack... items) {
        return new PackageContents(Arrays.stream(items).toList());
    }

    private boolean matches(MailCraftingRecipe recipe, PackageContents input) {
        return recipe.matches(input, server.overworld());
    }
}
