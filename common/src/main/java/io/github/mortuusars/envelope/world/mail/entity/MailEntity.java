package io.github.mortuusars.envelope.world.mail.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.util.EnvelopeSymbols;
import io.github.mortuusars.envelope.world.mail.address.AddressLocation;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.RegistryFileCodec;

public record MailEntity(Component name, String icon, AddressLocation location) {
    public static final Codec<MailEntity> DIRECT_CODEC = RecordCodecBuilder.create(i -> i.group(
          ComponentSerialization.CODEC.fieldOf("name").forGetter(MailEntity::name),
          Codec.STRING.optionalFieldOf("icon", EnvelopeSymbols.ADDRESS_ENTITY).forGetter(MailEntity::icon),
          AddressLocation.CODEC.fieldOf("location").forGetter(MailEntity::location)
    ).apply(i, MailEntity::new));

    public static final Codec<Holder<MailEntity>> CODEC =
          RegistryFileCodec.create(Envelope.Registries.MAIL_ENTITY, DIRECT_CODEC);
}
