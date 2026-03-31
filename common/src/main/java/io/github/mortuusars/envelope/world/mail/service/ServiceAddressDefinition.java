package io.github.mortuusars.envelope.world.mail.service;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.util.EnvelopeSymbols;
import io.github.mortuusars.envelope.world.mail.address.AddressLocation;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.RegistryFileCodec;

public record ServiceAddressDefinition(Component name, String icon, AddressLocation location) {
    public static final Codec<ServiceAddressDefinition> DIRECT_CODEC = RecordCodecBuilder.create(i -> i.group(
          ComponentSerialization.CODEC.fieldOf("name").forGetter(ServiceAddressDefinition::name),
          Codec.STRING.optionalFieldOf("icon", EnvelopeSymbols.ADDRESS_SERVICE).forGetter(ServiceAddressDefinition::icon),
          AddressLocation.CODEC.fieldOf("location").forGetter(ServiceAddressDefinition::location)
    ).apply(i, ServiceAddressDefinition::new));

    public static final Codec<Holder<ServiceAddressDefinition>> CODEC =
          RegistryFileCodec.create(Envelope.Registries.SERVICE_ADDRESS_DEFINITION, DIRECT_CODEC);
}
