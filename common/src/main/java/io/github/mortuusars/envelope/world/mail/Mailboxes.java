package io.github.mortuusars.envelope.world.mail;

import com.google.common.base.Preconditions;
import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.api.mail.Address;
import io.github.mortuusars.envelope.util.result.Failure;
import io.github.mortuusars.envelope.util.result.Result;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class Mailboxes extends SavedData {
    protected static final String SAVED_DATA_NAME = "envelope_mailboxes";

    protected final Map<Address.Mailbox, Map<UUID, ItemStack>> mailboxes = new HashMap<>();

    // --

    public void create(Address.Mailbox address) {
        mailboxes.computeIfAbsent(address, a -> new HashMap<>());
        setDirty();
    }

    public void remove(Address.Mailbox address) {
        mailboxes.remove(address);
        setDirty();
    }

    public boolean exists(Address.Mailbox address) {
        return mailboxes.containsKey(address);
    }

    // --

    public List<ItemStack> getAllMail(Address.Mailbox address) {
        @Nullable Map<UUID, ItemStack> map = mailboxes.get(address);
        if (map == null) {
            return Collections.emptyList();
        }
        return List.copyOf(map.values());
    }

    public void putMail(Address.Mailbox address, ItemStack mail) {
        Preconditions.checkArgument(mail.has(Envelope.DataComponents.MAIL_ID), "Mail must have 'envelope:mail_id'. " + mail);
        mailboxes.computeIfAbsent(address, uuid -> new HashMap<>()).put(mail.get(Envelope.DataComponents.MAIL_ID), mail);
        setDirty();
    }

    public Result<ItemStack> removeMail(Address.Mailbox address, UUID mailId) {
        @Nullable Map<UUID, ItemStack> contents = mailboxes.get(address);
        if (contents == null) {
            return Result.failure(new Failure("No mailbox with address '" + address + "' exists."));
        }

        @Nullable ItemStack mail = contents.remove(mailId);
        if (mail != null) {
            setDirty();
            return Result.success(mail);
        }

        return Result.failure(new Failure("Mail with mailId '" + mailId.toString() + "' is not in mailbox '" + address + "'."));
    }

    // --

    public static Mailboxes get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(factory(), SAVED_DATA_NAME);
    }

    public @NotNull CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        for (Map.Entry<Address.Mailbox, Map<UUID, ItemStack>> entry : mailboxes.entrySet()) {
            ListTag list = new ListTag();
            for (ItemStack item : entry.getValue().values()) {
                try {
                    list.add(item.save(registries, new CompoundTag()));
                } catch (Exception e) {
                    Envelope.LOGGER.error("Cannot save mail '{}': {}", item, e.getMessage());
                }
            }
            tag.put(entry.getKey().id(), list);
        }

        return tag;
    }

    public void load(CompoundTag tag, HolderLookup.Provider registries) {
        mailboxes.clear();

        for (String address : tag.getAllKeys()) {
            try {
                ListTag mailList = tag.getList(address, Tag.TAG_COMPOUND);
                Map<UUID, ItemStack> mailMap = new HashMap<>();

                for (Tag mailTag : mailList) {
                    try {
                        ItemStack mail = ItemStack.parse(registries, mailTag).orElseThrow();
                        @Nullable UUID id = mail.get(Envelope.DataComponents.MAIL_ID);
                        Preconditions.checkState(id != null, "No 'envelope:mail_id' in mail " + mail);
                        mailMap.put(id, mail);
                    } catch (Exception e) {
                        Envelope.LOGGER.error("Cannot load mail '{}': {}", mailTag, e.getMessage());
                    }
                }

                mailboxes.put(new Address.Mailbox(address), mailMap);
            } catch (Exception e) {
                Envelope.LOGGER.error("Cannot load mail of '{}': {}", address, e.getMessage());
            }
        }
    }

    private static Factory<Mailboxes> factory() {
        return new Factory<>(Mailboxes::new,
                (tag, provider) -> {
                    Mailboxes instance = new Mailboxes();
                    instance.load(tag, provider);
                    return instance;
                }, null);
    }
}