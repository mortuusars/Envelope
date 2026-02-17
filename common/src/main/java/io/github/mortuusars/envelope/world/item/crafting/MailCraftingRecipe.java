package io.github.mortuusars.envelope.world.item.crafting;

import com.mojang.serialization.Codec;
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
import java.util.BitSet;
import java.util.List;

public class MailCraftingRecipe implements MailRecipe {
    private final EntityAddress address;
    private final List<StackIngredient> ingredients;
    private final ItemStack result;
    private final float experience;

    public MailCraftingRecipe(EntityAddress address, List<StackIngredient> ingredients, ItemStack result, float experience) {
        this.address = address;
        this.ingredients = ingredients;
        this.result = result;
        this.experience = experience;
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

    public float getExperience() {
        return experience;
    }

    @Override
    public int getIngredientsCount() {
        return ingredients.size();
    }

    @Override
    public List<ItemStack> getIngredientStacks(int index) {
        return Arrays.stream(ingredients.get(index).stacks()).toList();
    }

    @Override
    public boolean matches(PackageRecipeInput input, Level level) {
        BitSet matchedSlots = new BitSet();

        for (StackIngredient ingredient : ingredients) {
            for (int slot = 0; slot < input.size(); slot++) {
                if (matchedSlots.get(slot)) {
                    continue;
                }

                ItemStack stack = input.getItem(slot);

                if (ingredient.testIgnoreCount(stack) && ingredient.count() <= stack.getCount()) {
                    matchedSlots.set(slot);
                    break;
                }
            }
        }

        int matchedCount = matchedSlots.cardinality();
        return input.size() == matchedCount && ingredients.size() == matchedCount;
    }

    @Override
    public @NotNull ItemStack assemble(PackageRecipeInput input, HolderLookup.Provider registries) {
        return getResultItem(registries).copy();
    }

    @Override
    public List<ItemStack> consumeOnce(PackageRecipeInput input) {
        BitSet processedSlots = new BitSet();

        List<ItemStack> result = new ArrayList<>();

        for (StackIngredient ingredient : ingredients) {
            for (int slot = 0; slot < input.size(); slot++) {
                if (processedSlots.get(slot)) {
                    continue;
                }

                ItemStack stack = input.getItem(slot);

                if (ingredient.testIgnoreCount(stack) && ingredient.count() <= stack.getCount()) {
                    result.add(stack.copyWithCount(stack.getCount() - ingredient.count()));
                    processedSlots.set(slot);
                    break;
                }
            }
        }

        return result;
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
              (experience > 0 ? ", experience=" + experience : "") +
              '}';
    }

    public record Serializer() implements RecipeSerializer<MailCraftingRecipe> {
        public static final MapCodec<MailCraftingRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
              RegistryFixedCodec.create(Envelope.Registries.MAIL_ENTITY)
                    .xmap(EntityAddress::new, EntityAddress::getEntityHolder)
                    .fieldOf("entity")
                    .forGetter(MailCraftingRecipe::getEntityAddress),
              StackIngredient.CODEC.listOf(1, 6)
                    .fieldOf("ingredients")
                    .forGetter(MailCraftingRecipe::getRequestedIngredients),
              ItemStack.STRICT_CODEC
                    .fieldOf("result")
                    .forGetter(MailCraftingRecipe::getResult),
              Codec.FLOAT
                    .optionalFieldOf("experience", 0.0f)
                    .forGetter(MailCraftingRecipe::getExperience)
        ).apply(i, MailCraftingRecipe::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, MailCraftingRecipe> STREAM_CODEC = StreamCodec.composite(
              EntityAddress.STREAM_CODEC, MailCraftingRecipe::getEntityAddress,
              StackIngredient.STREAM_CODEC.apply(ByteBufCodecs.list(6)), MailCraftingRecipe::getRequestedIngredients,
              ItemStack.STREAM_CODEC, MailCraftingRecipe::getResult,
              ByteBufCodecs.FLOAT, MailCraftingRecipe::getExperience,
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
