package io.github.mortuusars.envelope.world.mail.entity.mail_service.payback_department;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.world.item.component.Id;
import io.github.mortuusars.envelope.world.item.component.PaybackSubject;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class PaybackDepartmentData extends SavedData {
    public static final Codec<PaybackDepartmentData> CODEC = RecordCodecBuilder.create(i -> i.group(
          Codec.unboundedMap(Id.CODEC, PaybackSubject.CODEC)
                .optionalFieldOf("payback_pending", Collections.emptyMap())
                .forGetter(PaybackDepartmentData::getPaybackPendingSubjects)
    ).apply(i, PaybackDepartmentData::new));

    private final Map<Id, PaybackSubject> paybackPendingSubjects;

    public PaybackDepartmentData(Map<Id, PaybackSubject> paybackPendingSubjects) {
        this.paybackPendingSubjects = new HashMap<>(paybackPendingSubjects); // Make sure it's mutable
    }

    public PaybackDepartmentData() {
        this.paybackPendingSubjects = new HashMap<>();
    }

    public Map<Id, PaybackSubject> getPaybackPendingSubjects() {
        return paybackPendingSubjects;
    }

    // -- Save / Load

    public static PaybackDepartmentData get(ServerLevel level, String name) {
        return level.getDataStorage().computeIfAbsent(factory(), name);
    }

    public @NotNull CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        return CODEC.encode(this, registries.createSerializationContext(NbtOps.INSTANCE), tag)
              .ifError(e -> Envelope.LOGGER.error("Cannot save PaybackDepartmentData: {}", e.message()))
              .result()
              .filter(t -> t instanceof CompoundTag)
              .map(t -> ((CompoundTag) t))
              .orElse(tag);
    }

    private static PaybackDepartmentData load(CompoundTag tag, HolderLookup.Provider registries) {
        return CODEC.decode(registries.createSerializationContext(NbtOps.INSTANCE), tag)
              .ifError(e -> Envelope.LOGGER.error("Cannot load PaybackDepartmentData: {}", e.message()))
              .result()
              .map(Pair::getFirst)
              .orElseGet(PaybackDepartmentData::new);
    }

    private static SavedData.Factory<PaybackDepartmentData> factory() {
        return new SavedData.Factory<>(PaybackDepartmentData::new, PaybackDepartmentData::load, null);
    }
}
