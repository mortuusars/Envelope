package io.github.mortuusars.envelope.world.block.mailbox;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.world.mail.address.Address;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class MailboxesSavedData extends SavedData {
    public static final Codec<MailboxesSavedData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
          Codec.unboundedMap(Address.Block.STRING_CODEC, RegisteredMailbox.CODEC)
                .optionalFieldOf("mailboxes", Collections.emptyMap()).forGetter(MailboxesSavedData::getMailboxes)
    ).apply(instance, MailboxesSavedData::new));

    private final HashMap<Address.Block, RegisteredMailbox> mailboxes;

    protected MailboxesSavedData(Map<Address.Block, RegisteredMailbox> mailboxes) {
        this.mailboxes = new HashMap<>(mailboxes); // Make sure it's mutable
    }

    protected MailboxesSavedData() {
        this(new HashMap<>());
    }

    public HashMap<Address.Block, RegisteredMailbox> getMailboxes() {
        return mailboxes;
    }

    // -- Save / Load

    public static MailboxesSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(factory(), "envelope_mailboxes");
    }

    public static @NotNull Factory<MailboxesSavedData> factory() {
        return new Factory<>(
              MailboxesSavedData::new,
              (tag, provider) -> CODEC.parse(provider.createSerializationContext(NbtOps.INSTANCE), tag)
                    .resultOrPartial(e -> Envelope.LOGGER.error("Cannot load MailboxesSavedData: {}", e))
                    .orElse(null),
              null);
    }

    public @NotNull CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        return (CompoundTag) CODEC.encode(this, registries.createSerializationContext(NbtOps.INSTANCE), tag)
              .resultOrPartial(e -> Envelope.LOGGER.error("Cannot save MailboxesSavedData: {}", e))
              .orElse(tag);
    }
}