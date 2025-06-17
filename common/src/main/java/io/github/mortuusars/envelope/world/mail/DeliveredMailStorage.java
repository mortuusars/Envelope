package io.github.mortuusars.envelope.world.mail;

import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.api.mail.Mail;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class DeliveredMailStorage {
    protected final Map<UUID, Set<Mail>> mail = new HashMap<>();

    public boolean deliver(UUID recipientUuid, Mail mail) {
        return this.mail.computeIfAbsent(recipientUuid, uuid -> new HashSet<>()).add(mail);
    }

    public boolean takeOut(UUID recipientUuid, Mail mail) {
        @Nullable Set<Mail> set = this.mail.get(recipientUuid);
        return set != null && set.remove(mail);
    }

    public Set<Mail> getAll(UUID recipientUuid) {
        Set<Mail> set = mail.getOrDefault(recipientUuid, Collections.emptySet());
        return Collections.unmodifiableSet(set);
    }

    // --

    public @NotNull CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        for (Map.Entry<UUID, Set<Mail>> entry : mail.entrySet()) {
            ListTag list = new ListTag();
            for (Mail item : entry.getValue()) {
                try {
                    list.add(Mail.CODEC.encodeStart(NbtOps.INSTANCE, item).getOrThrow());
                } catch (Exception e) {
                    Envelope.LOGGER.error("Cannot save mail '{}': {}", item, e.getMessage());
                }
            }
            tag.put(entry.getKey().toString(), list);
        }

        return tag;
    }

    public void load(CompoundTag tag, HolderLookup.Provider registries) {
        mail.clear();

        for (String id : tag.getAllKeys()) {
            UUID uuid;
            try {
                uuid = UUID.fromString(id);
            } catch (Exception e) {
                Envelope.LOGGER.error("Cannot parse owner uuid from '{}': {}", id, e.getMessage());
                return;
            }

            try {
                ListTag mailList = tag.getList(id, Tag.TAG_COMPOUND);
                Set<Mail> mailSet = new HashSet<>();

                for (Tag mailTag : mailList) {
                    try {
                        mailSet.add(Mail.CODEC.decode(NbtOps.INSTANCE, mailTag).getOrThrow().getFirst());
                    } catch (Exception e) {
                        Envelope.LOGGER.error("Cannot load mail '{}': {}", mailTag, e.getMessage());
                    }
                }

                mail.put(uuid, mailSet);
            } catch (Exception e) {
                Envelope.LOGGER.error("Cannot load mail of '{}': {}", uuid, e.getMessage());
            }
        }
    }
}