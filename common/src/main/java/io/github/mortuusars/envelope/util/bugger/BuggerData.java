package io.github.mortuusars.envelope.util.bugger;

import com.mojang.logging.LogUtils;
import io.github.mortuusars.envelope.network.Packets;
import io.github.mortuusars.envelope.util.bugger.data.Data;
import io.github.mortuusars.envelope.util.bugger.network.BuggerDataS2CP;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;


/**
 * Utility system to simplify debug data transferring from server to client.<br>
 * Data is stored only on the client, server only sends.<br>
 * Uses CompoundTag to encode/decode data, which is not ideal for sending data over network (StreamCodecs exist),
 * but it simplifies data writing and reading, which is more important than performance for debug cases.
 */
public class BuggerData {
    public static final Map<ResourceLocation, Data<?>> DEFINITIONS = new HashMap<>();
    public static final Map<ResourceLocation, Object> DATA = new HashMap<>();
    private static final Logger LOGGER = LogUtils.getLogger();

    public static <T> Optional<T> get(ResourceLocation id) {
        if (!Bugger.isEnabled()) return Optional.empty();
        try {
            @SuppressWarnings("unchecked")
            T value = (T) BuggerData.DATA.get(id);
            return Optional.ofNullable(value);
        } catch (ClassCastException e) {
            LOGGER.error("Stored bugger data '{}' has incorrect type: ", id, e);
            return Optional.empty();
        }
    }

    public static void send(ResourceLocation id, Function<RegistryAccess, @Nullable Tag> dataSupplier) {
        if (!Bugger.isEnabled()) return;
        Packets.sendToAllClients(registryAccess -> {
            CompoundTag tag = new CompoundTag();
            @Nullable Tag data = dataSupplier.apply(registryAccess);
            if (data != null) tag.put("data", data);
            return new BuggerDataS2CP(id, tag);
        });
    }

    public static void receive(ResourceLocation id, CompoundTag tag, RegistryAccess registryAccess) {
        if (!Bugger.isEnabled()) return;
        Data<?> definition = Objects.requireNonNull(DEFINITIONS.get(id), "Definition '" + id + "' was not registered.");
        @Nullable Object newValue = tag.contains("data") ? definition.decode(tag.get("data"), registryAccess) : null;

        if (newValue == null) {
            DATA.put(id, null);
        } else {
            receiveInternal(definition, id, newValue);
        }
    }

    // Glorious generics magick
    private static <T> void receiveInternal(Data<T> definition, ResourceLocation id, @NotNull Object newValueObj) {
        DATA.compute(id, (dataId, existingValueObj) -> {
            if (existingValueObj == null) return newValueObj;
            @SuppressWarnings("unchecked")
            T existingValue = (T) existingValueObj;
            @SuppressWarnings("unchecked")
            T newValue = (T) newValueObj;
            T appliedValue = definition.apply(existingValue, newValue);
            definition.handle(appliedValue);
            return appliedValue;
        });
    }
}
