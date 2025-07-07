package io.github.mortuusars.envelope.api.mail;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.Optional;

public record Address(String name, Optional<Component> displayName) {
    public static final Address MAIL_SERVICE = new Address("<Mail Service>",
            Optional.of(Component.translatable("address.envelope.mail_service")));

    public static final Codec<Address> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.string(1, 256).fieldOf("name").forGetter(Address::name),
            ComponentSerialization.CODEC.optionalFieldOf("display_name").forGetter(Address::displayName)
    ).apply(instance, Address::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, Address> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, Address::name,
            ByteBufCodecs.optional(ComponentSerialization.STREAM_CODEC), Address::displayName,
            Address::new
    );
}
