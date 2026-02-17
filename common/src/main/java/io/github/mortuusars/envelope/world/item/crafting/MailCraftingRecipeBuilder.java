package io.github.mortuusars.envelope.world.item.crafting;

import io.github.mortuusars.envelope.world.inventory.StackIngredient;
import io.github.mortuusars.envelope.world.mail.address.type.EntityAddress;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

import java.util.ArrayList;
import java.util.List;

public class MailCraftingRecipeBuilder extends MailRecipeBuilder {
    private final List<StackIngredient> ingredients = new ArrayList<>();
    private ItemStack result = ItemStack.EMPTY;
    private float experience;

    public MailCraftingRecipeBuilder(EntityAddress address) {
        super(address);
    }

    @Override
    public ItemStack getResult() {
        return result;
    }

    public MailCraftingRecipeBuilder forResult(ItemStack result) {
        this.result = result;
        return this;
    }

    public MailCraftingRecipeBuilder forResult(ItemLike item, int count) {
        this.result = new ItemStack(item, count);
        return this;
    }

    public MailCraftingRecipeBuilder forResult(ItemLike item) {
        this.result = new ItemStack(item, 1);
        return this;
    }

    public MailCraftingRecipeBuilder requires(StackIngredient ingredient) {
        ingredients.add(ingredient);
        return this;
    }

    public MailCraftingRecipeBuilder requires(StackIngredient ingredient, int quantity) {
        for (int i = 0; i < quantity; i++) {
            ingredients.add(ingredient);
        }
        return this;
    }

    public MailCraftingRecipeBuilder experience(float experience) {
        this.experience = experience;
        return this;
    }

    public void save(RecipeOutput output, ResourceLocation id) {
        MailCraftingRecipe recipe = new MailCraftingRecipe(getAddress(), ingredients, result, experience);
        output.accept(id, recipe, null);
    }
}
