package io.github.mortuusars.envelope.world.mail.entity;

import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.world.delivery.log.DeliveryRecord;
import io.github.mortuusars.envelope.world.item.LetterItem;
import io.github.mortuusars.envelope.world.item.component.LetterContent;
import io.github.mortuusars.envelope.world.item.component.seal.Seal;
import io.github.mortuusars.envelope.world.item.component.seal.SealImpression;
import io.github.mortuusars.envelope.world.item.component.seal.SealMaterial;
import io.github.mortuusars.envelope.world.mail.Mail;
import io.github.mortuusars.envelope.world.mail.address.Address;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public class VillagerMailEntity extends MailEntity {
    public VillagerMailEntity(Address.Entity address, int distance) {
        super(address, distance);
    }

    @Override
    public Mail receiveMail(ServerLevel level, Mail mail) {
        if (!(mail.getItemForReading().getItem() instanceof LetterItem)) {
            return mail.writeToLog(DeliveryRecord.returnedFrom(getAddress()));
        }

        @Nullable Address sender = mail.get(Envelope.DataComponents.SENDER);
        if (sender != null) {
            ItemStack letter = new ItemStack(Envelope.Items.SEALED_LETTER.get());
            letter.set(Envelope.DataComponents.LETTER_CONTENT,
                  new LetterContent(Component.literal("uhhh\n\nI got your letter. it was nice. thanks.")));
            letter.set(Envelope.DataComponents.SEAL, new Seal(
                  SealMaterial.getHolder(level.registryAccess(), SealMaterial.RED_WAX),
                  SealImpression.getHolder(level.registryAccess(), SealImpression.VILLAGER),
                  Component.translatable("entity.minecraft.villager")
            ));
            letter.set(Envelope.DataComponents.SENDER, getAddress());
            letter.set(Envelope.DataComponents.RECIPIENT, sender);

            return new Mail(letter).writeToLog(DeliveryRecord.sentFrom(getAddress()).at(level.getGameTime()));
        }

        return Mail.EMPTY;
    }
}
