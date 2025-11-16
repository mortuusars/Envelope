package io.github.mortuusars.envelope.util.validation;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public interface Issue {
    String id();

    default MutableComponent getMessage() {
        return Component.translatable("gui.envelope.validation.issue." + id());
    }
}
