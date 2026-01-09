package io.github.mortuusars.envelope.world.block.mailbox;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.mortuusars.envelope.Envelope;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class Inboxes extends SavedData {
    public static final Codec<Inboxes> CODEC = RecordCodecBuilder.create(instance -> instance.group(
          Codec.unboundedMap(UUIDUtil.STRING_CODEC, Inbox.CODEC)
                .optionalFieldOf("inboxes", Collections.emptyMap()).forGetter(Inboxes::getInboxes)
    ).apply(instance, Inboxes::new));

    private final Map<UUID, Inbox> inboxes;

    public Inboxes(Map<UUID, Inbox> inboxes) {
        this.inboxes = new HashMap<>(inboxes); // Make sure it's mutable
    }

    public Inboxes() {
        this.inboxes = new HashMap<>();
    }

    public Map<UUID, Inbox> getInboxes() {
        return inboxes;
    }

    public void store(UUID id, Inbox mail) {
        inboxes.put(id, mail);
        setDirty();
    }

    public Optional<Inbox> retrieve(UUID id) {
        Inbox inbox = inboxes.remove(id);
        if (inbox != null) {
            setDirty();
            return Optional.of(inbox);
        } else {
            return Optional.empty();
        }
    }

    // --

    public static Inboxes get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(factory(), "envelope_inboxes");
    }

    public static @NotNull Factory<Inboxes> factory() {
        return new Factory<>(
              Inboxes::new,
              (tag, provider) -> CODEC.parse(provider.createSerializationContext(NbtOps.INSTANCE), tag)
                    .resultOrPartial(e -> Envelope.LOGGER.error("Cannot load Inboxes: {}", e))
                    .orElse(null),
              null);
    }

    public @NotNull CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        return (CompoundTag) CODEC.encode(this, registries.createSerializationContext(NbtOps.INSTANCE), tag)
              .resultOrPartial(e -> Envelope.LOGGER.error("Cannot save Inboxes: {}", e))
              .orElse(tag);
    }
}
