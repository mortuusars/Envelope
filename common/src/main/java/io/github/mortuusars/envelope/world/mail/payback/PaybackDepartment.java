package io.github.mortuusars.envelope.world.mail.payback;

import com.google.common.base.Preconditions;
import com.mojang.logging.LogUtils;
import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.world.item.component.*;
import io.github.mortuusars.envelope.world.mail.address.type.ServiceAddress;
import io.github.mortuusars.envelope.world.mail.delivery.Delivery;
import io.github.mortuusars.envelope.world.mail.delivery.DeliveryPhase;
import io.github.mortuusars.envelope.world.item.PaybackPackageItem;
import io.github.mortuusars.envelope.world.item.PaybackBoxItem;
import io.github.mortuusars.envelope.world.item.mail.Mail;
import io.github.mortuusars.envelope.world.item.component.mail.log.DeliveryRecord;
import io.github.mortuusars.envelope.world.mail.MailService;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

public class PaybackDepartment {
    private static final Logger LOGGER = LogUtils.getLogger();

    private final MailService mailService;
    private @Nullable PaybackDepartmentData data = null;

    public PaybackDepartment(MailService context) {
        this.mailService = context;
    }

    public MailService getMailService() {
        return mailService;
    }

    public ServiceAddress getAddress() {
        return getMailService().getAddress();
    }

    public @NotNull PaybackDepartmentData getData() {
        if (data == null) {
            data = PaybackDepartmentData.get(mailService.getLevel(), "envelope_mail_service_payback_department");
        }
        return data;
    }

    // --

    public int getPendingPaybackSubjectCount() {
        return getData().getPaybackPendingSubjects().size();
    }

    protected @Nullable PaybackSubject getPendingPaybackSubject(Id id) {
        return getData().getPaybackPendingSubjects().get(id);
    }

    protected @Nullable PaybackSubject removePendingPaybackSubject(Id id) {
        @Nullable PaybackSubject subject = getData().getPaybackPendingSubjects().remove(id);
        if (subject != null) {
            getData().setDirty();
        }
        return subject;
    }

    protected long calculateExpireTick(ItemStack subject) {
        PaybackDuration duration = subject.getOrDefault(Envelope.DataComponents.MAIL_PAYBACK_REQUEST, PaybackRequest.createDefault())
              .duration();
        return getMailService().getGameTime() + duration.getTicks();
    }

    protected void awaitPayback(PaybackSubject paybackSubject) {
        getData().getPaybackPendingSubjects().put(paybackSubject.id(), paybackSubject);
        getData().setDirty();
    }

    // --

    public void tick() {
        //TODO: It would be a good idea to consider performance here.
        // Depending on mail counts this could get laggy, especially if multiple returns occur at the same time.

        if (getMailService().getGameTime() % 200 == 0) { // Check every minute
            getData().getPaybackPendingSubjects().entrySet().removeIf(entry -> {
                if (entry.getValue().expiresAt() <= getMailService().getGameTime()) {
                    returnSubjectToSender(entry.getValue(), DeliveryRecord.Message.PAYBACK_EXPIRED);
                    getData().setDirty();
                    return true;
                }
                return false;
            });
        }
    }

    public int returnAllAwaitingAsTimedOut() {
        int count = 0;
        for (PaybackSubject subject : getData().getPaybackPendingSubjects().values()) {
            returnSubjectToSender(subject, DeliveryRecord.Message.PAYBACK_EXPIRED);
            count++;
        }
        getData().getPaybackPendingSubjects().clear();
        getData().setDirty();
        return count;
    }

    public void returnSubjectToSender(PaybackSubject subject, Component reason) {
        ItemStack mail = subject.mail().copy();

        Mail.returned(mail, reason);
        Mail.writeToLog(mail, DeliveryRecord.sentFrom(getAddress()));

        getMailService().getDeliveryManager().startService(Delivery.draft()
              .deliver(mail)
              .from(getAddress())
              .to(subject.returnAddress())
              .startAtPhase(DeliveryPhase.TRAVELING_FROM_HUB_TO_RECIPIENT));
    }

