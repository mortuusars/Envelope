package io.github.mortuusars.envelope.world.mail.address.type;

import com.mojang.serialization.*;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.world.mail.address.Address;
import io.github.mortuusars.envelope.world.mail.address.EntityAddressDefinition;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.Objects;

public class EntityAddress implements Address.Realizable {
    public static final MapCodec<EntityAddress> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
          ResourceKey.codec(Envelope.Registries.ENTITY_ADDRESS).fieldOf("key").forGetter(EntityAddress::getKey)
    ).apply(i, EntityAddress::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, EntityAddress> STREAM_CODEC = StreamCodec.composite(
          ResourceKey.streamCodec(Envelope.Registries.ENTITY_ADDRESS), EntityAddress::getKey,
          EntityAddress::new
    );

    private final ResourceKey<EntityAddressDefinition> key;

    public EntityAddress(ResourceKey<EntityAddressDefinition> key) {
        this.key = key;
    }

    public EntityAddress(ResourceLocation key) {
        this(ResourceKey.create(Envelope.Registries.ENTITY_ADDRESS, key));
    }

    @Override
    public Type getType() {
        return Type.ENTITY;
    }

    @Override
    public String getId() {
        return key.location().toString();
    }

    public ResourceKey<EntityAddressDefinition> getKey() {
        return key;
    }

    @Override
    public Address.Realized realize(RegistryAccess access) {
        Registry<EntityAddressDefinition> registry = access.registryOrThrow(Envelope.Registries.ENTITY_ADDRESS);
        @Nullable EntityAddressDefinition definition = registry.get(key);
        return definition != null
              ? new Realized(key, definition)
              : Address.UNKNOWN;
    }

    // --

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        EntityAddress that = (EntityAddress) o;
        return Objects.equals(key, that.key);
    }

    @Override
    public int hashCode() {
        return ("e" + getId().toLowerCase(Locale.ROOT)).hashCode();
    }

    @Override
    public @NotNull String toString() {
        return "Entity[" + getId() + "]";
    }

    // --

    public static class Realized extends EntityAddress implements Address.Realized {
        private final EntityAddressDefinition definition;

        public Realized(ResourceKey<EntityAddressDefinition> key, EntityAddressDefinition definition) {
            super(key);
            this.definition = definition;
        }

        public Realized(ResourceLocation key, EntityAddressDefinition definition) {
            super(key);
            this.definition = definition;
        }

        @Override
        public String getDisplayString() {
            return getDisplayComponent().getString();
        }

        @Override
        public MutableComponent getDisplayComponent() {
            return definition.displayName().copy();
        }
    }
}
