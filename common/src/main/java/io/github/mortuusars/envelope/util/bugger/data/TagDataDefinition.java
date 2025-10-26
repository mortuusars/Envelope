package io.github.mortuusars.envelope.util.bugger.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Consumer;

public class TagDataDefinition extends DataDefinition<CompoundTag> {
    private final boolean accumulate;

    public TagDataDefinition(ResourceLocation id) {
        this(id, false);
    }

    public TagDataDefinition(ResourceLocation id, boolean accumulate) {
        super(id, CompoundTag.CODEC);
        this.accumulate = accumulate;
    }

    @Override
    public CompoundTag apply(CompoundTag oldValue, CompoundTag newValue) {
        if (accumulate) return oldValue.merge(newValue);
        return newValue;
    }

    public TagDataDefinition sendValues(Consumer<CompoundTag> tag) {
        CompoundTag compoundTag = new CompoundTag();
        tag.accept(compoundTag);
        send(compoundTag);
        return this;
    }
}
