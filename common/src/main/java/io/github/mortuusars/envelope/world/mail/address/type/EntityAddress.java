package io.github.mortuusars.envelope.world.mail.address.type;

import com.mojang.serialization.*;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.world.mail.address.Address;
import io.github.mortuusars.envelope.world.mail.entity.MailEntity;
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

public class EntityAddress implements Address {
    public static final MapCodec<EntityAddress> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
          RegistryFixedCodec.create(Envelope.Registries.MAIL_ENTITY).fieldOf("entity").forGetter(EntityAddress::getEntityHolder)
    ).apply(i, EntityAddress::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, EntityAddress> STREAM_CODEC = StreamCodec.composite(
          ByteBufCodecs.holderRegistry(Envelope.Registries.MAIL_ENTITY), EntityAddress::getEntityHolder,
          EntityAddress::new
    );

    private final Holder<MailEntity> entity;

    public EntityAddress(Holder<MailEntity> entity) {
        this.entity = entity;
    }

    @Override
    public Type getType() {
        return Type.ENTITY;
    }

    public Holder<MailEntity> getEntityHolder() {
        return entity;
    }

    public MailEntity getEntity() {
        return getEntityHolder().value();
    }

    @Override
    public String getString() {
        return getEntity().name().getString();
    }

    @Override
    public MutableComponent getComponent() {
        return getEntity().name().copy();
    }

    // --

    @SuppressWarnings("deprecation")
    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        EntityAddress that = (EntityAddress) o;
        return entity.is(that.getEntityHolder());
    }

    @Override
    public int hashCode() {
        return ("e" + getString().toLowerCase(Locale.ROOT)).hashCode();
    }

    @Override
    public @NotNull String toString() {
        return "Entity[" + getString() + "]";
    }

    // --

    public static EntityAddress get(RegistryAccess access, ResourceKey<MailEntity> key) {
        return new EntityAddress(getHolderOrThrow(access, key));
    }

    public static Holder.Reference<MailEntity> getHolderOrThrow(RegistryAccess access, ResourceKey<MailEntity> key) {
        return access.registryOrThrow(Envelope.Registries.MAIL_ENTITY).getHolderOrThrow(key);
    }

    public static Optional<Holder.Reference<MailEntity>> getHolder(RegistryAccess access, ResourceKey<MailEntity> key) {
        return access.registryOrThrow(Envelope.Registries.MAIL_ENTITY).getHolder(key);
    }
}