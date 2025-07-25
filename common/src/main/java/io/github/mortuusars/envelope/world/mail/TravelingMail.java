package io.github.mortuusars.envelope.world.mail;

import io.github.mortuusars.envelope.Envelope;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

public class TravelingMail extends SavedData {
    protected static final String SAVED_DATA_NAME = "envelope_traveling_mail";
    protected static final String MAIL_TAG = "Mail";

    protected final Set<ItemStack> mail = new HashSet<>();
    protected final Set<ItemStack> finishedBuffer = new HashSet<>();
    protected Consumer<ItemStack> onFinishedTraveling;

    public boolean startTraveling(ItemStack mail) {
        if (!mail.has(Envelope.DataComponents.MAIL_SENT_AT) || !mail.has(Envelope.DataComponents.MAIL_TRAVEL_DURATION)) {
            Envelope.LOGGER.error("Mail '{}' cannot start traveling: no envelope:mail_sent_at or envelope:mail_travel_duration defined.", mail);
            return false;
        }

        this.mail.add(mail);
        setDirty();
        return true;
    }

    public void tick(MinecraftServer server) {
        long gameTime = server.overworld().getGameTime();
        mail.removeIf(mail -> {
            long sentAt = mail.getOrDefault(Envelope.DataComponents.MAIL_SENT_AT, 0L);
            int duration = mail.getOrDefault(Envelope.DataComponents.MAIL_TRAVEL_DURATION, 0);
            if (sentAt + duration <= gameTime) {
                finishedBuffer.add(mail);
                setDirty();
                return true;
            }
            return false;
        });

        for (ItemStack mail : finishedBuffer) {
            onFinishedTraveling.accept(mail);
        }
        finishedBuffer.clear();
    }

    // --

    public static TravelingMail get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(factory(), SAVED_DATA_NAME);
    }

    public static TravelingMail get(ServerLevel level) {
        return get(level.getServer());
    }

    // --

    public @NotNull CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();

        for (ItemStack item : mail) {
            try {
                list.add(item.save(registries, new CompoundTag()));
            } catch (Exception e) {
                Envelope.LOGGER.error("Cannot save mail '{}': {}", item, e.getMessage());
            }
        }

        tag.put(MAIL_TAG, list);

        return tag;
    }

    public void load(CompoundTag tag, HolderLookup.Provider registries) {
        mail.clear();

        ListTag list = tag.getList(MAIL_TAG, Tag.TAG_COMPOUND);

        for (Tag mailTag : list) {
            try {
                mail.add(ItemStack.parse(registries, mailTag).orElseThrow());
            } catch (Exception e) {
                Envelope.LOGGER.error("Cannot load mail '{}': {}", mailTag, e.getMessage());
            }
        }
    }

    private static Factory<TravelingMail> factory() {
        return new Factory<>(TravelingMail::new,
                (tag, provider) -> {
                    TravelingMail instance = new TravelingMail();
                    instance.load(tag, provider);
                    return instance;
                }, null);
    }
}
