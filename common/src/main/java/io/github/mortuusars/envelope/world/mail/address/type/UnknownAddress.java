package io.github.mortuusars.envelope.world.mail.address.type;

import io.github.mortuusars.envelope.world.mail.address.Address;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.jetbrains.annotations.NotNull;

public enum UnknownAddress implements Address.Presentable {
    INSTANCE;

    @Override
    public Type getType() {
        return Type.UNKNOWN;
    }

    @Override
    public String getId() {
        return "Unknown";
    }

    @Override
    public String getDisplayString() {
        return getDisplayComponent().getString();
    }

    @Override
    public MutableComponent getDisplayComponent() {
        return Component.translatable("address.envelope.unknown");
    }

    // --

    @Override
    public @NotNull String toString() {
        return "[Unknown]";
    }
}