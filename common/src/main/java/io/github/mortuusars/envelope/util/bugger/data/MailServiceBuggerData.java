package io.github.mortuusars.envelope.util.bugger.data;

import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.util.EnvelopeSymbols;
import io.github.mortuusars.envelope.world.item.mail.Mail;
import io.github.mortuusars.envelope.world.mail.delivery.Courier;
import io.github.mortuusars.envelope.world.mail.delivery.Delivery;
import io.github.mortuusars.envelope.world.mail.delivery.PhysicalCourier;
import io.github.mortuusars.envelope.world.mail.delivery.background.BackgroundCourier;
import io.github.mortuusars.envelope.world.entity.Pigeon;
import io.github.mortuusars.envelope.world.mail.MailService;
import io.github.mortuusars.mortaar.bugger.data.NbtData;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.world.entity.PathfinderMob;
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
        List<PhysicalCourier> couriers = mailService.getLevel().getEntities(EntityTypeTest.forClass(PathfinderMob.class),
              mob -> !mob.isNoAi() && mob instanceof PhysicalCourier courier && courier.isDelivering())
              .stream().map(mob -> (PhysicalCourier)mob)
              .toList();
        List<BackgroundCourier> backgroundCouriers = mailService.getBackgroundDelivery().getActiveCouriers();

        tag.putInt("mailboxes", mailService.getMailboxes().getAllAddresses().size());
        tag.putInt("dropped_mail_count", mailService.getBackgroundDelivery().getDroppedMail().size());
        tag.putInt("payback_pending_mail_count", mailService.getPaybackDepartment().getPendingPaybackSubjectCount());
        tag.putInt("cloud_depository_count", mailService.getCloudService().getTotalItemCount());

        tag.putInt("delivering_couriers", couriers.size());
        tag.putInt("background_delivering_couriers", backgroundCouriers.size());
        tag.putInt("background_finished_couriers", mailService.getBackgroundDelivery().getFinishedCouriers().size());

        ListTag deliveries = Stream.concat(couriers.stream(), backgroundCouriers.stream())
              .sorted(Comparator.comparingLong(courier -> courier.getCurrentDelivery().orElseThrow().getId().getTick()))
              .map(courier -> formDeliveryString(mailService, courier))
              .map(StringTag::valueOf)
              .collect(Collectors.toCollection(ListTag::new));

        tag.put("deliveries", deliveries);
    }

    private @NotNull String formDeliveryString(MailService mailService, Courier courier) {
        Delivery delivery = courier.getCurrentDelivery().orElseThrow();
        int phaseDuration = courier.getPhaseDuration(mailService.getLevel(), delivery, delivery.getPhase());

        return ChatFormatting.AQUA + delivery.getSender().format().withIcon().toString() + ChatFormatting.RESET +
              " " + EnvelopeSymbols.SMALL_FILLED_ARROW_RIGHT + " " +
              ChatFormatting.GREEN + delivery.getRecipient().format().withIcon().toString() + ChatFormatting.RESET +

              ChatFormatting.GRAY +
              " | " + (courier.getCourierOrigin().isService() ? "Service" : "Regular") +
              " | " + (Mail.isReturned(delivery.getMail()) ? "← " : "") + "✉ " + (!delivery.getMail().isEmpty() ? delivery.getMail().getHoverName().getString() : "Empty") +
              " | ↔ " + delivery.getRoute().getDistance(mailService.getLevel()) +
              " | ⌚" + delivery.getRoute().getFullTravelDuration().seconds() + "s" +
              ChatFormatting.RESET +

              " // " + delivery.getPhase().toPrettyString() +

              ChatFormatting.GRAY +
              " ⌛" + (phaseDuration - delivery.getPhaseProgress()) / 20 +
              ChatFormatting.RESET;
    }
}