    // --

    public boolean tryHandleDelivery(Delivery delivery) {
        if (delivery.getMail().has(Envelope.DataComponents.MAIL_PAYBACK_REQUEST)) {
            handlePaybackSubject(delivery);
            return true;
        }

        if (delivery.getMail().getItem() instanceof PaybackPackageItem) {
            handlePaybackPayment(delivery);
            return true;
        }

        return false;
    }

    public boolean tryHandleReturn(Delivery delivery) {
        if (delivery.getMail().getItem() instanceof PaybackBoxItem) {
            @Nullable PaybackSubject subject = delivery.getMail().get(Envelope.DataComponents.PAYBACK_SUBJECT);
            if (subject == null) {
                LOGGER.info("Returning Payback Packing Box does not have a subject linked. Packing Box will be voided.");
                delivery.setMail(ItemStack.EMPTY);
                delivery.beginPhase(DeliveryPhase.FINISHED);
                return true;
            }

            @Nullable PaybackSubject storedSubject = removePendingPaybackSubject(subject.id());
            if (storedSubject == null) {
                LOGGER.info("Returning Payback Packing Box links to subject of [{}] with id [{}], " +
                            "which is not found in storage. Packing Box will be voided.",
                      subject.mail().getHoverName().getString(), subject.id());
                delivery.setMail(ItemStack.EMPTY);
                delivery.beginPhase(DeliveryPhase.FINISHED);
                return true;
            }

            returnSubjectToSender(storedSubject, DeliveryRecord.Message.RECIPIENT_NOT_FOUND);
            LOGGER.info("Pending payback subject of [{}] with id [{}] was returned to sender, " +
                        "as Payback Packing Box couldn't be delivered to recipient.",
                  subject.mail().getHoverName().getString(), subject.id());

            delivery.setMail(ItemStack.EMPTY);
            delivery.beginPhase(DeliveryPhase.FINISHED);
            return true;
        }

        return false;
    }

    // -- Payback Subject

    protected void handlePaybackSubject(Delivery subjectDelivery) {
        Preconditions.checkArgument(subjectDelivery.getMail().has(Envelope.DataComponents.MAIL_PAYBACK_REQUEST));

        ItemStack subject = subjectDelivery.getMail();

        Mail.writeToLog(subject, DeliveryRecord.arrivedTo(getAddress()));

        Id subjectId = Id.create(getMailService().getLevel());
        PaybackSubject paybackSubject = new PaybackSubject(subjectId, subject, subjectDelivery.getSender(), calculateExpireTick(subject));

        sendPaybackPackingBoxToBuyer(subjectDelivery, paybackSubject);
        awaitPayback(paybackSubject);
        subjectDelivery.setMail(ItemStack.EMPTY);

        subjectDelivery.beginPhase(DeliveryPhase.TRAVELING_FROM_HUB_TO_SENDER);
    }

    protected void sendPaybackPackingBoxToBuyer(Delivery subjectDelivery, PaybackSubject paybackSubject) {
        ItemStack packingBox = Mail.createPaybackBox(paybackSubject)
              .sender(subjectDelivery.getSender())
              .recipient(subjectDelivery.getRecipient())
              .writeToLog(DeliveryRecord.sentFrom(getAddress()))
              .get();

        getMailService().getDeliveryManager()
              .startService(Delivery.draft()
                    .deliver(packingBox)
                    .from(subjectDelivery.getSender())
                    .to(subjectDelivery.getRecipient())
                    .startAtPhase(DeliveryPhase.DISPATCHING_DELIVERY));
    }

    // -- Payback Payment

