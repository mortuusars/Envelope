package io.github.mortuusars.envelope.world.mail.entity.mail_service;

import com.mojang.logging.LogUtils;
import io.github.mortuusars.envelope.Config;
import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.util.Ticks;
import io.github.mortuusars.envelope.world.delivery.Delivery;
import io.github.mortuusars.envelope.world.delivery.log.DeliveryRecord;
import io.github.mortuusars.envelope.world.item.PaybackPackageItem;
import io.github.mortuusars.envelope.world.item.component.PackageContents;
import io.github.mortuusars.envelope.world.item.component.Payback;
import io.github.mortuusars.envelope.world.item.component.StoredItemStack;
import io.github.mortuusars.envelope.world.mail.Mail;
import io.github.mortuusars.envelope.world.mail.MailId;
import io.github.mortuusars.envelope.world.mail.address.Address;
import io.github.mortuusars.envelope.world.service.MailService;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.function.Supplier;

public class MailServicePaybackDepartment {
    private static final Logger LOGGER = LogUtils.getLogger();

    private final MailService mailService;
    private final Supplier<PaybackDepartmentData> data;

    public MailServicePaybackDepartment(MailService context, Supplier<PaybackDepartmentData> data) {
        this.mailService = context;
        this.data = data;
    }

    public MailService getMailService() {
        return mailService;
    }

    public PaybackDepartmentData getData() {
        return data.get();
    }

    public int getMailAwaitingPaybackCount() {
        return getData().getMailAwaitingPayback().size();
    }

    protected MailAwaitingPayback getMailAwaitingPayback(MailId id) {
        return getData().getMailAwaitingPayback().get(id);
    }

    protected void removeMailAwaitingPayback(MailId subjectId) {
        getData().getMailAwaitingPayback().remove(subjectId);
        getData().setDirty();
    }

    protected MailId awaitPayback(Mail mail, long timeoutTicks) {
        mail = mail.writeToLog(DeliveryRecord.arrivedTo(Address.MAIL_SERVICE)
              .at(getMailService().getGameTime())
              .message(DeliveryRecord.Message.WAITING_FOR_PAYMENT));
        MailId id = MailId.createRandom();
        MailAwaitingPayback mailAwaitingPayback = new MailAwaitingPayback(mail, getMailService().getGameTime() + timeoutTicks);
        getData().getMailAwaitingPayback().put(id, mailAwaitingPayback);
        getData().setDirty();
        return id;
    }

    protected long getPaybackTimeoutTicksFor(Mail mail) {
        return Ticks.fromMinutes(Config.Server.DELIVERY_PAYBACK_TIMEOUT_MINUTES.get());
    }

    // --

    public boolean shouldHandle(Mail mail) {
        return mail.hasPayback() || mail.getItem().getItem() instanceof PaybackPackageItem;
    }

