package io.github.mortuusars.envelope.world.block.mailbox;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.world.mail.address.Address;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class Inboxes extends SavedData {
    public static final Codec<Inboxes> CODEC = RecordCodecBuilder.create(instance -> instance.group(
          Codec.unboundedMap(UUIDUtil.STRING_CODEC, UnloadedInbox.CODEC)
                .optionalFieldOf("inboxes", Collections.emptyMap()).forGetter(Inboxes::getInboxes)
    ).apply(instance, Inboxes::new));

    private final Map<UUID, UnloadedInbox> inboxes;

    public Inboxes(Map<UUID, UnloadedInbox> inboxes) {
        this.inboxes = new HashMap<>(inboxes); // Make sure it's mutable
    }

    public Inboxes() {
        this.inboxes = new HashMap<>();
    }

    public Map<UUID, UnloadedInbox> getInboxes() {
        return inboxes;
    }

    public void store(UnloadedInbox inbox) {
        inbox.getAllMail().removeIf(ItemStack::isEmpty); // No empty stacks are allowed.
        inboxes.put(inbox.getId(), inbox);
        setDirty();
    }

    public void store(UUID id, Inbox inbox) {
        store(new UnloadedInbox(id, inbox.getAddress(), inbox.getInboxCapacity(), inbox.getAllMail()));
    }

    public Optional<UnloadedInbox> retrieve(UUID id) {
        UnloadedInbox inbox = inboxes.remove(id);
        if (inbox != null) {
            setDirty();
            return Optional.of(inbox);
        } else {
            return Optional.empty();
        }
    }

    public @NotNull Optional<UnloadedInbox> forDelivery(Address.Block address) {
        @Nullable UnloadedInbox observableInbox = null;
        for (UnloadedInbox inbox : getInboxes().values()) {
            if (inbox.getAddress().equals(address)) {
                observableInbox = new UnloadedInbox(inbox.getId(), inbox.getAddress(), inbox.getInboxCapacity(), inbox.getAllMail()) {
                    @Override
                    public void onInboxChanged() {
                        setDirty();
                    }
                };
            }
        }

        if (observableInbox != null) {
            store(observableInbox); // Store it so changes will be saved properly
            return Optional.of(observableInbox);
        }
        return Optional.empty();
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