package io.github.mortuusars.envelope.world.mail.entity.mail_service;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.mortuusars.envelope.world.mail.Mail;

public record MailAwaitingPayback(Mail mail, long timeoutTick) {
    public static final Codec<MailAwaitingPayback> CODEC = RecordCodecBuilder.create(i -> i.group(
          Mail.CODEC.fieldOf("mail").forGetter(MailAwaitingPayback::mail),
          Codec.LONG.fieldOf("timeout_tick").forGetter(MailAwaitingPayback::timeoutTick)
    ).apply(i, MailAwaitingPayback::new));
}
