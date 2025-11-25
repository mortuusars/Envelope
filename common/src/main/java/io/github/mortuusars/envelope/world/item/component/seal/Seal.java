package io.github.mortuusars.envelope.world.item.component.seal;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.inventory.tooltip.TooltipComponent;

public record Seal(SealMaterial material, SealImpression impression, Component signature) implements TooltipComponent {
    public static final Codec<Seal> CODEC = RecordCodecBuilder.create(i -> i.group(
          SealMaterial.CODEC.optionalFieldOf("material", SealMaterials.RED_WAX).forGetter(Seal::material),
          SealImpression.CODEC.optionalFieldOf("impression", SealImpressions.DEFAULT).forGetter(Seal::impression),
          ComponentSerialization.CODEC.optionalFieldOf("signature", CommonComponents.EMPTY).forGetter(Seal::signature)
    ).apply(i, Seal::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, Seal> STREAM_CODEC = StreamCodec.composite(
          SealMaterial.STREAM_CODEC, Seal::material,
          SealImpression.STREAM_CODEC, Seal::impression,
          ComponentSerialization.STREAM_CODEC, Seal::signature,
          Seal::new
    );
}
