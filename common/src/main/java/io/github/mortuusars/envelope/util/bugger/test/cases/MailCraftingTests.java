package io.github.mortuusars.envelope.util.bugger.test.cases;

import io.github.mortuusars.envelope.util.bugger.test.BuggerTests;
import io.github.mortuusars.envelope.util.bugger.test.Test;
import io.github.mortuusars.envelope.world.item.component.PackageContents;
import io.github.mortuusars.envelope.world.item.crafting.mail.MailCrafting;
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
            MailCrafting.Result result = MailCrafting.craft(server.overworld(),
                  recipe(
                        Ingredient.of(Items.EMERALD),
                        Ingredient.of(Items.DIAMOND),
                        Ingredient.of(Items.DIAMOND)
                  ),
                  input(
                        new ItemStack(Items.EMERALD, 6),
                        new ItemStack(Items.DIAMOND, 6),
                        new ItemStack(Items.DIAMOND, 6)
                  ),
                  Address.UNKNOWN);

            return result.remainder().isEmpty() && result.output().size() == 1;
        }));

        add("MailCrafting_CraftsWithRemainder", Test.isTrue(() -> {
            MailCrafting.Result result = MailCrafting.craft(server.overworld(),
                  recipe(
                        Ingredient.of(Items.EMERALD),
                        Ingredient.of(Items.DIAMOND),
                        Ingredient.of(Items.DIAMOND)
                  ),
                  input(
                        new ItemStack(Items.EMERALD, 16),
                        new ItemStack(Items.DIAMOND, 6),
                        new ItemStack(Items.DIAMOND, 6)
                  ),
                  Address.UNKNOWN);

            return !result.remainder().isEmpty()
                  && result.remainder().getItems().stream().filter(stack -> !stack.isEmpty()).toList().getFirst().getCount() == 10
                  && result.output().size() == 1;
        }));

        add("MailCrafting_CraftingReturnsCorrectTotalExperience", Test.isTrue(() -> {
            MailCrafting.Result result = MailCrafting.craft(server.overworld(),
                  recipe(
                        Ingredient.of(Items.EMERALD),
                        Ingredient.of(Items.DIAMOND),
                        Ingredient.of(Items.DIAMOND)
                  ),
                  input(
                        new ItemStack(Items.EMERALD, 3),
                        new ItemStack(Items.DIAMOND, 3),
                        new ItemStack(Items.DIAMOND, 3)
                  ),
                  Address.UNKNOWN);

            return result.remainder().isEmpty() && result.experience() == RECIPE_EXPERIENCE * 3;
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

    private PackageContents input(ItemStack... items) {
        return new PackageContents(Arrays.stream(items).toList());
    }
}
