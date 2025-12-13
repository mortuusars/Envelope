package io.github.mortuusars.envelope.world.mail.entity;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.util.Ticks;
import io.github.mortuusars.envelope.world.delivery.Delivery;
import io.github.mortuusars.envelope.world.delivery.DeliveryOrigin;
import io.github.mortuusars.envelope.world.delivery.log.DeliveryRecord;
import io.github.mortuusars.envelope.world.item.PaybackPackageItem;
import io.github.mortuusars.envelope.world.item.component.PackageContents;
import io.github.mortuusars.envelope.world.item.component.Payback;
import io.github.mortuusars.envelope.world.item.component.StoredItemStack;
import io.github.mortuusars.envelope.world.mail.Mail;
import io.github.mortuusars.envelope.world.mail.MailId;
import io.github.mortuusars.envelope.world.mail.address.Address;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

public class MailService extends MailEntity {
    private static final Logger LOGGER = LogUtils.getLogger();

    private final ServerLevel level;
    private @Nullable MailServiceData data;

    public MailService(ServerLevel level) {
        super(Address.MAIL_SERVICE, 1000);
        this.level = level;
    }

    public @NotNull MailServiceData getData() {
        if (data == null) {
            data = MailServiceData.get(level, "envelope_mail_service_data");
        }
        return data;
    }

    public void tick() {
        //TODO: It would be a good idea to consider performance here.
        // Depending on mail counts this could get laggy, especially if multiple returns occur at the same time.

        boolean removed = getData().getMailAwaitingPayback().entrySet().removeIf(entry -> {
            if (entry.getValue().timeoutTick() <= level.getGameTime()) {
                returnTimedOutPaybackMail(entry.getValue().mail());
                return true;
            }
            return false;
        });

        if (removed) {
            getData().setDirty();
        }
    }

    protected void returnTimedOutPaybackMail(Mail mail) {
        mail = mail.writeToLog(DeliveryRecord.sentFrom(getAddress())
              .at(level.getGameTime())
              .withMessage(DeliveryRecord.Message.PAYBACK_IS_TIMED_OUT));

        Delivery.create(level, mail, getAddress(), mail.getSender(), DeliveryOrigin.service())
              .ifPresentOrElse(
                    delivery -> level.getEnvelopeContext().startServiceDelivery(delivery),
                    error -> error.log(LOGGER)
              );
    }

    @Override
    public Mail receiveMail(ServerLevel level, Mail mail) {
        if (mail.hasPayback()) {
            MailId subjectId = storePaybackSubject(level, mail);
            if (sendPaybackPackingBoxToBuyer(level, mail, subjectId)) {
                return Mail.EMPTY;
            } else {
                return mail.writeToLog(DeliveryRecord.returnedFrom(getAddress())
                      .withMessage(DeliveryRecord.Message.PAYBACK_IS_NOT_VALID));
            }
        }

        if (mail.getItemForReading().getItem() instanceof PaybackPackageItem) {
            MailId subjectId = mail.getOrDefault(Envelope.DataComponents.PAYBACK_SUBJECT, StoredItemStack.EMPTY)
                  .getForReading()
                  .getOrDefault(Envelope.DataComponents.PAYBACK_SUBJECT_ID, MailId.NIL);

            @Nullable MailAwaitingPayback mailAwaitingPayback = getData().getMailAwaitingPayback().get(subjectId);

            if (mailAwaitingPayback == null) {
                return mail.writeToLog(DeliveryRecord.returnedFrom(getAddress())
                      .withMessage(DeliveryRecord.Message.PAYBACK_SUBJECT_NOT_FOUND));
            }

            Payback payback = mailAwaitingPayback.mail().getOrDefault(Envelope.DataComponents.PAYBACK, Payback.DEFAULT);
            PackageContents packageContents = PackageContents.of(mail.getItemForReading());

            if (!payback.matches(packageContents)) {
                return mail.writeToLog(DeliveryRecord.returnedFrom(getAddress())
                      .withMessage(DeliveryRecord.Message.PAYBACK_IS_NOT_VALID));
            }

            //TODO: Dedicated payment package item? that shows the subject.
            ItemStack paymentPackage = new ItemStack(Envelope.Items.PACKAGE.get());
            paymentPackage.set(Envelope.DataComponents.PACKAGE_CONTENTS, packageContents);
            paymentPackage.set(Envelope.DataComponents.SENDER, mail.getSender());
            paymentPackage.set(Envelope.DataComponents.RECIPIENT, mail.getRecipient());

            return Delivery.create(level, new Mail(paymentPackage, mail.getLog()), Address.MAIL_SERVICE, mail.getRecipient(), DeliveryOrigin.service())
                  .map(
                        delivery -> {
                            level.getEnvelopeContext().startServiceDelivery(delivery);
                            getData().getMailAwaitingPayback().remove(subjectId);
                            // Return goods to the original recipient using the same courier:
                            return mailAwaitingPayback.mail().writeToLog(DeliveryRecord.sentFrom(getAddress())
                                  .withMessage(DeliveryRecord.Message.PAYBACK_FULFILLED));
                        },
                        error -> {
                            error.log(LOGGER);
                            return mail.writeToLog(DeliveryRecord.returnedFrom(getAddress())
                                  .withMessage(DeliveryRecord.Message.PAYBACK_IS_NOT_VALID));
                        }
                  );
        }

        return mail.writeToLog(DeliveryRecord.returnedFrom(getAddress()));
    }

    private MailId storePaybackSubject(ServerLevel level, Mail mail) {
        mail.writeToLog(DeliveryRecord
              .arrivedTo(getAddress())
              .at(level.getGameTime())
              .withMessage(DeliveryRecord.Message.WAITING_FOR_PAYMENT));

        return awaitPayback(level, mail);
    }

    private boolean sendPaybackPackingBoxToBuyer(ServerLevel level, Mail mail, MailId subjectId) {
        ItemStack subject = mail.getItemCopy();
        subject.set(Envelope.DataComponents.PAYBACK_SUBJECT_ID, subjectId);

        ItemStack box = new ItemStack(Envelope.Items.PAYBACK_PACKING_BOX.get());
        box.set(Envelope.DataComponents.PAYBACK_SUBJECT, new StoredItemStack(subject));
        box.set(Envelope.DataComponents.SENDER, mail.getSender());
        box.set(Envelope.DataComponents.RECIPIENT, mail.getRecipient());

        return Delivery.create(level, new Mail(box), getAddress(), mail.getRecipient(), DeliveryOrigin.service()).map(
              delivery -> {
                  level.getEnvelopeContext().startServiceDelivery(delivery);
                  return true;
              },
              error -> {
                  error.log(LOGGER);
                  return false;
              }
        );
    }

    protected MailId awaitPayback(ServerLevel level, Mail mail) {
        MailId id = MailId.createRandom();
        getData().getMailAwaitingPayback().put(id, new MailAwaitingPayback(mail, level.getGameTime() + Ticks.fromMinutes(30)));
        getData().setDirty();
        return id;
    }

    public record MailAwaitingPayback(Mail mail, long timeoutTick) {
        public static final Codec<MailAwaitingPayback> CODEC = RecordCodecBuilder.create(i -> i.group(
              Mail.CODEC.fieldOf("mail").forGetter(MailAwaitingPayback::mail),
              Codec.LONG.fieldOf("timeout_tick").forGetter(MailAwaitingPayback::timeoutTick)
        ).apply(i, MailAwaitingPayback::new));
    }
}