    public Mail handle(Mail mail) {
        if (mail.hasPayback()) {
            MailId subjectId = awaitPayback(mail, getPaybackTimeoutTicksFor(mail));

            if (sendPaybackPackingBoxToBuyer(mail, subjectId)) {
                return Mail.empty();
            } else {
                getData().getMailAwaitingPayback().remove(subjectId);
                return mail.writeToLog(DeliveryRecord.returnedFrom(Address.MAIL_SERVICE)
                      .message(DeliveryRecord.Message.PAYBACK_IS_NOT_VALID));
            }
        }

        if (mail.getItem().getItem() instanceof PaybackPackageItem) {
            MailId subjectId = mail.getOrDefault(Envelope.DataComponents.PAYBACK_SUBJECT, StoredItemStack.EMPTY)
                  .getForReading()
                  .getOrDefault(Envelope.DataComponents.PAYBACK_SUBJECT_ID, MailId.NIL);

            @Nullable MailAwaitingPayback mailAwaitingPayback = getMailAwaitingPayback(subjectId);

            if (mailAwaitingPayback == null) {
                return mail.writeToLog(DeliveryRecord.returnedFrom(Address.MAIL_SERVICE)
                      .message(DeliveryRecord.Message.PAYBACK_SUBJECT_NOT_FOUND));
            }

            Payback payback = mailAwaitingPayback.mail().getOrDefault(Envelope.DataComponents.PAYBACK_TAG, Payback.DEFAULT);
            PackageContents packageContents = PackageContents.of(mail.getItem());

            if (!payback.matches(packageContents)) {
                return mail.writeToLog(DeliveryRecord.returnedFrom(Address.MAIL_SERVICE)
                      .message(DeliveryRecord.Message.PAYBACK_IS_NOT_VALID));
            }

            //TODO: Dedicated payment package item? that shows the subject.
            ItemStack paymentPackage = new ItemStack(Envelope.Items.PACKAGE.get());
            paymentPackage.set(Envelope.DataComponents.PACKAGE_CONTENTS, packageContents);
            paymentPackage.set(Envelope.DataComponents.SENDER_ADDRESS, mail.getSenderAddress());
            paymentPackage.set(Envelope.DataComponents.ADDRESS_TAG, mail.getRecipient());

            mail.writeToLog(DeliveryRecord.arrivedTo(Address.MAIL_SERVICE).at(getMailService().getGameTime()));

            return getMailService().getDeliveryManager()
                  .startService(Delivery.builder().deliver(new Mail(paymentPackage, mail.getLog())).from(Address.MAIL_SERVICE).to(mail.getRecipient()))
                  .map(
                        delivery -> {
                            removeMailAwaitingPayback(subjectId);
                            // Return goods to the original recipient using the same courier:
                            return mailAwaitingPayback.mail().writeToLog(DeliveryRecord.sentFrom(Address.MAIL_SERVICE)
                                  .message(DeliveryRecord.Message.PAYBACK_FULFILLED));
                        },
                        error -> {
                            error.log(LOGGER);
                            return mail.writeToLog(DeliveryRecord.returnedFrom(Address.MAIL_SERVICE)
                                  .message(DeliveryRecord.Message.PAYBACK_IS_NOT_VALID));
                        }
                  );
        }

        return mail.writeToLog(DeliveryRecord.returnedFrom(Address.MAIL_SERVICE));
    }

    private boolean sendPaybackPackingBoxToBuyer(Mail mailAwaitingPayback, MailId subjectId) {
        ItemStack subject = mailAwaitingPayback.getItem().copy();
        subject.set(Envelope.DataComponents.PAYBACK_SUBJECT_ID, subjectId);

        ItemStack box = new ItemStack(Envelope.Items.PAYBACK_PACKING_BOX.get());
        box.set(Envelope.DataComponents.PAYBACK_SUBJECT, new StoredItemStack(subject));
        box.set(Envelope.DataComponents.SENDER_ADDRESS, mailAwaitingPayback.getSenderAddress());

        Mail mail = new Mail(box);

        return getMailService().getDeliveryManager()
              .startService(Delivery.builder().deliver(mail).from(Address.MAIL_SERVICE).to(mailAwaitingPayback.getRecipient()))
              .isSuccess();
    }

    // --

    public void tick() {
        //TODO: It would be a good idea to consider performance here.
        // Depending on mail counts this could get laggy, especially if multiple returns occur at the same time.

        if (getMailService().getGameTime() % 200 == 0) { // Check every minute
            getData().getMailAwaitingPayback().entrySet().removeIf(entry -> {
                if (entry.getValue().timeoutTick() <= getMailService().getGameTime()) {
                    returnAsTimedOut(entry.getValue().mail());
                    getData().setDirty();
                    return true;
                }
                return false;
            });
        }
    }

    public int returnAllAwaitingAsTimedOut() {
        int count = 0;
        for (MailAwaitingPayback mail : getData().getMailAwaitingPayback().values()) {
            if (returnAsTimedOut(mail.mail())) {
                count++;
            }
        }
        getData().getMailAwaitingPayback().clear();
        getData().setDirty();
        return count;
    }

    public boolean returnAsTimedOut(Mail mail) {
        mail = mail.writeToLog(DeliveryRecord.returnedFrom(Address.MAIL_SERVICE)
              .at(getMailService().getGameTime())
              .message(DeliveryRecord.Message.PAYBACK_IS_TIMED_OUT));

        return getMailService().getDeliveryManager()
              .startService(Delivery.builder().deliver(mail).from(Address.MAIL_SERVICE).to(mail.getSenderAddress()))
              .isSuccess();
    }
}
