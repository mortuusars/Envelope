package io.github.mortuusars.envelope.world.mail;

import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.api.mail.Address;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class Mailboxes extends SavedData {
    protected static final String SAVED_DATA_NAME = "envelope_mailboxes";

    protected final Map<String, List<ItemStack>> mailboxes = new HashMap<>();

    public boolean isKnown(String address) {
        return mailboxes.containsKey(address);
    }

    public boolean isKnown(Address address) {
        return mailboxes.containsKey(address.name());
    }

    public void receive(String address, ItemStack mail) {
        this.mailboxes.computeIfAbsent(address, uuid -> new ArrayList<>()).add(mail);
        setDirty();
    }

    public void receive(Address address, ItemStack mail) {
        receive(address.name(), mail);
    }

    public boolean takeOut(String address, ItemStack mail) {
        @Nullable List<ItemStack> set = this.mailboxes.get(address);
        boolean removed = set != null && set.remove(mail);
        if (removed) {
            setDirty();
        }
        return removed;
    }

    public List<ItemStack> getAll(String address) {
        @Nullable List<ItemStack> list = mailboxes.get(address);
        if (list == null) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(list);
    }

    public void create(String address) {
        mailboxes.computeIfAbsent(address, a -> new ArrayList<>());
        setDirty();
    }

    // --

    public static Mailboxes get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(factory(), SAVED_DATA_NAME);
    }

    public static Mailboxes get(ServerLevel level) {
        return get(level.getServer());
    }

    // --

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