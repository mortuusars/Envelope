package io.github.mortuusars.envelope.util.bugger_data;

import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.util.EnvelopeSymbols;
import io.github.mortuusars.envelope.util.bugger.data.NbtData;
import io.github.mortuusars.envelope.world.delivery.Courier;
import io.github.mortuusars.envelope.world.delivery.Delivery;
import io.github.mortuusars.envelope.world.delivery.background.BackgroundCourier;
import io.github.mortuusars.envelope.world.entity.Pigeon;
import io.github.mortuusars.envelope.world.service.MailService;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.world.level.entity.EntityTypeTest;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MailServiceBuggerData extends NbtData {
    public MailServiceBuggerData() {
        super(Envelope.resource("mail_service"));
    }

    public void collectAndSendData(MailService mailService) {
        sendValues(tag -> writeDebugInfo(mailService, tag));
    }

    // --

    private void writeDebugInfo(MailService mailService, CompoundTag tag) {
        List<? extends Pigeon> pigeons = mailService.getLevel().getEntities(EntityTypeTest.forClass(Pigeon.class), Pigeon::isDelivering);
        List<BackgroundCourier> backgroundCouriers = mailService.getBackgroundDelivery().getCouriers();

        tag.putInt("mailboxes", mailService.mailboxes().getAllAddresses().size());
        tag.putInt("delivering_pigeons", pigeons.size());
        tag.putInt("background_delivering_pigeons", backgroundCouriers.size());
        tag.putInt("background_finished_pigeons", mailService.getBackgroundDelivery().getFinishedCouriers().size());

        tag.putInt("mail_awaiting_payback", mailService.getPaybackDepartment().getPendingPaybackSubjectCount());

        ListTag deliveries = Stream.concat(pigeons.stream(), backgroundCouriers.stream())
              .sorted(Comparator.comparingLong(courier -> courier.getCurrentDelivery().orElseThrow().getMetadata().timestamp()))
              .map(courier -> formDeliveryString(mailService, courier))
              .map(StringTag::valueOf)
              .collect(Collectors.toCollection(ListTag::new));

        tag.put("deliveries", deliveries);
    }

    private @NotNull String formDeliveryString(MailService mailService, Courier courier) {
        Delivery delivery = courier.getCurrentDelivery().orElseThrow();
        int phaseDuration = courier.getDeliveryHandler().getPhaseDuration(mailService.getLevel(), delivery, delivery.getPhase());

        return ChatFormatting.AQUA + delivery.getSender().format().withIcon().toString() + ChatFormatting.RESET +
              " " + EnvelopeSymbols.SMALL_FILLED_ARROW_RIGHT + " " +
              ChatFormatting.GREEN + delivery.getRecipient().format().withIcon().toString() + ChatFormatting.RESET +

              ChatFormatting.GRAY +
              (!delivery.getMail().isEmpty() ? " " + delivery.getMail().getHoverName().getString() : "") +
              mailService.getDistanceBetween(delivery.getSender(), delivery.getRecipient()).map(d -> " | ↔" + d).orElse("") +
              " | ⌚" + delivery.getRoute().travelDuration().seconds() + "s" +
              ChatFormatting.RESET +

              " // " + delivery.getPhase().toPrettyString() +

              ChatFormatting.GRAY +
              " ⌛" + (phaseDuration - delivery.getPhaseProgress()) / 20 +
              ChatFormatting.RESET;
    }
}
