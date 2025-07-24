package io.github.mortuusars.envelope.world.mail;

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

    protected final Map<String, List<ItemStack>> mailboxes = new HashMap<>();

    public void create(String address) {
        mailboxes.computeIfAbsent(address, a -> new ArrayList<>());
        setDirty();
    }

    public void create(Address address) {
        create(address.id());
    }

    public boolean exists(String address) {
        return mailboxes.containsKey(address);
    }

    public boolean exists(Address address) {
        return mailboxes.containsKey(address.id());
    }

    public List<ItemStack> getAllMail(String address) {
        @Nullable List<ItemStack> list = mailboxes.get(address);
        if (list == null) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(list);
    }

    public List<ItemStack> getAllMail(Address address) {
        return getAllMail(address.id());
    }

    public void put(String address, ItemStack mail) {
        mailboxes.computeIfAbsent(address, uuid -> new ArrayList<>()).add(mail);
        setDirty();
    }

    public void put(Address address, ItemStack mail) {
        put(address.id(), mail);
    }

    public Result<ItemStack> extract(String address, ItemStack mail) {
        @Nullable List<ItemStack> contents = mailboxes.get(address);
        if (contents == null) {
            return Result.failure(new Failure("No mailbox with address '" + address + "' exists."));
        }

        if (contents.remove(mail)) {
            setDirty();
            return Result.success(mail);
        }

        return Result.failure(new Failure("'" + mail + "' is not in mailbox '" + address + "'."));
    }

    public boolean takeOut(String address, ItemStack mail) {
        //TODO: Rethink take out
        @Nullable List<ItemStack> set = this.mailboxes.get(address);
        boolean removed = set != null && set.remove(mail);
        if (removed) {
            setDirty();
        }
        return removed;
    }

    // --

    public static Mailboxes get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(factory(), SAVED_DATA_NAME);
    }

    public @NotNull CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        for (Map.Entry<String, List<ItemStack>> entry : mailboxes.entrySet()) {
            ListTag list = new ListTag();
            for (ItemStack item : entry.getValue()) {
                try {
                    list.add(item.save(registries, new CompoundTag()));
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
                List<ItemStack> mailSet = new ArrayList<>();

                for (Tag mailTag : mailList) {
                    try {
                        mailSet.add(ItemStack.parse(registries, mailTag).orElseThrow());
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

    private static Factory<Mailboxes> factory() {
        return new Factory<>(Mailboxes::new,
                (tag, provider) -> {
                    Mailboxes instance = new Mailboxes();
                    instance.load(tag, provider);
                    return instance;
                }, null);
    }
}