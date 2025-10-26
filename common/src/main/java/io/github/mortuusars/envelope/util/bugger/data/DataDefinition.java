package io.github.mortuusars.envelope.util.bugger.data;

import com.google.common.base.Preconditions;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import io.github.mortuusars.envelope.util.bugger.Bugger;
import io.github.mortuusars.envelope.util.bugger.BuggerData;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;

public class DataDefinition<T> {
    private final ResourceLocation id;
    private final Codec<T> codec;

    public DataDefinition(ResourceLocation id, Codec<T> codec) {
        @Nullable DataDefinition<?> definition = BuggerData.DEFINITIONS.get(id);
        Preconditions.checkArgument(definition == null || definition.codec().equals(codec),
              "Duplicate definition detected: '" + id + "'.");
        BuggerData.DEFINITIONS.put(id, this);
        this.id = id;
        this.codec = codec;
    }

    public ResourceLocation id() {
        return id;
    }

    public Codec<T> codec() {
        return codec;
    }

    public Optional<T> get() {
        return BuggerData.get(id);
    }

    public DataDefinition<T> send(@Nullable T value) {
        BuggerData.send(id, registryAccess -> encode(value, registryAccess));
        return this;
    }

    public T apply(T oldValue, T newValue) {
        return newValue;
    }

    // --

    public @Nullable Tag encode(@Nullable T value, RegistryAccess registryAccess) {
        if (value == null) return null;
        return codec().encodeStart(registryAccess.createSerializationContext(NbtOps.INSTANCE), value)
              .ifError(e -> Bugger.LOGGER.error("Cannot encode '{}' bugger data: '{}'. Value: '{}'", id, e.message(), value))
              .result()
              .orElse(null);
    }

    public @Nullable T decode(Tag tag, RegistryAccess registryAccess) {
        return codec().decode(registryAccess.createSerializationContext(NbtOps.INSTANCE), tag)
              .ifError(e -> Bugger.LOGGER.error("Cannot decode '{}' bugger data: '{}'. Tag: '{}'", id, e.message(), tag))
              .result()
              .map(Pair::getFirst)
              .orElse(null);
    }

    // --

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (DataDefinition<?>) obj;
        return Objects.equals(this.id, that.id) &&
              Objects.equals(this.codec, that.codec);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, codec);
    }

    @Override
    public String toString() {
        return "Definition[" +
              "id=" + id + ", " +
              "codec=" + codec + ", " + ']';
    }
}
