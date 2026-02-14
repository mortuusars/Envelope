package io.github.mortuusars.envelope.world.mail.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.world.mail.address.AddressLocation;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.RegistryFileCodec;

public record MailEntity(Component name, AddressLocation location) {
    public static final Codec<MailEntity> DIRECT_CODEC = RecordCodecBuilder.create(i -> i.group(
          ComponentSerialization.CODEC.fieldOf("name").forGetter(MailEntity::name),
          AddressLocation.CODEC.fieldOf("location").forGetter(MailEntity::location)
    ).apply(i, MailEntity::new));

    public static final Codec<Holder<MailEntity>> CODEC =
          RegistryFileCodec.create(Envelope.Registries.MAIL_ENTITY, DIRECT_CODEC);
}
