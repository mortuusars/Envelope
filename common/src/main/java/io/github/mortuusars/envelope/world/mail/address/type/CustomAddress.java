package io.github.mortuusars.envelope.world.mail.address.type;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.mortuusars.envelope.world.mail.address.Address;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.codec.StreamCodec;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.Objects;

public final class CustomAddress implements Address {
    public static final MapCodec<CustomAddress> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
          ComponentSerialization.CODEC.fieldOf("name").forGetter(CustomAddress::getComponent)
    ).apply(instance, CustomAddress::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, CustomAddress> STREAM_CODEC = StreamCodec.composite(
          ComponentSerialization.STREAM_CODEC, CustomAddress::getComponent,
          CustomAddress::new
    );

    private final Component name;

    public CustomAddress(@NotNull Component name) {
        this.name = Objects.requireNonNull(name);
    }

    @Override
    public Type getType() {
        return Type.CUSTOM;
    }

    @Override
    public String getString() {
        return getComponent().getString();
    }

    @Override
    public MutableComponent getComponent() {
        return name.copy();
    }

    // --

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        CustomAddress that = (CustomAddress) o;
        return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return ("c" + getString().toLowerCase(Locale.ROOT)).hashCode();
    }

    @Override
    public @NotNull String toString() {
        return "Custom[" + getString() + "]";
    }
}