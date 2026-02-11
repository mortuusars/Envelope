package io.github.mortuusars.envelope.world.mail.address.type;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.mortuusars.envelope.world.mail.address.Address;
import io.github.mortuusars.envelope.world.mail.address.AddressValidation;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

public final class CustomAddress implements Address {
    public static final MapCodec<CustomAddress> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
          Address.ID_CODEC.fieldOf("id").forGetter(CustomAddress::getId),
          ComponentSerialization.CODEC.optionalFieldOf("display_name").forGetter(CustomAddress::getDisplayName)
    ).apply(instance, CustomAddress::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, CustomAddress> STREAM_CODEC = StreamCodec.composite(
          ByteBufCodecs.STRING_UTF8, CustomAddress::getId,
          ByteBufCodecs.optional(ComponentSerialization.STREAM_CODEC), CustomAddress::getDisplayName,
          CustomAddress::new
    );

    private final String id;
    private final Optional<Component> displayName;

    public CustomAddress(String id, Optional<Component> displayName) {
        AddressValidation.validateId(id).getOrThrow();
        this.id = id;
        this.displayName = displayName;
    }

    public CustomAddress(String id, @Nullable Component displayName) {
        this(id, Optional.ofNullable(displayName));
    }

    public CustomAddress(String id) {
        this(id, Optional.empty());
    }

    @Override
    public Type getType() {
        return Type.CUSTOM;
    }

    @Override
    public String getId() {
        return id;
    }

    public Optional<Component> getDisplayName() {
        return displayName;
    }

    @Override
    public MutableComponent getDisplayComponent() {
        return getDisplayName().map(Component::copy).orElse(Component.literal(id));
    }

    @Override
    public String getDisplayString() {
        return getDisplayName().map(Component::getString).orElse(id);
    }

    // --

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        CustomAddress that = (CustomAddress) o;
        return Objects.equals(id, that.id) && Objects.equals(displayName, that.displayName);
    }

    @Override
    public int hashCode() {
        return ("c" + id.toLowerCase(Locale.ROOT)).hashCode();
    }

    @Override
    public @NotNull String toString() {
        return "Custom[" + id + "]";
    }
}