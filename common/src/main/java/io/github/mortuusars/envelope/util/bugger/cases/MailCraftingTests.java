package io.github.mortuusars.envelope.util.bugger.cases;

import io.github.mortuusars.mortaar.bugger.test.BuggerTests;
import io.github.mortuusars.mortaar.bugger.test.Test;
import io.github.mortuusars.envelope.world.item.crafting.mail.MailRecipeInput;
import io.github.mortuusars.envelope.world.item.crafting.mail.Mailing;
import io.github.mortuusars.envelope.world.item.crafting.mail.MailCraftingRecipe;
import io.github.mortuusars.envelope.world.mail.MailService;
import io.github.mortuusars.envelope.world.mail.address.Address;
import net.minecraft.core.NonNullList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.Arrays;

public class MailCraftingTests extends BuggerTests {
    public static final int RECIPE_EXPERIENCE = 3;
    private final MinecraftServer server;

    public MailCraftingTests(MinecraftServer server) {
        this.server = server;

        add("MailCrafting_CraftsWithoutRemainder", Test.isTrue(() -> {
            Mailing.Result result = Mailing.craft(
                  recipe(
                        Ingredient.of(Items.EMERALD),
                        Ingredient.of(Items.DIAMOND),
                        Ingredient.of(Items.DIAMOND)
                  ),
                  input(
                        new ItemStack(Items.EMERALD, 6),
                        new ItemStack(Items.DIAMOND, 6),
                        new ItemStack(Items.DIAMOND, 6)
                  ));

            return result.input().isEmpty() && result.output().size() == 1 && result.output().getFirst().getCount() == 6;
        }));

        add("MailCrafting_CraftsWithRemainder", Test.isTrue(() -> {
            Mailing.Result result = Mailing.craft(
                  recipe(
                        Ingredient.of(Items.EMERALD),
                        Ingredient.of(Items.DIAMOND),
                        Ingredient.of(Items.DIAMOND)
                  ),
                  input(
                        new ItemStack(Items.EMERALD, 16),
                        new ItemStack(Items.DIAMOND, 6),
                        new ItemStack(Items.DIAMOND, 6)
                  ));

            return !result.input().isEmpty()
                  && result.input().items().stream().filter(stack -> !stack.isEmpty()).toList().getFirst().getCount() == 10
                  && result.output().size() == 1;
        }));

        add("MailCrafting_CraftingReturnsCorrectTotalExperience", Test.isTrue(() -> {
            Mailing.Result result = Mailing.craft(
                  recipe(
                        Ingredient.of(Items.EMERALD),
                        Ingredient.of(Items.DIAMOND),
                        Ingredient.of(Items.DIAMOND)
                  ),
                  input(
                        new ItemStack(Items.EMERALD, 3),
                        new ItemStack(Items.DIAMOND, 3),
                        new ItemStack(Items.DIAMOND, 3)
                  ));

            return result.input().isEmpty() && result.experience() == RECIPE_EXPERIENCE * 3;
        }));

        add("MailCrafting_ConsumesCorrectly", Test.isTrue(() -> {
            MailCraftingRecipe recipe = recipe(
                  Ingredient.of(Items.EMERALD),
                  Ingredient.of(Items.DIAMOND)
            );

            MailRecipeInput input = input(
                  new ItemStack(Items.EMERALD),
                  new ItemStack(Items.DIAMOND)
            );

            Mailing.consumeInputs(recipe, input);

            return input.isEmpty();
        }));

        add("MailCrafting_ConsumesAndReturnsRemainder", Test.isTrue(() -> {
            MailCraftingRecipe recipe = recipe(
                  Ingredient.of(Items.EMERALD),
                  Ingredient.of(Items.DIAMOND)
            );

            MailRecipeInput input = input(
                  new ItemStack(Items.EMERALD),
                  new ItemStack(Items.DIAMOND, 3)
            );

            Mailing.consumeInputs(recipe, input);

            return input.items()
                  .stream()
                  .filter(stack -> !stack.isEmpty())
                  .findAny()
                  .map(stack -> stack.getItem() == Items.DIAMOND && stack.getCount() == 2)
                  .orElse(false);
        }));

        add("MailCrafting_ConsumesOnlyForOneOperation", Test.isTrue(() -> {
            MailCraftingRecipe recipe = recipe(
                  Ingredient.of(Items.EMERALD),
                  Ingredient.of(Items.DIAMOND)
            );

            MailRecipeInput input = input(
                  new ItemStack(Items.EMERALD, 6),
                  new ItemStack(Items.DIAMOND, 4)
            );

            Mailing.consumeInputs(recipe, input);

            return input.items().getFirst().getCount() == 5 && input.items().get(1).getCount() == 3;
        }));
    }

    // --

    private MailCraftingRecipe recipe(Ingredient... ingredients) {
        return new MailCraftingRecipe(
              MailService.of(server.overworld()).getAddress(),
              NonNullList.of(Ingredient.EMPTY, ingredients),
              new ItemStack(Items.BARRIER),
              RECIPE_EXPERIENCE);
    }

    private MailRecipeInput input(ItemStack... items) {
        return new MailRecipeInput(server.overworld().getEnvelopeMailService(), Address.UNKNOWN, Arrays.stream(items).toList());
    }
}
