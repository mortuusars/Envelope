package io.github.mortuusars.envelope.world.mail.address;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.mortuusars.envelope.Envelope;
import net.minecraft.core.Holder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.RegistryFileCodec;

public record EntityAddressDefinition(String id, Component displayName) {
    public static final Codec<EntityAddressDefinition> DIRECT_CODEC = RecordCodecBuilder.create(i -> i.group(
          Codec.STRING.fieldOf("id").forGetter(EntityAddressDefinition::id),
          ComponentSerialization.CODEC.fieldOf("display_name").forGetter(EntityAddressDefinition::displayName)
    ).apply(i, EntityAddressDefinition::new));

    public static final Codec<Holder<EntityAddressDefinition>> CODEC =
          RegistryFileCodec.create(Envelope.Registries.ENTITY_ADDRESS, DIRECT_CODEC);

    public static final StreamCodec<RegistryFriendlyByteBuf, EntityAddressDefinition> STREAM_CODEC =
          StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, EntityAddressDefinition::id,
                ComponentSerialization.STREAM_CODEC, EntityAddressDefinition::displayName,
                EntityAddressDefinition::new
          );
}
