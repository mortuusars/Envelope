package io.github.mortuusars.envelope.world.item.crafting.mail.serializer;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.util.EnvelopeCodecs;
import io.github.mortuusars.envelope.world.item.component.PackageContents;
import io.github.mortuusars.envelope.world.item.crafting.mail.MailCraftingRecipe;
import io.github.mortuusars.envelope.world.item.crafting.mail.MailRecipe;
import io.github.mortuusars.envelope.world.mail.address.type.ServiceAddress;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

public class MailRecipeSerializer<T extends MailCraftingRecipe> implements RecipeSerializer<T> {
    private final MapCodec<T> codec;
    private final StreamCodec<RegistryFriendlyByteBuf, T> streamCodec;

    public MailRecipeSerializer(Constructor<T> constructor) {
        this.codec = RecordCodecBuilder.mapCodec(i -> i.group(
              ServiceAddress.DEFINITION_CODEC
                    .fieldOf("address")
                    .forGetter(MailCraftingRecipe::getAddress),
              EnvelopeCodecs.recipeIngredients(PackageContents.SLOTS, Envelope.RecipeTypes.MAILING.get())
                    .fieldOf("ingredients")
                    .forGetter(MailCraftingRecipe::getIngredients),
              ItemStack.STRICT_CODEC
                    .fieldOf("result")
                    .forGetter(MailCraftingRecipe::getResult),
              Codec.FLOAT
                    .optionalFieldOf("experience", 0.0f)
                    .forGetter(MailCraftingRecipe::getExperience)
        ).apply(i, constructor::create));

        this.streamCodec = StreamCodec.composite(
              ServiceAddress.STREAM_CODEC, MailCraftingRecipe::getAddress,
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
    public interface Constructor<T extends MailRecipe> {
        T create(ServiceAddress address, NonNullList<Ingredient> ingredients, ItemStack result, float experience);
    }
}
