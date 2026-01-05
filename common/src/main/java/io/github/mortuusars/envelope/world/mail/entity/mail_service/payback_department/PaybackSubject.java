package io.github.mortuusars.envelope.world.mail.entity.mail_service.payback_department;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.mortuusars.envelope.world.mail.Mail;
import io.github.mortuusars.envelope.world.mail.MailId;
import io.github.mortuusars.envelope.world.mail.address.Address;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record PaybackSubject(Mail mail, MailId id, Address returnAddress, long timeoutTick) {
    public static final Codec<PaybackSubject> CODEC = RecordCodecBuilder.create(i -> i.group(
          Mail.CODEC.fieldOf("mail").forGetter(PaybackSubject::mail),
          MailId.CODEC.fieldOf("id").forGetter(PaybackSubject::id),
          Address.CODEC.fieldOf("return_address").forGetter(PaybackSubject::returnAddress),
          Codec.LONG.fieldOf("timeout_tick").forGetter(PaybackSubject::timeoutTick)
    ).apply(i, PaybackSubject::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, PaybackSubject> STREAM_CODEC = StreamCodec.composite(
          Mail.STREAM_CODEC, PaybackSubject::mail,
          MailId.STREAM_CODEC, PaybackSubject::id,
          Address.STREAM_CODEC, PaybackSubject::returnAddress,
          ByteBufCodecs.VAR_LONG, PaybackSubject::timeoutTick,
          PaybackSubject::new
    );
}
