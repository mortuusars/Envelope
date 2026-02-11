package io.github.mortuusars.envelope.world.inventory.recipe;

import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.world.item.component.PackageContents;
import io.github.mortuusars.envelope.world.mail.address.type.EntityAddress;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.Function;

public class MailDeliveryRecipe implements Recipe<CraftingInput> {
    private final EntityAddress address;
    private final NonNullList<Ingredient> ingredients;
    private final ItemStack result;

    public MailDeliveryRecipe(EntityAddress address, NonNullList<Ingredient> ingredients, ItemStack result) {
        this.address = address;
        this.ingredients = ingredients;
        this.result = result;
    }

    public EntityAddress getAddress() {
        return address;
    }

    @Override
    public @NotNull NonNullList<Ingredient> getIngredients() {
        return ingredients;
    }

    public ItemStack getResult() {
        return result;
    }

    // --

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= PackageContents.SLOTS;
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        if (input.ingredientCount() != ingredients.size()) {
            return false;
        } else {
            return input.size() == 1 && ingredients.size() == 1
                  ? ingredients.getFirst().test(input.getItem(0))
                  : input.stackedContents().canCraft(this, null);
        }
    }

    @Override
    public @NotNull ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        return getResult().copy();
    }

    @Override
    public @NotNull ItemStack getResultItem(HolderLookup.Provider registries) {
        return result;
    }

    @Override
    public @NotNull RecipeType<?> getType() {
        return Envelope.RecipeTypes.MAIL_DELIVERY.get();
    }

    @Override
    public @NotNull RecipeSerializer<?> getSerializer() {

        return Envelope.RecipeSerializers.MAIL_DELIVERY.get();
    }

    public record Serializer() implements RecipeSerializer<MailDeliveryRecipe> {
        public static final MapCodec<MailDeliveryRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
              EntityAddress.DIRECT_CODEC.fieldOf("address").forGetter(MailDeliveryRecipe::getAddress),
              Ingredient.CODEC_NONEMPTY.listOf()
                    .fieldOf("ingredients")
                    .flatXmap(Serializer::validateIngredients, DataResult::success)
                    .forGetter(MailDeliveryRecipe::getIngredients),
              ItemStack.STRICT_CODEC
                    .fieldOf("result")
                    .flatXmap(Serializer::validateResult, DataResult::success)
                    .forGetter(MailDeliveryRecipe::getResult)
        ).apply(i, MailDeliveryRecipe::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, NonNullList<Ingredient>> INGREDIENTS_STREAM_CODEC =
              Ingredient.CONTENTS_STREAM_CODEC.apply(ByteBufCodecs.list(PackageContents.SLOTS))
                    .map(list -> {
                        NonNullList<Ingredient> ingredients = NonNullList.withSize(list.size(), Ingredient.EMPTY);
                        for (int i = 0; i < list.size(); i++) {
                            ingredients.set(i, list.get(i));
                        }
                        return ingredients;
                    }, Function.identity());

        public static final StreamCodec<RegistryFriendlyByteBuf, MailDeliveryRecipe> STREAM_CODEC = StreamCodec.composite(
              EntityAddress.STREAM_CODEC, MailDeliveryRecipe::getAddress,
              INGREDIENTS_STREAM_CODEC, MailDeliveryRecipe::getIngredients,
              ItemStack.STREAM_CODEC, MailDeliveryRecipe::getResult,
              MailDeliveryRecipe::new
        );

        @Override
        public @NotNull MapCodec<MailDeliveryRecipe> codec() {
            return CODEC;
        }

        @Override
        public @NotNull StreamCodec<RegistryFriendlyByteBuf, MailDeliveryRecipe> streamCodec() {
            return STREAM_CODEC;
        }

        // --

        private static DataResult<NonNullList<Ingredient>> validateIngredients(List<Ingredient> list) {
            Ingredient[] ingredients = list.stream().filter((ingredient) -> !ingredient.isEmpty()).toArray(Ingredient[]::new);
            if (ingredients.length == 0) {
                return DataResult.error(() -> "No ingredients for mail recipe");
            } else {
                return ingredients.length > PackageContents.SLOTS
                      ? DataResult.error(() -> "Too many ingredients for mail recipe. Max is " + PackageContents.SLOTS)
                      : DataResult.success(NonNullList.of(Ingredient.EMPTY, ingredients));
            }
        }

        private static DataResult<ItemStack> validateResult(ItemStack result) {
            return PackageContents.canHold(result)
                  ? DataResult.success(result)
                  : DataResult.error(() -> result + " is not valid for MailRecipe. Cannot fit in the Package.");
        }
    }
}
