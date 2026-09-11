package io.github.mortuusars.envelope.util;

import com.mojang.serialization.Codec;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Supplier;

public abstract class PersistentData {
    protected boolean dirty;

    public void setDirty() {
        setDirty(true);
    }

    public void setDirty(boolean dirty) {
        this.dirty = dirty;
    }

    public boolean isDirty() {
        return this.dirty;
    }

    public record Type<T extends PersistentData>(ResourceLocation id, Supplier<T> constructor, Codec<T> codec) {}
}
