package io.github.mortuusars.envelope.world.mail;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.world.delivery.log.DeliveryLog;
import io.github.mortuusars.envelope.world.mail.address.Address;
import net.minecraft.Util;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

import java.util.Objects;

public class StoredMail {
    public static final Codec<StoredMail> CODEC = RecordCodecBuilder.create(i -> i.group(
          MailId.CODEC.fieldOf("id").forGetter(StoredMail::getId),
          ItemStack.OPTIONAL_CODEC.optionalFieldOf("item", ItemStack.EMPTY).forGetter(StoredMail::getItem),
          DeliveryLog.CODEC.optionalFieldOf("log", DeliveryLog.EMPTY).forGetter(StoredMail::getLog)
    ).apply(i, StoredMail::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, StoredMail> STREAM_CODEC = StreamCodec.composite(
          MailId.STREAM_CODEC, StoredMail::getId,
          ItemStack.STREAM_CODEC, StoredMail::getItem,
          DeliveryLog.STREAM_CODEC, StoredMail::getLog,
          StoredMail::new
    );

    public static final StoredMail EMPTY = new StoredMail(new MailId(Util.NIL_UUID), ItemStack.EMPTY, DeliveryLog.EMPTY);

    private final MailId id;
    private final ItemStack item;
    private final DeliveryLog log;

    public StoredMail(MailId id, ItemStack item, DeliveryLog log) {
        this.id = id;
        this.item = item;
        this.log = log;
    }

    public MailId getId() {
        return id;
    }

    public ItemStack getItem() {
        return item;
    }

    public DeliveryLog getLog() {
        return log;
    }

    public Address getSenderAddress() {
        return item.getOrDefault(Envelope.DataComponents.MAIL_SENDER, Address.UNKNOWN);
    }

    public boolean matches(MailId id) {
        return this.id.matches(id);
    }

    public boolean matches(StoredMail mail) {
        return this.id.matches(mail.getId());
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (StoredMail) obj;
        return Objects.equals(this.id, that.id) &&
              Objects.equals(this.item, that.item) &&
              Objects.equals(this.log, that.log);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, item, log);
    }

    @Override
    public String toString() {
        return "StoredMail[" +
              "id=" + id + ", " +
              "item=" + item + ", " +
              "log=" + log + ']';
    }
}
