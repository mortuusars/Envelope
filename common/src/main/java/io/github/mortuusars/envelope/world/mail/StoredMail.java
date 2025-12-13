package io.github.mortuusars.envelope.world.mail;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.mortuusars.envelope.world.delivery.log.DeliveryLog;
import net.minecraft.Util;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

public class StoredMail extends Mail {
    public static final Codec<StoredMail> CODEC = RecordCodecBuilder.create(i -> i.group(
          ItemStack.OPTIONAL_CODEC.optionalFieldOf("item", ItemStack.EMPTY).forGetter(StoredMail::getItemForReading),
          DeliveryLog.CODEC.optionalFieldOf("log", DeliveryLog.EMPTY).forGetter(StoredMail::getLog),
          MailId.CODEC.fieldOf("id").forGetter(StoredMail::getId)
    ).apply(i, StoredMail::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, StoredMail> STREAM_CODEC = StreamCodec.composite(
          ItemStack.STREAM_CODEC, StoredMail::getItemForReading,
          DeliveryLog.STREAM_CODEC, StoredMail::getLog,
          MailId.STREAM_CODEC, StoredMail::getId,
          StoredMail::new
    );

    public static final StoredMail EMPTY = new StoredMail(ItemStack.EMPTY, DeliveryLog.EMPTY, new MailId(Util.NIL_UUID));

    private final MailId id;

    public StoredMail(ItemStack stack, DeliveryLog deliveryLog, MailId id) {
        super(stack, deliveryLog);
        this.id = id;
    }

    public MailId getId() {
        return id;
    }

    @Override
    public boolean isEmpty() {
        return this == EMPTY || super.isEmpty();
    }

    public boolean matches(MailId id) {
        return this.id.matches(id);
    }

    public boolean matches(StoredMail mail) {
        return this.id.matches(mail.getId());
    }
}
