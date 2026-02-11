package io.github.mortuusars.envelope.world.mail.address.type;

import com.mojang.serialization.*;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.world.mail.address.Address;
import io.github.mortuusars.envelope.world.mail.address.EntityAddressDefinition;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.Objects;

public class EntityAddress implements Address.Representable {
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
    public Address.Presentable represent(RegistryAccess access) {
        return access.registryOrThrow(Envelope.Registries.ENTITY_ADDRESS)
              .getHolder(key)
              .map(holder -> new Presentable(key, holder))
              .map(Address.Presentable.class::cast)
              .orElse(Address.UNKNOWN);
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

    public static class Presentable extends EntityAddress implements Address.Presentable {
        private final Holder<EntityAddressDefinition> entity;

        public Presentable(ResourceKey<EntityAddressDefinition> key, Holder<EntityAddressDefinition> entity) {
            super(key);
            this.entity = entity;
        }

        @Override
        public String getDisplayString() {
            return getDisplayComponent().getString();
        }

        @Override
        public MutableComponent getDisplayComponent() {
            return entity.value().displayName().copy();
        }
    }
}