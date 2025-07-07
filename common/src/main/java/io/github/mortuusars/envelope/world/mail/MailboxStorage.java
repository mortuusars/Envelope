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
public class MailboxStorage {
    protected final Map<String, List<Mail>> mailboxes = new HashMap<>();

    public boolean exists(String address) {
        return mailboxes.containsKey(address);
    }

    public void deliver(String address, Mail mail) {
        this.mailboxes.computeIfAbsent(address, uuid -> new ArrayList<>()).add(mail);
    }

    public boolean takeOut(String address, Mail mail) {
        @Nullable List<Mail> set = this.mailboxes.get(address);
        return set != null && set.remove(mail);
    }

    public List<Mail> getAll(String address) {
        @Nullable List<Mail> list = mailboxes.get(address);
        if (list == null) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(list);
    }

    // --

    public @NotNull CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        for (Map.Entry<String, List<Mail>> entry : mailboxes.entrySet()) {
            ListTag list = new ListTag();
            for (Mail item : entry.getValue()) {
                try {
                    list.add(Mail.CODEC.encodeStart(NbtOps.INSTANCE, item).getOrThrow());
                } catch (Exception e) {
                    Envelope.LOGGER.error("Cannot save mail '{}': {}", item, e.getMessage());
                }
            }
            tag.put(entry.getKey(), list);
        }

        return tag;
    }

    public void load(CompoundTag tag, HolderLookup.Provider registries) {
        mailboxes.clear();

        for (String address : tag.getAllKeys()) {
            try {
                ListTag mailList = tag.getList(address, Tag.TAG_COMPOUND);
                List<Mail> mailSet = new ArrayList<>();

                for (Tag mailTag : mailList) {
                    try {
                        mailSet.add(Mail.CODEC.decode(NbtOps.INSTANCE, mailTag).getOrThrow().getFirst());
                    } catch (Exception e) {
                        Envelope.LOGGER.error("Cannot load mail '{}': {}", mailTag, e.getMessage());
                    }
                }

                mailboxes.put(address, mailSet);
            } catch (Exception e) {
                Envelope.LOGGER.error("Cannot load mail of '{}': {}", address, e.getMessage());
            }
        }
    }

    public void create(String address) {
        mailboxes.computeIfAbsent(address, a -> new ArrayList<>());
    }
}