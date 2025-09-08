package io.github.mortuusars.envelope.mail.log;

import com.mojang.serialization.Codec;
import io.github.mortuusars.envelope.Envelope;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public record MailDeliveryLog(List<TravelingRecord> records) {
    public static final Codec<MailDeliveryLog> CODEC = TravelingRecord.CODEC.listOf()
            .xmap(MailDeliveryLog::new, MailDeliveryLog::records);
    public static final StreamCodec<RegistryFriendlyByteBuf, MailDeliveryLog> STREAM_CODEC =
            TravelingRecord.STREAM_CODEC.apply(ByteBufCodecs.list(32)).map(MailDeliveryLog::new, MailDeliveryLog::records);

    public static final MailDeliveryLog EMPTY = new MailDeliveryLog(Collections.emptyList());

    public boolean isEmpty() {
        return this.equals(EMPTY) || records.isEmpty();
    }

    public Optional<TravelingRecord> getLastRecord() {
        return !records.isEmpty() ? Optional.of(records.getLast()) : Optional.empty();
    }

    // --

    public static MailDeliveryLog of(ItemStack mail) {
        return mail.getOrDefault(Envelope.DataComponents.MAIL_DELIVERY_LOG, EMPTY);
    }

    public static void addRecords(ItemStack mail, TravelingRecord... records) {
        MailDeliveryLog mailDeliveryLog = mail.getOrDefault(Envelope.DataComponents.MAIL_DELIVERY_LOG, MailDeliveryLog.EMPTY);
        List<TravelingRecord> travelingRecords = new ArrayList<>(mailDeliveryLog.records);
        travelingRecords.addAll(List.of(records));
        mail.set(Envelope.DataComponents.MAIL_DELIVERY_LOG, new MailDeliveryLog(travelingRecords));
    }
}