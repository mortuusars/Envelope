package io.github.mortuusars.envelope.world.mail;

import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.api.mail.Mail;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.MinecraftServer;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TravelingMailStorage {
    protected final OnFinishedTraveling onFinishTraveling;
    protected final Set<Mail> mail = new HashSet<>();
    protected final List<Mail> finishedTravelingBuffer = new ArrayList<>();

    public TravelingMailStorage(OnFinishedTraveling deliver) {
        this.onFinishTraveling = deliver;
    }

    public void add(Mail mail) {
        this.mail.add(mail);
    }

    public void tick(MinecraftServer server) {
        mail.removeIf(mail -> {
            if (mail.sentAt() + mail.travelDuration() <= server.overworld().getGameTime()) {
                finishedTravelingBuffer.add(mail);
                return true;
            }
            return false;
        });

        for (Mail mail : finishedTravelingBuffer) {
            onFinishTraveling.finishTraveling(server, mail);
        }

        finishedTravelingBuffer.clear();
    }

    // --

    public @NotNull CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        for (Mail item : mail) {
            try {
                tag.put(item.sender().name(), Mail.CODEC.encodeStart(NbtOps.INSTANCE, item).getOrThrow());
            } catch (Exception e) {
                Envelope.LOGGER.error("Cannot save mail '{}': {}", item, e.getMessage());
            }
        }
        return tag;
    }

    public void load(CompoundTag tag, HolderLookup.Provider registries) {
        mail.clear();
        for (String key : tag.getAllKeys()) {
            CompoundTag compound = tag.getCompound(key);
            try {
                mail.add(Mail.CODEC.decode(NbtOps.INSTANCE, compound).getOrThrow().getFirst());
            } catch (Exception e) {
                Envelope.LOGGER.error("Cannot load mail '{}': {}", compound, e.getMessage());
            }
        }
    }

    // --

    public interface OnFinishedTraveling {
        void finishTraveling(MinecraftServer server, Mail mail);
    }
}
