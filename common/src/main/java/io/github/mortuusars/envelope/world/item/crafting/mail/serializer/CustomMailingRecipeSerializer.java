package io.github.mortuusars.envelope.world.item.crafting.mail.serializer;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.world.item.crafting.mail.CustomMailRecipe;
import io.github.mortuusars.envelope.world.mail.address.type.ServiceAddress;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.RegistryFixedCodec;
import net.minecraft.world.item.crafting.RecipeSerializer;
import org.jetbrains.annotations.NotNull;

public class CustomMailingRecipeSerializer<T extends CustomMailRecipe> implements RecipeSerializer<T> {
    private final MapCodec<T> codec;
    private final StreamCodec<RegistryFriendlyByteBuf, T> streamCodec;

    public CustomMailingRecipeSerializer(Constructor<T> constructor) {
        this.codec = RecordCodecBuilder.mapCodec(i -> i.group(
              RegistryFixedCodec.create(Envelope.Registries.SERVICE_ADDRESS_DEFINITION)
                    .xmap(ServiceAddress::new, ServiceAddress::getDefinitionHolder)
                    .fieldOf("address")
                    .forGetter(CustomMailRecipe::getAddress)
        ).apply(i, constructor::create));

        this.streamCodec = StreamCodec.composite(
              ServiceAddress.STREAM_CODEC, CustomMailRecipe::getAddress,
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
    public interface Constructor<T extends CustomMailRecipe> {
        T create(ServiceAddress address);
    }
}
