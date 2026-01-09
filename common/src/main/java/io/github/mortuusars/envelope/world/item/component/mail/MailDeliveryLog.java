package io.github.mortuusars.envelope.world.item.component.mail;

import com.mojang.serialization.Codec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.*;

public record MailDeliveryLog(List<MailDeliveryRecord> records) {
    public static final Codec<MailDeliveryLog> CODEC =
          MailDeliveryRecord.CODEC.listOf(0, 64).xmap(MailDeliveryLog::new, MailDeliveryLog::records);
    public static final StreamCodec<RegistryFriendlyByteBuf, MailDeliveryLog> STREAM_CODEC =
          MailDeliveryRecord.STREAM_CODEC.apply(ByteBufCodecs.list(64)).map(MailDeliveryLog::new, MailDeliveryLog::records);

    public static final MailDeliveryLog EMPTY = new MailDeliveryLog(Collections.emptyList());

    public boolean isEmpty() {
        return records.isEmpty();
    }

    public MailDeliveryLog append(MailDeliveryRecord record) {
        List<MailDeliveryRecord> records = new ArrayList<>(this.records);
        records.add(record);
        return new MailDeliveryLog(records);
    }

    public MailDeliveryLog append(MailDeliveryRecord.Builder recordBuilder) {
        return append(recordBuilder.build());
    }

    public MailDeliveryLog append(MailDeliveryRecord... list) {
        List<MailDeliveryRecord> records = new ArrayList<>(this.records);
        records.addAll(Arrays.asList(list));
        return new MailDeliveryLog(records);
    }

    public MailDeliveryLog append(MailDeliveryRecord.Builder... list) {
        List<MailDeliveryRecord> records = new ArrayList<>(this.records);
        for (MailDeliveryRecord.Builder record : list) {
            records.add(record.build());
        }
        return new MailDeliveryLog(records);
    }
}