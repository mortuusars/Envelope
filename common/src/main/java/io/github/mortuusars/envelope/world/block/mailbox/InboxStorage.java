package io.github.mortuusars.envelope.world.block.mailbox;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.util.bugger.Bugger;
import io.github.mortuusars.envelope.world.mail.address.type.BlockAddress;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.util.*;

public class InboxStorage extends SavedData {
    public static final Codec<InboxStorage> CODEC = RecordCodecBuilder.create(instance -> instance.group(
          Codec.unboundedMap(UUIDUtil.STRING_CODEC, UnloadedInbox.CODEC)
                .optionalFieldOf("inboxes", Collections.emptyMap()).forGetter(InboxStorage::getInboxes)
    ).apply(instance, InboxStorage::new));
    public static final Logger LOGGER = LogUtils.getLogger();

    private final Map<UUID, UnloadedInbox> inboxes;

    public InboxStorage(Map<UUID, UnloadedInbox> inboxes) {
        this.inboxes = new HashMap<>(inboxes); // Make sure it's mutable
    }

    public InboxStorage() {
        this.inboxes = new HashMap<>();
    }

    protected Map<UUID, UnloadedInbox> getInboxes() {
        return inboxes;
    }

    public void put(UUID id, Inbox inbox) {
        UnloadedInbox unloadedInbox = new UnloadedInbox(id, inbox.getAddress(), inbox.getInboxCapacity(), inbox.getAllMail());
        unloadedInbox.getAllMail().removeIf(ItemStack::isEmpty); // Empty stacks are not allowed.
        inboxes.put(unloadedInbox.getId(), unloadedInbox);
        setDirty();

        if (Bugger.isEnabled() && !unloadedInbox.isInboxEmpty()) {
            LOGGER.info("Stored inbox with size [{}] of mailbox {}", unloadedInbox.getAllMail().size(), unloadedInbox.getAddress());
        }
    }

    /**
     * Removes inbox from storage by its id, if present.
     */
    public Optional<Inbox> remove(UUID id) {
        return Optional.ofNullable(inboxes.remove(id))
              .map(inbox -> {
                  setDirty();
                  if (Bugger.isEnabled() && !inbox.getAllMail().isEmpty()) {
                      LOGGER.info("Loaded inbox with size [{}] of mailbox {}", inbox.getAllMail().size(), inbox.getAddress());
                  }
                  return inbox;
              });
    }

    public @NotNull Optional<Inbox> getForDelivery(BlockAddress address) {
        for (UnloadedInbox inbox : getInboxes().values()) {
            if (inbox.getAddress().equals(address)) {
                inbox.onChanged(this::setDirty);
                return Optional.of(inbox);
            }
        }
        return Optional.empty();
    }

    // --

    public static InboxStorage get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(factory(), "envelope_inboxes");
    }

    public static @NotNull Factory<InboxStorage> factory() {
        return new Factory<>(
              InboxStorage::new,
              (tag, provider) -> CODEC.parse(provider.createSerializationContext(NbtOps.INSTANCE), tag)
                    .resultOrPartial(e -> Envelope.LOGGER.error("Cannot load InboxStorage: {}", e))
                    .orElse(null),
              null);
    }

    public @NotNull CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        return (CompoundTag) CODEC.encode(this, registries.createSerializationContext(NbtOps.INSTANCE), tag)
              .resultOrPartial(e -> Envelope.LOGGER.error("Cannot save InboxStorage: {}", e))
              .orElse(tag);
    }
}