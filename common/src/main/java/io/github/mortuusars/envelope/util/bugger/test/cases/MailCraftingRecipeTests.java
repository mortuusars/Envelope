package io.github.mortuusars.envelope.util.bugger.test.cases;

import io.github.mortuusars.envelope.util.bugger.test.BuggerTests;
import io.github.mortuusars.envelope.util.bugger.test.Test;
import io.github.mortuusars.envelope.world.item.crafting.mail.MailCraftingRecipe;
import io.github.mortuusars.envelope.world.item.crafting.mail.MailRecipeInput;
import io.github.mortuusars.envelope.world.mail.MailService;
import io.github.mortuusars.envelope.world.mail.address.Address;
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

    // --

    private MailCraftingRecipe recipe(Ingredient... ingredients) {
        return new MailCraftingRecipe(
              MailService.of(server.overworld()).getAddress(),
              NonNullList.of(Ingredient.EMPTY, ingredients),
              new ItemStack(Items.BARRIER),
              0);
    }

    private MailRecipeInput input(ItemStack... items) {
        return new MailRecipeInput(server.overworld().getEnvelopeMailService(), Address.UNKNOWN, Arrays.stream(items).toList());
    }

    private boolean matches(MailCraftingRecipe recipe, MailRecipeInput input) {
        return recipe.matches(input, server.overworld());
    }
}
