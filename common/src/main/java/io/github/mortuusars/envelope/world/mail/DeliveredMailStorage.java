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
//TODO: implement SavedData
public class DeliveredMailStorage {
    protected final Map<UUID, List<Mail>> mail = new HashMap<>();

    public void deliver(UUID ownerUuid, Mail mail) {
        this.mail.computeIfAbsent(ownerUuid, uuid -> new ArrayList<>()).add(mail);
    }

    public boolean takeOut(UUID ownerUuid, Mail mail) {
        @Nullable List<Mail> set = this.mail.get(ownerUuid);
        return set != null && set.remove(mail);
    }

    public List<Mail> getAll(UUID ownerUuid) {
        List<Mail> list = mail.getOrDefault(ownerUuid, Collections.emptyList());
        return Collections.unmodifiableList(list);
    }

    // --

    public @NotNull CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        for (Map.Entry<UUID, List<Mail>> entry : mail.entrySet()) {
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
                List<Mail> mailSet = new ArrayList<>();

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