package io.github.mortuusars.envelope.world.mail.entity.mail_service;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.world.mail.MailId;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class MailServiceData extends SavedData implements PaybackDepartmentData {
    public static final Codec<MailServiceData> CODEC = RecordCodecBuilder.create(i -> i.group(
          Codec.unboundedMap(MailId.STRING_CODEC, MailAwaitingPayback.CODEC)
                .optionalFieldOf("mail_awaiting_payback", Collections.emptyMap())
                .forGetter(MailServiceData::getMailAwaitingPayback)
    ).apply(i, MailServiceData::new));

    private final Map<MailId, MailAwaitingPayback> mailAwaitingPayback;

    public MailServiceData(Map<MailId, MailAwaitingPayback> mailAwaitingPayback) {
        this.mailAwaitingPayback = new HashMap<>(mailAwaitingPayback); // Make sure it's mutable
    }

    public MailServiceData() {
        this(Collections.emptyMap());
    }

    public Map<MailId, MailAwaitingPayback> getMailAwaitingPayback() {
        return mailAwaitingPayback;
    }

    // -- Save / Load

    public static MailServiceData get(ServerLevel level, String name) {
        return level.getDataStorage().computeIfAbsent(factory(), name);
    }

    public @NotNull CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        return CODEC.encode(this, registries.createSerializationContext(NbtOps.INSTANCE), tag)
              .ifError(e -> Envelope.LOGGER.error("Cannot save PigeonholeSavedData: {}", e.message()))
              .result()
              .filter(t -> t instanceof CompoundTag)
              .map(t -> ((CompoundTag) t))
              .orElse(tag);
    }

    private static MailServiceData load(CompoundTag tag, HolderLookup.Provider registries) {
        return CODEC.decode(registries.createSerializationContext(NbtOps.INSTANCE), tag)
              .ifError(e -> Envelope.LOGGER.error("Cannot load PigeonholeSavedData: {}", e.message()))
              .result()
              .map(Pair::getFirst)
              .orElseGet(MailServiceData::new);
    }

    private static Factory<MailServiceData> factory() {
        return new Factory<>(MailServiceData::new, MailServiceData::load, null);
    }
}
