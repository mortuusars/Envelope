package io.github.mortuusars.envelope.world.delivery.log;

import com.mojang.serialization.Codec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

public record DeliveryLog(List<DeliveryRecord> records) {
    public static final Codec<DeliveryLog> CODEC = DeliveryRecord.CODEC.listOf().xmap(DeliveryLog::new, DeliveryLog::records);
    public static final StreamCodec<RegistryFriendlyByteBuf, DeliveryLog> STREAM_CODEC =
            DeliveryRecord.STREAM_CODEC.apply(ByteBufCodecs.list(32)).map(DeliveryLog::new, DeliveryLog::records);

    public static final DeliveryLog EMPTY = new DeliveryLog(Collections.emptyList());

    public boolean isEmpty() {
        return this.equals(EMPTY) || records.isEmpty();
    }

    public Optional<DeliveryRecord> getLastRecord(Predicate<DeliveryRecord> predicate) {
        for (int i = records.size() - 1; i >= 0; i--) {
            DeliveryRecord record = records.get(i);
            if (predicate.test(record)) {
                return Optional.ofNullable(record);
            }
        }
        return Optional.empty();
    }

    public Optional<DeliveryRecord> getLastRecord() {
        return !records.isEmpty() ? Optional.of(records.getLast()) : Optional.empty();
    }

    public Optional<DeliveryRecord> getLastExceptionRecord() {
        return getLastRecord(record -> record.status().isException());
    }

    public Optional<DeliveryRecord> getLastRecordOfType(DeliveryRecord.Status status) {
        return getLastRecord(record -> record.status() == status);
    }

    // --

    public DeliveryLog append(DeliveryRecord record) {
        List<DeliveryRecord> records = new ArrayList<>(this.records);
        records.add(record);
        return new DeliveryLog(records);
    }

    public DeliveryLog append(DeliveryRecord... records) {
        List<DeliveryRecord> recordsList = new ArrayList<>(this.records);
        recordsList.addAll(List.of(records));
        return new DeliveryLog(recordsList);
    }
}