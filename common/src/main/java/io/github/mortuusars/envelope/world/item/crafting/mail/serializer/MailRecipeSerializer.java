package io.github.mortuusars.envelope.world.item.crafting.mail.serializer;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.world.item.component.PackageContents;
import io.github.mortuusars.envelope.world.item.crafting.mail.MailCraftingRecipe;
import io.github.mortuusars.envelope.world.item.crafting.mail.MailingRecipe;
import io.github.mortuusars.envelope.world.mail.address.type.EntityAddress;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.RegistryFixedCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

public class MailRecipeSerializer<T extends MailCraftingRecipe> implements RecipeSerializer<T> {
    private final MapCodec<T> codec;
    private final StreamCodec<RegistryFriendlyByteBuf, T> streamCodec;

    public MailRecipeSerializer(Constructor<T> constructor) {
        this.codec = RecordCodecBuilder.mapCodec(i -> i.group(
              RegistryFixedCodec.create(Envelope.Registries.MAIL_ENTITY)
                    .xmap(EntityAddress::new, EntityAddress::getEntityHolder)
                    .fieldOf("entity")
                    .forGetter(MailCraftingRecipe::getAddress),
              Ingredient.CODEC_NONEMPTY
                    .listOf()
                    .fieldOf("ingredients")
                    .flatXmap(
                          list -> {
                              Ingredient[] ingredients = list.stream().filter(ingredient -> !ingredient.isEmpty()).toArray(Ingredient[]::new);
                              if (ingredients.length == 0) {
                                  return DataResult.error(() -> "No ingredients for mail crafting recipe");
                              } else {
                                  return ingredients.length > PackageContents.SLOTS
                                        ? DataResult.error(() -> "Too many ingredients for mail crafting recipe")
                                        : DataResult.success(NonNullList.of(Ingredient.EMPTY, ingredients));
                              }
                          },
                          DataResult::success
                    )
                    .forGetter(MailCraftingRecipe::getIngredients),
              ItemStack.STRICT_CODEC
                    .fieldOf("result")
                    .forGetter(MailCraftingRecipe::getResult),
              Codec.FLOAT
                    .optionalFieldOf("experience", 0.0f)
                    .forGetter(MailCraftingRecipe::getExperience)
        ).apply(i, constructor::create));

        this.streamCodec = StreamCodec.composite(
              EntityAddress.STREAM_CODEC, MailCraftingRecipe::getAddress,
              Ingredient.CONTENTS_STREAM_CODEC
                    .apply(ByteBufCodecs.list(PackageContents.SLOTS))
                    .map(list ->
                                NonNullList.of(Ingredient.EMPTY, list.toArray(Ingredient[]::new)),
                          Function.identity()), MailCraftingRecipe::getIngredients,
              ItemStack.STREAM_CODEC, MailCraftingRecipe::getResult,
              ByteBufCodecs.FLOAT, MailCraftingRecipe::getExperience,
              constructor::create
        );
    }

    @Override
    public @NotNull MapCodec<T> codec() {
        return codec;
    }

    @Override
    public @NotNull StreamCodec<RegistryFriendlyByteBuf, T> streamCodec() {
        return streamCodec;
    }

    @FunctionalInterface
    public interface Constructor<T extends MailingRecipe> {
        T create(EntityAddress address, NonNullList<Ingredient> ingredients, ItemStack result, float experience);
    }
}
