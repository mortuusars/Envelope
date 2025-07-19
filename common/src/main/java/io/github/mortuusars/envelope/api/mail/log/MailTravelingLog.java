package io.github.mortuusars.envelope.api.mail.log;

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

public record MailTravelingLog(List<TravelingRecord> records) {
    public static final Codec<MailTravelingLog> CODEC = TravelingRecord.CODEC.listOf(0, 32)
            .xmap(MailTravelingLog::new, MailTravelingLog::records);
    public static final StreamCodec<RegistryFriendlyByteBuf, MailTravelingLog> STREAM_CODEC =
            TravelingRecord.STREAM_CODEC.apply(ByteBufCodecs.list(32)).map(MailTravelingLog::new, MailTravelingLog::records);

    public static final MailTravelingLog EMPTY = new MailTravelingLog(Collections.emptyList());

    public boolean isEmpty() {
        return this.equals(EMPTY) || records.isEmpty();
    }

    public Optional<TravelingRecord> getLastRecord() {
        return !records.isEmpty() ? Optional.of(records.getLast()) : Optional.empty();
    }

    // --

    public static MailTravelingLog of(ItemStack mail) {
        return mail.getOrDefault(Envelope.DataComponents.MAIL_TRAVELING_LOG, EMPTY);
    }

    public static void addRecords(ItemStack mail, TravelingRecord... records) {
        MailTravelingLog mailTravelingLog = mail.getOrDefault(Envelope.DataComponents.MAIL_TRAVELING_LOG, MailTravelingLog.EMPTY);
        List<TravelingRecord> travelingRecords = new ArrayList<>(mailTravelingLog.records);
        travelingRecords.addAll(List.of(records));
        mail.set(Envelope.DataComponents.MAIL_TRAVELING_LOG, new MailTravelingLog(travelingRecords));
    }
}