    protected void handlePaybackPayment(Delivery paybackDelivery) {
        ItemStack paybackPackage = paybackDelivery.getMail();
        Mail.writeToLog(paybackPackage, DeliveryRecord.arrivedTo(getAddress()));

        @Nullable PaybackSubject paybackSubject = paybackPackage.get(Envelope.DataComponents.PAYBACK_SUBJECT);
        if (paybackSubject == null) {
            Mail.writeToLog(paybackPackage,
                  DeliveryRecord.returned(DeliveryRecord.Message.PAYBACK_SUBJECT_NOT_FOUND),
                  DeliveryRecord.sentFrom(getAddress()));
            paybackDelivery.beginPhase(DeliveryPhase.TRAVELING_FROM_HUB_TO_SENDER);
            return;
        }

        paybackSubject = getPendingPaybackSubject(paybackSubject.id());
        if (paybackSubject == null) {
            Mail.writeToLog(paybackPackage,
                  DeliveryRecord.returned(DeliveryRecord.Message.PAYBACK_SUBJECT_NOT_FOUND),
                  DeliveryRecord.sentFrom(getAddress()));
            paybackDelivery.beginPhase(DeliveryPhase.TRAVELING_FROM_HUB_TO_SENDER);
            return;
        }

        if (!paybackDelivery.getRecipient().equals(paybackSubject.returnAddress())) {
            Mail.writeToLog(paybackPackage,
                  DeliveryRecord.returned(DeliveryRecord.Message.PAYBACK_SUBJECT_NOT_FOUND),
                  DeliveryRecord.sentFrom(getAddress()));
            paybackDelivery.beginPhase(DeliveryPhase.TRAVELING_FROM_HUB_TO_SENDER);
            return;
        }

        PackageContents packageContents = PackageContents.of(paybackPackage);
        if (!paybackSubject.getRequest().matches(packageContents)) {
            Mail.writeToLog(paybackPackage,
                  DeliveryRecord.returned(DeliveryRecord.Message.PAYBACK_IS_NOT_VALID),
                  DeliveryRecord.sentFrom(getAddress()));
            paybackDelivery.beginPhase(DeliveryPhase.TRAVELING_FROM_HUB_TO_SENDER);
            return;
        }

        sendPaymentPackageToSeller(paybackDelivery, packageContents, paybackPackage);

        removePendingPaybackSubject(paybackSubject.id());
        sendSubjectToBuyerUsingSameDelivery(paybackDelivery, paybackSubject);
    }

    protected void sendSubjectToBuyerUsingSameDelivery(Delivery paybackDelivery, @NotNull PaybackSubject paybackSubject) {
        // Send the subject to buyer using same delivery and courier:
        ItemStack subject = Mail.of(paybackSubject.mail())
              .sender(paybackSubject.returnAddress())
              .writeToLog(DeliveryRecord.message(DeliveryRecord.Message.PAYBACK_FULFILLED))
              .writeToLog(DeliveryRecord.sentFrom(getAddress()))
              .get();
        paybackDelivery.setMail(subject);
        paybackDelivery.beginPhase(DeliveryPhase.TRAVELING_FROM_HUB_TO_SENDER);
    }

    protected void sendPaymentPackageToSeller(Delivery paybackDelivery, PackageContents packageContents, ItemStack paybackPackage) {
        ItemStack paymentPackage = Mail.createPackage(packageContents)
              .sender(paybackDelivery.getSender())
              .recipient(paybackDelivery.getRecipient())
              .setLog(Mail.getLog(paybackPackage))
              .writeToLog(DeliveryRecord.message(DeliveryRecord.Message.PAYBACK_FULFILLED))
              .writeToLog(DeliveryRecord.sentFrom(getAddress()))
              .get();

        getMailService().getDeliveryManager()
              .startService(Delivery.draft()
                    .deliver(paymentPackage)
                    .from(paybackDelivery.getSender())
                    .to(paybackDelivery.getRecipient())
                    .startAtPhase(DeliveryPhase.DISPATCHING_DELIVERY));
    }
}