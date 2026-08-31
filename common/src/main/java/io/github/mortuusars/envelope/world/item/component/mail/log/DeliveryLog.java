package io.github.mortuusars.envelope.world.item.component.mail.log;

import com.mojang.serialization.Codec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.*;

public record DeliveryLog(List<DeliveryRecord> records) {
    public static final Codec<DeliveryLog> CODEC =
          DeliveryRecord.CODEC.listOf(0, 64).xmap(DeliveryLog::new, DeliveryLog::records);
    public static final StreamCodec<RegistryFriendlyByteBuf, DeliveryLog> STREAM_CODEC =
          DeliveryRecord.STREAM_CODEC.apply(ByteBufCodecs.list(64)).map(DeliveryLog::new, DeliveryLog::records);

    public static final DeliveryLog EMPTY = new DeliveryLog(Collections.emptyList());

    public boolean isEmpty() {
        return records.isEmpty();
    }

    public DeliveryLog append(DeliveryRecord record) {
        List<DeliveryRecord> records = new ArrayList<>(this.records);
        records.add(record);
        while (records.size() > 64) {
            records.removeFirst();
        }
        return new DeliveryLog(records);
    }

    public DeliveryLog append(DeliveryRecord... list) {
        List<DeliveryRecord> records = new ArrayList<>(this.records);
        records.addAll(Arrays.asList(list));
        while (records.size() > 64) {
            records.removeFirst();
        }
        return new DeliveryLog(records);
    }

    @Override
    public boolean equals(Object object) {
        if (object == null || getClass() != object.getClass()) return false;
        DeliveryLog that = (DeliveryLog) object;
        return Objects.equals(records, that.records);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(records);
    }
}