package io.github.mortuusars.envelope.world.item.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.world.mail.address.Address;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

public record PaybackSubject(Id id, ItemStack mail, Address returnAddress, long timeoutTick) {
    public static final Codec<PaybackSubject> CODEC = RecordCodecBuilder.create(i -> i.group(
          Id.CODEC.fieldOf("id").forGetter(PaybackSubject::id),
          ItemStack.CODEC.fieldOf("mail").forGetter(PaybackSubject::mail),
          Address.CODEC.fieldOf("return_address").forGetter(PaybackSubject::returnAddress),
          Codec.LONG.fieldOf("timeout_tick").forGetter(PaybackSubject::timeoutTick)
    ).apply(i, PaybackSubject::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, PaybackSubject> STREAM_CODEC = StreamCodec.composite(
          Id.STREAM_CODEC, PaybackSubject::id,
          ItemStack.STREAM_CODEC, PaybackSubject::mail,
          Address.STREAM_CODEC, PaybackSubject::returnAddress,
          ByteBufCodecs.VAR_LONG, PaybackSubject::timeoutTick,
          PaybackSubject::new
    );

    public PaybackRequest getRequest() {
        return mail.getOrDefault(Envelope.DataComponents.MAIL_PAYBACK_REQUEST, PaybackRequest.DEFAULT);
    }
}