package io.github.mortuusars.envelope.world.delivery.log;

import com.mojang.serialization.Codec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.*;
import java.util.function.Predicate;

public final class DeliveryLog {
    public static final Codec<DeliveryLog> CODEC = DeliveryRecord.CODEC.listOf(0, 64).xmap(DeliveryLog::new, DeliveryLog::getRecords);
    public static final StreamCodec<RegistryFriendlyByteBuf, DeliveryLog> STREAM_CODEC =
          DeliveryRecord.STREAM_CODEC.apply(ByteBufCodecs.list(64)).map(DeliveryLog::new, DeliveryLog::getRecords);

    private final List<DeliveryRecord> records;

    public DeliveryLog(List<DeliveryRecord> records) {
        this.records = new ArrayList<>(records);
    }

    public static DeliveryLog empty() {
        return new DeliveryLog(Collections.emptyList());
    }

    public boolean isEmpty() {
        return records.isEmpty();
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
        records.add(record);
        return this;
    }

    public DeliveryLog append(DeliveryRecord.Builder recordBuilder) {
        records.add(recordBuilder.build());
        return this;
    }

    public List<DeliveryRecord> getRecords() {
        return records;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (DeliveryLog) obj;
        return Objects.equals(this.records, that.records);
    }

    @Override
    public int hashCode() {
        return Objects.hash(records);
    }

    @Override
    public String toString() {
        return "DeliveryLog[records=" + records + ']';
    }
}