package io.github.mortuusars.envelope.world.mail.address.type;

import com.mojang.serialization.*;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.world.mail.address.Address;
import io.github.mortuusars.envelope.world.mail.service.ServiceAddressDefinition;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.RegistryFixedCodec;
import net.minecraft.resources.ResourceKey;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.Optional;

public final class ServiceAddress implements Address {
    public static final MapCodec<ServiceAddress> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
          RegistryFixedCodec.create(Envelope.Registries.SERVICE_ADDRESS_DEFINITION)
                .fieldOf("definition")
                .forGetter(ServiceAddress::getDefinitionHolder)
    ).apply(i, ServiceAddress::new));

    public static final Codec<ServiceAddress> DEFINITION_CODEC = RegistryFixedCodec.create(Envelope.Registries.SERVICE_ADDRESS_DEFINITION)
          .xmap(ServiceAddress::new, ServiceAddress::getDefinitionHolder);

    public static final StreamCodec<RegistryFriendlyByteBuf, ServiceAddress> STREAM_CODEC = StreamCodec.composite(
          ByteBufCodecs.holderRegistry(Envelope.Registries.SERVICE_ADDRESS_DEFINITION), ServiceAddress::getDefinitionHolder,
          ServiceAddress::new
    );

    private final Holder<ServiceAddressDefinition> definition;

    public ServiceAddress(Holder<ServiceAddressDefinition> definition) {
        this.definition = definition;
    }

    @Override
    public Type getType() {
        return Type.SERVICE;
    }

    public Holder<ServiceAddressDefinition> getDefinitionHolder() {
        return definition;
    }

    public ServiceAddressDefinition getDefinition() {
        return getDefinitionHolder().value();
    }

    @Override
    public String getString() {
        return getDefinition().name().getString();
    }

    @Override
    public MutableComponent getComponent() {
        return getDefinition().name().copy();
    }

    // --

    @SuppressWarnings("deprecation")
    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        ServiceAddress that = (ServiceAddress) o;
        return definition.is(that.getDefinitionHolder());
    }

    @Override
    public int hashCode() {
        return ("s" + getString().toLowerCase(Locale.ROOT)).hashCode();
    }

    @Override
    public @NotNull String toString() {
        return "Service[" + getString() + "]";
    }

    // --

    public static ServiceAddress getOrThrow(RegistryAccess access, ResourceKey<ServiceAddressDefinition> key) {
        return new ServiceAddress(getHolderOrThrow(access, key));
    }

    public static Optional<ServiceAddress> get(RegistryAccess access, ResourceKey<ServiceAddressDefinition> key) {
        return getHolder(access, key).map(ServiceAddress::new);
    }

    public static Holder.Reference<ServiceAddressDefinition> getHolderOrThrow(RegistryAccess access, ResourceKey<ServiceAddressDefinition> key) {
        return access.registryOrThrow(Envelope.Registries.SERVICE_ADDRESS_DEFINITION).getHolderOrThrow(key);
    }

    public static Optional<Holder.Reference<ServiceAddressDefinition>> getHolder(RegistryAccess access, ResourceKey<ServiceAddressDefinition> key) {
        return access.registryOrThrow(Envelope.Registries.SERVICE_ADDRESS_DEFINITION).getHolder(key);
    }
}