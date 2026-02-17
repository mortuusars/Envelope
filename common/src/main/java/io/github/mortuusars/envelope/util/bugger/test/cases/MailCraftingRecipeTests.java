package io.github.mortuusars.envelope.util.bugger.test.cases;

import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.util.bugger.test.BuggerTests;
import io.github.mortuusars.envelope.util.bugger.test.Test;
import io.github.mortuusars.envelope.world.inventory.StackIngredient;
import io.github.mortuusars.envelope.world.item.crafting.MailCraftingRecipe;
import io.github.mortuusars.envelope.world.item.crafting.PackageRecipeInput;
import io.github.mortuusars.envelope.world.mail.MailService;
import net.minecraft.Util;
import net.minecraft.core.component.DataComponentPredicate;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Unit;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.DyedItemColor;

import java.util.Arrays;
import java.util.List;

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
                    new StackIngredient(Items.EMERALD, 12),
                    new StackIngredient(Items.DIAMOND, 6)
              ),
              input(
                    new ItemStack(Items.EMERALD, 12),
                    new ItemStack(Items.DIAMOND, 6)
              ))));

        add("MailCraftingRecipe_MatchesIfCorrectMultipleOfSame", Test.isTrue(() -> matches(
              recipe(
                    new StackIngredient(Items.EMERALD, 12),
                    new StackIngredient(Items.DIAMOND, 6),
                    new StackIngredient(Items.DIAMOND, 6),
                    new StackIngredient(Items.COPPER_INGOT, 6)
              ),
              input(
                    new ItemStack(Items.EMERALD, 12),
                    new ItemStack(Items.DIAMOND, 6),
                    new ItemStack(Items.DIAMOND, 6),
                    new ItemStack(Items.COPPER_INGOT, 6)
              ))));

        add("MailCraftingRecipe_MatchesIfMore", Test.isTrue(() -> matches(
              recipe(
                    new StackIngredient(Items.EMERALD, 12),
                    new StackIngredient(Items.DIAMOND, 6)
              ),
              input(
                    new ItemStack(Items.EMERALD, 30),
                    new ItemStack(Items.DIAMOND, 30)
              )
        )));

        add("MailCraftingRecipe_MatchesIfMoreMultipleOfSame", Test.isTrue(() -> matches(
              recipe(
                    new StackIngredient(Items.EMERALD, 12),
                    new StackIngredient(Items.DIAMOND, 6),
                    new StackIngredient(Items.DIAMOND, 6),
                    new StackIngredient(Items.COPPER_INGOT, 6)
              ),
              input(
                    new ItemStack(Items.EMERALD, 30),
                    new ItemStack(Items.DIAMOND, 30),
                    new ItemStack(Items.DIAMOND, 30),
                    new ItemStack(Items.COPPER_INGOT, 30)
              ))));

        add("MailCraftingRecipe_MatchesWhenOutOfOrder", Test.isTrue(() -> matches(
              recipe(
                    new StackIngredient(Items.EMERALD, 12),
                    new StackIngredient(Items.DIAMOND, 6),
                    new StackIngredient(Items.DIAMOND, 6),
                    new StackIngredient(Items.COPPER_INGOT, 6)
              ),
              input(
                    new ItemStack(Items.COPPER_INGOT, 30),
                    new ItemStack(Items.DIAMOND, 30),
                    new ItemStack(Items.EMERALD, 30),
                    new ItemStack(Items.DIAMOND, 30)
              ))));

        add("MailCraftingRecipe_MatchesWithEmptyItems", Test.isTrue(() -> matches(
              recipe(
                    new StackIngredient(Items.EMERALD, 12),
                    new StackIngredient(Items.DIAMOND, 6)
              ),
              input(
                    ItemStack.EMPTY,
                    ItemStack.EMPTY,
                    new ItemStack(Items.DIAMOND, 6),
                    new ItemStack(Items.EMERALD, 12),
                    ItemStack.EMPTY
              )
        )));

        add("MailCraftingRecipe_MatchesWithComponents", Test.isTrue(() -> matches(
              recipe(
                    new StackIngredient(Items.EMERALD, 12, DataComponentPredicate.builder()
                          .expect(Envelope.DataComponents.LETTER_TATTERED, Unit.INSTANCE)
                          .build()),
                    new StackIngredient(Items.DIAMOND, 6)
              ),
              input(
                    new ItemStack(Items.DIAMOND, 6),
                    Util.make(() -> {
                        ItemStack stack = new ItemStack(Items.EMERALD, 12);
                        stack.set(Envelope.DataComponents.LETTER_TATTERED, Unit.INSTANCE);
                        stack.set(DataComponents.DYED_COLOR, new DyedItemColor(0xFF1122, true));
                        return stack;
                    })
              )
        )));

        // --

        add("MailCraftingRecipe_Matching_FailsIfInputEmpty", Test.isFalse(() -> matches(
              recipe(
                    new StackIngredient(Items.EMERALD, 12),
                    new StackIngredient(Items.DIAMOND, 6)
              ),
              input(
                    ItemStack.EMPTY,
                    ItemStack.EMPTY
              )
        )));

        add("MailCraftingRecipe_Matching_FailsIfNotEnough", Test.isFalse(() -> matches(
              recipe(
                    new StackIngredient(Items.EMERALD, 12),
                    new StackIngredient(Items.DIAMOND, 6)
              ),
              input(
                    new ItemStack(Items.EMERALD, 12),
                    new ItemStack(Items.DIAMOND, 1)
              )
        )));

        add("MailCraftingRecipe_Matching_FailsIfLessInputs", Test.isFalse(() -> matches(
              recipe(
                    new StackIngredient(Items.EMERALD, 12),
                    new StackIngredient(Items.DIAMOND, 6),
                    new StackIngredient(Items.ACACIA_BOAT)
              ),
              input(
                    new ItemStack(Items.EMERALD, 12),
                    new ItemStack(Items.DIAMOND, 1)
              )
        )));

        add("MailCraftingRecipe_Matching_FailsIfMoreInputs", Test.isFalse(() -> matches(
              recipe(
                    new StackIngredient(Items.EMERALD, 12),
                    new StackIngredient(Items.DIAMOND, 6)
              ),
              input(
                    new ItemStack(Items.EMERALD, 12),
                    new ItemStack(Items.DIAMOND, 1),
                    new ItemStack(Items.ACACIA_BOAT)
              )
        )));

        add("MailCraftingRecipe_Matching_FailsIfMoreInputsOfCorrectItem", Test.isFalse(() ->
              matches(
              recipe(
                    new StackIngredient(Items.EMERALD, 12),
                    new StackIngredient(Items.DIAMOND, 6)
              ),
              input(
                    new ItemStack(Items.EMERALD, 12),
                    new ItemStack(Items.DIAMOND, 6),
                    new ItemStack(Items.DIAMOND, 3)
              )
        )));
    }

    private void consuming() {
        add("MailCraftingRecipe_ConsumesCorrectly", Test.isTrue(() -> {
            MailCraftingRecipe recipe = recipe(
                  new StackIngredient(Items.EMERALD, 12),
                  new StackIngredient(Items.DIAMOND, 6)
            );

            PackageRecipeInput input = input(
                  new ItemStack(Items.EMERALD, 12),
                  new ItemStack(Items.DIAMOND, 6)
            );

            return recipe.consumeOnce(input).stream()
                  .allMatch(ItemStack::isEmpty);
        }));

        add("MailCraftingRecipe_ConsumesAndReturnsRemainder", Test.isTrue(() -> {
            MailCraftingRecipe recipe = recipe(
                  new StackIngredient(Items.EMERALD, 12),
                  new StackIngredient(Items.DIAMOND, 6)
            );

            PackageRecipeInput input = input(
                  new ItemStack(Items.EMERALD, 12),
                  new ItemStack(Items.DIAMOND, 8)
            );

            return recipe.consumeOnce(input).stream()
                  .filter(stack -> !stack.isEmpty())
                  .findAny()
                  .map(stack -> stack.getItem() == Items.DIAMOND && stack.getCount() == 2)
                  .orElse(false);
        }));

        add("MailCraftingRecipe_ConsumesOnlyForOneOperation", Test.isTrue(() -> {
            MailCraftingRecipe recipe = recipe(
                  new StackIngredient(Items.EMERALD, 12),
                  new StackIngredient(Items.DIAMOND, 6)
            );

            PackageRecipeInput input = input(
                  new ItemStack(Items.EMERALD, 36),
                  new ItemStack(Items.DIAMOND, 18)
            );

            List<ItemStack> remainder = recipe.consumeOnce(input);

            return remainder.getFirst().getCount() == 24 && remainder.get(1).getCount() == 12;
        }));
    }

    // --

    private MailCraftingRecipe recipe(StackIngredient... ingredients) {
        return new MailCraftingRecipe(
              MailService.of(server.overworld()).getAddress(),
              Arrays.stream(ingredients).toList(),
              new ItemStack(Items.BARRIER),
              0);
    }

    private PackageRecipeInput input(ItemStack... items) {
        return PackageRecipeInput.of(Arrays.stream(items).toList());
    }

    private boolean matches(MailCraftingRecipe recipe, PackageRecipeInput input) {
        return recipe.matches(input, server.overworld());
    }
}
