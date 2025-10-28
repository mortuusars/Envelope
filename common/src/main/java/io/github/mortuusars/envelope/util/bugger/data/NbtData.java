package io.github.mortuusars.envelope.util.bugger.data;

import io.github.mortuusars.envelope.util.bugger.Bugger;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Consumer;

public class NbtData extends Data<CompoundTag> {
    private final boolean accumulate;

    public NbtData(ResourceLocation id) {
        this(id, false);
    }

    public NbtData(ResourceLocation id, boolean accumulate) {
        super(id, CompoundTag.CODEC);
        this.accumulate = accumulate;
    }

    @Override
    public CompoundTag apply(CompoundTag oldValue, CompoundTag newValue) {
        if (accumulate) return oldValue.merge(newValue);
        return newValue;
    }

    public void sendValues(Consumer<CompoundTag> tag) {
        if (Bugger.isEnabled()) {
            CompoundTag compoundTag = new CompoundTag();
            tag.accept(compoundTag);
            send(compoundTag);
        }
    }
}
