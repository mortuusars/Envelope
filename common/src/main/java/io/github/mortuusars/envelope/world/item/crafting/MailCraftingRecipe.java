package io.github.mortuusars.envelope.world.item.crafting;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.world.inventory.StackIngredient;
import io.github.mortuusars.envelope.world.mail.address.type.EntityAddress;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.RegistryFixedCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class MailCraftingRecipe implements MailRecipe {
    private final EntityAddress address;
    private final List<StackIngredient> ingredients;
    private final ItemStack result;

    public MailCraftingRecipe(EntityAddress address, List<StackIngredient> ingredients, ItemStack result) {
        this.address = address;
        this.ingredients = ingredients;
        this.result = result;
    }

    @Override
    public EntityAddress getEntityAddress() {
        return address;
    }

    public List<StackIngredient> getRequestedIngredients() {
        return ingredients;
    }

    public ItemStack getResult() {
        return result;
    }

    @Override
    public int getIngredientsCount() {
        return ingredients.size();
    }

    @Override
    public List<ItemStack> getIngredientStacks(int index) {
        return Arrays.stream(ingredients.get(index).stacks()).toList();
    }

    /* Easier to read method of matching, but slower by about 20% then new one.
    @Override
    public boolean matches(MailRecipeInput input, Level level) {
        List<ItemStack> inputs = input.getItems()
              .filter(stack -> !stack.isEmpty())
              .map(ItemStack::copy)
              .collect(Collectors.toCollection(ArrayList::new));

        if (inputs.size() != ingredients.size()) {
            return false;
        }

        for (StackIngredient ingredient : ingredients) {
            int requiredCount = ingredient.count();

            for (ItemStack stack : inputs) {
                if (stack.isEmpty()) {
                    continue;
                }

                if (ingredient.testWithoutCount(stack)) {
                    requiredCount -= stack.split(requiredCount).getCount();
                }

                if (requiredCount <= 0) {
                    break;
                }
            }

            if (requiredCount > 0) {
                return false;
            }
        }

        return true;
    }
    */

    @Override
    public boolean matches(PackageRecipeInput input, Level level) {
        int size = input.size();
        int[] remaining = new int[size];

        int nonEmptyInputs = 0;

        for (int i = 0; i < size; i++) {
            ItemStack stack = input.getItem(i);
            if (stack.isEmpty()) {
                remaining[i] = 0;

            } else {
                remaining[i] = stack.getCount();
                nonEmptyInputs++;
            }
        }

        if (nonEmptyInputs != ingredients.size()) {
            return false;
        }

        for (StackIngredient ingredient : ingredients) {
            int required = ingredient.count();

            for (int i = 0; i < size; i++) {
                if (remaining[i] <= 0) {
                    continue;
                }

                ItemStack stack = input.getItem(i);
                if (stack.isEmpty()) {
                    continue;
                }

                if (ingredient.testWithoutCount(stack)) {
                    int taken = Math.min(required, remaining[i]);
                    required -= taken;
                    remaining[i] -= taken;
                }

                if (required <= 0) {
                    break;
                }
            }

            if (required > 0) {
                return false;
            }
        }

        return true;
    }

    @Override
    public @NotNull ItemStack assemble(PackageRecipeInput input, HolderLookup.Provider registries) {
        return getResultItem(registries).copy();
    }

    @Override
    public List<ItemStack> consumeOnce(PackageRecipeInput input) {
        List<ItemStack> inputs = input.getItems()
              .map(ItemStack::copy)
              .collect(Collectors.toCollection(ArrayList::new));

        for (StackIngredient ingredient : ingredients) {
            int requiredCount = ingredient.count();

            for (ItemStack stack : inputs) {
                if (stack.isEmpty()) {
                    continue;
                }

                if (ingredient.testWithoutCount(stack)) {
                    requiredCount -= stack.split(requiredCount).getCount();
                }

                if (requiredCount <= 0) {
                    break;
                }
            }

            if (requiredCount > 0) {
                return inputs;
            }
        }

        return inputs;
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
        return "MailCraftingRecipe{" +
              "address=" + address +
              ", ingredients=" + ingredients +
              ", result=" + result +
              '}';
    }

    public record Serializer() implements RecipeSerializer<MailCraftingRecipe> {
        public static final MapCodec<MailCraftingRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
              RegistryFixedCodec.create(Envelope.Registries.ENTITY_ADDRESS)
                    .xmap(EntityAddress::new, EntityAddress::getEntityHolder)
                    .fieldOf("entity")
                    .forGetter(MailCraftingRecipe::getEntityAddress),
              StackIngredient.CODEC.listOf(1, 6)
                    .fieldOf("ingredients")
                    .forGetter(MailCraftingRecipe::getRequestedIngredients),
              ItemStack.STRICT_CODEC
                    .fieldOf("result")
                    .forGetter(MailCraftingRecipe::getResult)
        ).apply(i, MailCraftingRecipe::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, MailCraftingRecipe> STREAM_CODEC = StreamCodec.composite(
              EntityAddress.STREAM_CODEC, MailCraftingRecipe::getEntityAddress,
              StackIngredient.STREAM_CODEC.apply(ByteBufCodecs.list(6)), MailCraftingRecipe::getRequestedIngredients,
              ItemStack.STREAM_CODEC, MailCraftingRecipe::getResult,
              MailCraftingRecipe::new
        );

        @Override
        public @NotNull MapCodec<MailCraftingRecipe> codec() {
            return CODEC;
        }

        @Override
        public @NotNull StreamCodec<RegistryFriendlyByteBuf, MailCraftingRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
