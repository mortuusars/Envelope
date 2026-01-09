package io.github.mortuusars.envelope.world.mail.entity.mail_service.payback_department;

import com.mojang.logging.LogUtils;
import io.github.mortuusars.envelope.Config;
import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.util.Ticks;
import io.github.mortuusars.envelope.world.delivery.Delivery;
import io.github.mortuusars.envelope.world.delivery.log.DeliveryLog;
import io.github.mortuusars.envelope.world.delivery.log.DeliveryRecord;
import io.github.mortuusars.envelope.world.delivery.phase.DeliveryPhase;
import io.github.mortuusars.envelope.world.item.PaybackPackageItem;
import io.github.mortuusars.envelope.world.item.component.PackageContents;
import io.github.mortuusars.envelope.world.item.component.RequestedPayback;
import io.github.mortuusars.envelope.world.mail.Mail;
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

    protected long getPaybackTimeoutTicksFor(Mail mail) {
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
        Mail mail = subject.mail();

        mail = mail.writeToLog(DeliveryRecord.returnedFrom(Address.MAIL_SERVICE)
              .at(getMailService().getGameTime())
              .message(DeliveryRecord.Message.PAYBACK_IS_TIMED_OUT));

        return getMailService().getDeliveryManager()
              .startService(Delivery.builder()
                    .deliver(mail)
                    .from(Address.MAIL_SERVICE)
                    .to(subject.returnAddress())
                    .atPhase(DeliveryPhase.RETURNING_TO_SENDER))
              .map(delivery -> true, error -> {
                  LOGGER.error("Cannot return timed out pending payback mail. It will be voided.");
                  return false;
              });
    }

    // --

    public boolean tryHandle(Delivery delivery) {
        if (delivery.getMail().hasPayback()) {
            handlePaybackSubject(delivery);
            return true;
        }

        if (delivery.getMail().getItem().getItem() instanceof PaybackPackageItem) {
            handlePaybackPayment(delivery);
            return true;
        }

        return false;
    }

    // -- Payback Subject

    protected void handlePaybackSubject(Delivery subjectDelivery) {
        Mail subject = subjectDelivery.getMail();

        subject.writeToLog(DeliveryRecord.arrivedTo(Address.MAIL_SERVICE)
              .at(getMailService().getGameTime())
              .message(DeliveryRecord.Message.WAITING_FOR_PAYMENT));

        MailId subjectId = MailId.createRandom();
        long timeoutTick = getMailService().getGameTime() + getPaybackTimeoutTicksFor(subject);
        PaybackSubject paybackSubject = new PaybackSubject(subject, subjectId, subjectDelivery.getSender(), timeoutTick);

        if (sendPaybackPackingBoxToBuyer(subjectDelivery, paybackSubject)) {
            awaitPayback(paybackSubject);
            subjectDelivery.setMail(Mail.EMPTY);
        } else {
            subject.writeToLog(DeliveryRecord.returnedFrom(Address.MAIL_SERVICE)
                  .message(DeliveryRecord.Message.PAYBACK_IS_NOT_VALID));
        }

        subjectDelivery.setPhaseAndResetProgress(DeliveryPhase.RETURNING_TO_SENDER);
    }

    protected boolean sendPaybackPackingBoxToBuyer(Delivery subjectDelivery, PaybackSubject paybackSubject) {
        ItemStack packingBox = new ItemStack(Envelope.Items.PAYBACK_PACKING_BOX.get());
        packingBox.set(Envelope.DataComponents.PAYBACK_PACKAGE_SUBJECT, paybackSubject);
        packingBox.set(Envelope.DataComponents.MAIL_RECIPIENT, subjectDelivery.getRecipient());

        return getMailService().getDeliveryManager()
              .startService(Delivery.builder()
                    .deliver(new Mail(packingBox, new DeliveryLog()
                          .append(DeliveryRecord.sentFrom(subjectDelivery.getSender())
                          .at(mailService.getGameTime()))))
                    .from(subjectDelivery.getSender())
                    .to(subjectDelivery.getRecipient())
                    .atPhase(DeliveryPhase.DISPATCHING))
              .isSuccess();
    }

    // -- Payback Payment

    protected void handlePaybackPayment(Delivery paybackDelivery) {
        Mail packingBox = paybackDelivery.getMail();
        packingBox.writeToLog(DeliveryRecord.arrivedTo(Address.MAIL_SERVICE).at(getMailService().getGameTime()));

        @Nullable PaybackSubject paybackSubject = packingBox.get(Envelope.DataComponents.PAYBACK_PACKAGE_SUBJECT);
        if (paybackSubject == null) {
            packingBox.writeToLog(DeliveryRecord.returnedFrom(Address.MAIL_SERVICE)
                  .message(DeliveryRecord.Message.PAYBACK_SUBJECT_NOT_FOUND));
            paybackDelivery.setPhaseAndResetProgress(DeliveryPhase.RETURNING_TO_SENDER);
            return;
        }

        paybackSubject = getPendingPaybackSubject(paybackSubject.id());
        if (paybackSubject == null) {
            packingBox.writeToLog(DeliveryRecord.returnedFrom(Address.MAIL_SERVICE)
                  .message(DeliveryRecord.Message.PAYBACK_SUBJECT_NOT_FOUND));
            paybackDelivery.setPhaseAndResetProgress(DeliveryPhase.RETURNING_TO_SENDER);
            return;
        }

        RequestedPayback requestedPayback = paybackSubject.mail().getOrDefault(Envelope.DataComponents.MAIL_REQUESTED_PAYBACK, RequestedPayback.DEFAULT);
        PackageContents packageContents = PackageContents.of(packingBox.getItem());
        if (!requestedPayback.matches(packageContents)) {
            packingBox.writeToLog(DeliveryRecord.returnedFrom(Address.MAIL_SERVICE)
                  .message(DeliveryRecord.Message.PAYBACK_IS_NOT_VALID));
            paybackDelivery.setPhaseAndResetProgress(DeliveryPhase.RETURNING_TO_SENDER);
            return;
        }

        if (sendPaymentPackageToSeller(paybackDelivery, packageContents, packingBox)) {
            removePendingPaybackSubject(paybackSubject.id());
            // Send the subject to buyer using same delivery and courier:
            paybackDelivery.setMail(paybackSubject.mail().writeToLog(DeliveryRecord.sentFrom(Address.MAIL_SERVICE)
                  .at(mailService.getGameTime())
                  .message(DeliveryRecord.Message.PAYBACK_FULFILLED)));
            paybackDelivery.setPhaseAndResetProgress(DeliveryPhase.RETURNING_TO_SENDER);
        } else {
            packingBox.writeToLog(DeliveryRecord.returnedFrom(Address.MAIL_SERVICE)
                  .message(DeliveryRecord.Message.PAYBACK_IS_NOT_VALID));
            paybackDelivery.setPhaseAndResetProgress(DeliveryPhase.RETURNING_TO_SENDER);
        }
    }

    protected boolean sendPaymentPackageToSeller(Delivery paybackDelivery, PackageContents packageContents, Mail packingBox) {
        ItemStack collectedPaybackPackage = new ItemStack(Envelope.Items.PACKAGE.get());
        collectedPaybackPackage.set(Envelope.DataComponents.PACKAGE_CONTENTS, packageContents);
        collectedPaybackPackage.set(Envelope.DataComponents.MAIL_RECIPIENT, packingBox.getRecipient());

        Mail mail = new Mail(collectedPaybackPackage, packingBox.getLog()
              .copy() // Transferring log from incoming mail
              .append(DeliveryRecord.sentFrom(Address.MAIL_SERVICE)
                    .at(mailService.getGameTime())
                    .message(DeliveryRecord.Message.PAYBACK_FULFILLED)));

        return getMailService().getDeliveryManager().startService(Delivery.builder()
                    .deliver(mail)
                    .from(paybackDelivery.getSender())
                    .to(paybackDelivery.getRecipient())
                    .atPhase(DeliveryPhase.DISPATCHING))
              .isSuccess();
    }
}