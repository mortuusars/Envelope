package io.github.mortuusars.envelope.world.item.crafting.mail;

import io.github.mortuusars.envelope.world.mail.address.type.EntityAddress;
import net.minecraft.core.NonNullList;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;

public class MailCraftingRecipeBuilder extends MailRecipeBuilder {
    private final NonNullList<Ingredient> ingredients = NonNullList.create();
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

    public MailCraftingRecipeBuilder requires(Ingredient ingredient) {
        ingredients.add(ingredient);
        return this;
    }

    public MailCraftingRecipeBuilder requires(Ingredient ingredient, int quantity) {
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
