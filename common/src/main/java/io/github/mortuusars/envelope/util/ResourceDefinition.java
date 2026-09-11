package io.github.mortuusars.envelope.util;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.function.Function;

public record ResourceDefinition<T, O>(ResourceKey<T> key, Function<Holder<T>, O> toObject)  {
    public static <T, O> ResourceDefinition<T, O> create(ResourceKey<? extends Registry<T>> registryKey,
                                                         ResourceLocation location, Function<Holder<T>, O> toObject) {
        return new ResourceDefinition<>(ResourceKey.create(registryKey, location), toObject);
    }

    public Holder.Reference<T> getHolderOrThrow(HolderLookup.Provider registries) {
        return registries.lookupOrThrow(key.registryKey()).getOrThrow(key);
    }

    public Optional<Holder.Reference<T>> getHolder(HolderLookup.Provider registries) {
        return registries.lookupOrThrow(key.registryKey()).get(key);
    }

    public O getOrThrow(HolderLookup.Provider registries) {
        return toObject.apply(getHolderOrThrow(registries));
    }

    public Optional<O> get(HolderLookup.Provider registries) {
        return getHolder(registries).map(toObject);
    }

    // -- ResourceKey

    public boolean isFor(ResourceKey<? extends Registry<?>> registryKey) {
        return key.isFor(registryKey);
    }

    public <E> Optional<ResourceKey<E>> cast(ResourceKey<? extends Registry<E>> registryKey) {
        return key.cast(registryKey);
    }

    public ResourceLocation location() {
        return key.location();
    }

    public ResourceLocation registry() {
        return key.registry();
    }

    public ResourceKey<Registry<T>> registryKey() {
        return key.registryKey();
    }

    @Override
    public @NotNull String toString() {
        return key().toString();
    }
}
