package io.github.mortuusars.envelope.world.mail.entity.mail_service.payback_department;

import com.mojang.logging.LogUtils;
import io.github.mortuusars.envelope.Config;
import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.util.Ticks;
import io.github.mortuusars.envelope.world.delivery.Delivery;
import io.github.mortuusars.envelope.world.delivery.phase.DeliveryPhase;
import io.github.mortuusars.envelope.world.item.PaybackPackageItem;
import io.github.mortuusars.envelope.world.item.component.Mail;
import io.github.mortuusars.envelope.world.item.component.PackageContents;
import io.github.mortuusars.envelope.world.item.component.RequestedPayback;
import io.github.mortuusars.envelope.world.item.component.mail.DeliveryRecord;
import io.github.mortuusars.envelope.world.mail.MailId;
import io.github.mortuusars.envelope.world.mail.address.Address;
import io.github.mortuusars.envelope.world.service.MailService;
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

    public @NotNull PaybackDepartmentData getData() {
        if (data == null) {
            data = PaybackDepartmentData.get(mailService.getLevel(), "mail_service_payback_department");
        }
        return data;
    }

    // --

    public int getPendingPaybackSubjectCount() {
        return getData().getPaybackPendingSubjects().size();
    }

    protected @Nullable PaybackSubject getPendingPaybackSubject(MailId id) {
        return getData().getPaybackPendingSubjects().get(id);
    }

    protected void removePendingPaybackSubject(MailId id) {
        getData().getPaybackPendingSubjects().remove(id);
        getData().setDirty();
    }

    protected long getPaybackTimeoutTicksFor(ItemStack mail) {
        return Ticks.fromMinutes(Config.Server.DELIVERY_PAYBACK_TIMEOUT_MINUTES.get());
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
                if (entry.getValue().timeoutTick() <= getMailService().getGameTime()) {
                    returnAsTimedOut(entry.getValue());
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
            if (returnAsTimedOut(subject)) {
                count++;
            }
        }
        getData().getPaybackPendingSubjects().clear();
        getData().setDirty();
        return count;
    }

    public boolean returnAsTimedOut(PaybackSubject subject) {
        ItemStack mail = subject.mail();

        Mail.writeToLog(mail, DeliveryRecord.returned(Address.MAIL_SERVICE)
              .at(getMailService().getGameTime())
              .message(DeliveryRecord.Message.PAYBACK_IS_TIMED_OUT));

        return getMailService().getDeliveryManager()
              .startService(Delivery.draft()
                    .deliver(mail)
                    .from(Address.MAIL_SERVICE)
                    .to(subject.returnAddress())
                    .startAtPhase(DeliveryPhase.RETURNING_TO_SENDER))
              .map(delivery -> true, error -> {
                  LOGGER.error("Cannot return timed out pending payback mail. It will be voided.");
                  return false;
              });
    }

    // --

    public boolean tryHandle(Delivery delivery) {
        if (delivery.getMail().has(Envelope.DataComponents.MAIL_REQUESTED_PAYBACK)) {
            handlePaybackSubject(delivery, delivery.getMail().get(Envelope.DataComponents.MAIL_REQUESTED_PAYBACK));
            return true;
        }

        if (delivery.getMail().getItem() instanceof PaybackPackageItem) {
            handlePaybackPayment(delivery);
            return true;
        }

        return false;
    }

    // -- Payback Subject

    protected void handlePaybackSubject(Delivery subjectDelivery, RequestedPayback requestedPayback) {
        ItemStack subject = subjectDelivery.getMail();

        Mail.writeToLog(subject, DeliveryRecord.arrivedTo(Address.MAIL_SERVICE)
              .at(getMailService().getGameTime())
              .message(DeliveryRecord.Message.WAITING_FOR_PAYMENT));

        MailId subjectId = MailId.createRandom();
        long timeoutTick = getMailService().getGameTime() + getPaybackTimeoutTicksFor(subject);
        PaybackSubject paybackSubject = new PaybackSubject(subject, subjectId, subjectDelivery.getSender(), timeoutTick);

        if (sendPaybackPackingBoxToBuyer(subjectDelivery, paybackSubject)) {
            awaitPayback(paybackSubject);
            subjectDelivery.setMail(ItemStack.EMPTY);
        } else {
            Mail.writeToLog(subject, DeliveryRecord.returned(Address.MAIL_SERVICE)
                  .message(DeliveryRecord.Message.PAYBACK_IS_NOT_VALID));
        }

        subjectDelivery.setPhaseAndResetProgress(DeliveryPhase.RETURNING_TO_SENDER);
    }

    protected boolean sendPaybackPackingBoxToBuyer(Delivery subjectDelivery, PaybackSubject paybackSubject) {
        ItemStack packingBox = Mail.createPaybackPackingBox(paybackSubject)
              .sender(subjectDelivery.getSender())
              .recipient(subjectDelivery.getRecipient())
              .writeToLog(DeliveryRecord.sentFrom(subjectDelivery.getSender()).at(mailService.getGameTime()))
              .get();

        return getMailService().getDeliveryManager()
              .startService(Delivery.draft()
                    .deliver(packingBox)
                    .from(subjectDelivery.getSender())
                    .to(subjectDelivery.getRecipient())
                    .startAtPhase(DeliveryPhase.DISPATCHING))
              .isSuccess();
    }

    // -- Payback Payment

    protected void handlePaybackPayment(Delivery paybackDelivery) {
        ItemStack paybackPackage = paybackDelivery.getMail();
        Mail.writeToLog(paybackPackage, DeliveryRecord.arrivedTo(Address.MAIL_SERVICE).at(getMailService().getGameTime()));

        @Nullable PaybackSubject paybackSubject = paybackPackage.get(Envelope.DataComponents.PAYBACK_PACKAGE_SUBJECT);
        if (paybackSubject == null) {
            Mail.writeToLog(paybackPackage, DeliveryRecord.returned(Address.MAIL_SERVICE)
                  .message(DeliveryRecord.Message.PAYBACK_SUBJECT_NOT_FOUND));
            paybackDelivery.setPhaseAndResetProgress(DeliveryPhase.RETURNING_TO_SENDER);
            return;
        }

        paybackSubject = getPendingPaybackSubject(paybackSubject.id());
        if (paybackSubject == null) {
            Mail.writeToLog(paybackPackage, DeliveryRecord.returned(Address.MAIL_SERVICE)
                  .message(DeliveryRecord.Message.PAYBACK_SUBJECT_NOT_FOUND));
            paybackDelivery.setPhaseAndResetProgress(DeliveryPhase.RETURNING_TO_SENDER);
            return;
        }

        RequestedPayback requestedPayback = paybackSubject.mail().getOrDefault(Envelope.DataComponents.MAIL_REQUESTED_PAYBACK, RequestedPayback.DEFAULT);
        PackageContents packageContents = PackageContents.of(paybackPackage);
        if (!requestedPayback.matches(packageContents)) {
            Mail.writeToLog(paybackPackage, DeliveryRecord.returned(Address.MAIL_SERVICE)
                  .message(DeliveryRecord.Message.PAYBACK_IS_NOT_VALID));
            paybackDelivery.setPhaseAndResetProgress(DeliveryPhase.RETURNING_TO_SENDER);
            return;
        }

        if (!sendPaymentPackageToSeller(paybackDelivery, packageContents, paybackPackage)) {
            Mail.writeToLog(paybackPackage, DeliveryRecord.returned(Address.MAIL_SERVICE)
                  .message(DeliveryRecord.Message.PAYBACK_IS_NOT_VALID));
            paybackDelivery.setPhaseAndResetProgress(DeliveryPhase.RETURNING_TO_SENDER);
            return;
        }

        removePendingPaybackSubject(paybackSubject.id());
        // Send the subject to buyer using same delivery and courier:
        ItemStack subject = Mail.of(paybackSubject.mail())
              .sender(paybackDelivery.getSender())
              .writeToLog(DeliveryRecord.sentFrom(Address.MAIL_SERVICE)
                    .at(mailService.getGameTime())
                    .message(DeliveryRecord.Message.PAYBACK_FULFILLED))
              .get();
        paybackDelivery.setMail(subject);
        paybackDelivery.setPhaseAndResetProgress(DeliveryPhase.RETURNING_TO_SENDER);
    }

    protected boolean sendPaymentPackageToSeller(Delivery paybackDelivery, PackageContents packageContents, ItemStack paybackPackage) {
        ItemStack paymentPackage = Mail.createPackage(packageContents)
              .sender(paybackDelivery.getSender())
              .recipient(paybackDelivery.getRecipient())
              .setLog(Mail.getLog(paybackPackage))
              .writeToLog(DeliveryRecord.sentFrom(Address.MAIL_SERVICE)
                    .at(mailService.getGameTime())
                    .message(DeliveryRecord.Message.PAYBACK_FULFILLED))
              .get();

        return getMailService().getDeliveryManager()
              .startService(Delivery.draft()
                    .deliver(paymentPackage)
                    .from(paybackDelivery.getSender())
                    .to(paybackDelivery.getRecipient())
                    .startAtPhase(DeliveryPhase.DISPATCHING))
              .isSuccess();
    }
}