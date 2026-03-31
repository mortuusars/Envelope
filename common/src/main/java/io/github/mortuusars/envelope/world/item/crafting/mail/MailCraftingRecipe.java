package io.github.mortuusars.envelope.world.item.crafting.mail;

import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.world.mail.address.type.ServiceAddress;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import org.jetbrains.annotations.NotNull;

public class MailCraftingRecipe implements MailRecipe {
    private final ServiceAddress address;
    private final NonNullList<Ingredient> ingredients;
    private final ItemStack result;
    private final float experience;

    public MailCraftingRecipe(ServiceAddress address, NonNullList<Ingredient> ingredients, ItemStack result, float experience) {
        this.address = address;
        this.ingredients = ingredients;
        this.result = result;
        this.experience = experience;
    }

    @Override
    public ServiceAddress getAddress() {
        return address;
    }

    @Override
    public @NotNull NonNullList<Ingredient> getIngredients() {
        return ingredients;
    }

    public ItemStack getResult() {
        return result;
    }

    @Override
    public float getExperience() {
        return experience;
    }

    @Override
    public @NotNull ItemStack getResultItem(HolderLookup.Provider registries) {
        return result;
    }

    @Override
    public @NotNull RecipeSerializer<?> getSerializer() {
        return Envelope.RecipeSerializers.MAIL_CRAFTING.get();
    }

    @Override
    public String toString() {
        return getClass().getName() + "{" +
              "address=" + address +
              ", ingredients=" + ingredients +
              ", result=" + result +
              (experience > 0 ? ", experience=" + experience : "") +
              '}';
    }
}
