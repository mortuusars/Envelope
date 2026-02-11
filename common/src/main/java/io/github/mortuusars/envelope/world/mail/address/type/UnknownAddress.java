package io.github.mortuusars.envelope.world.mail.address.type;

import io.github.mortuusars.envelope.world.mail.address.Address;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.jetbrains.annotations.NotNull;

public enum UnknownAddress implements Address {
    INSTANCE;

    @Override
    public Type getType() {
        return Type.UNKNOWN;
    }

    @Override
    public String getString() {
        return getComponent().getString();
    }

    @Override
    public MutableComponent getComponent() {
        return Component.translatable("address.envelope.unknown");
    }

    // --

    @Override
    public @NotNull String toString() {
        return "[Unknown]";
    }
}