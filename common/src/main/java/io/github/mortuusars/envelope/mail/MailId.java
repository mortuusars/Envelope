package io.github.mortuusars.envelope.mail;

import com.mojang.serialization.Codec;
import io.github.mortuusars.envelope.Envelope;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

public record MailId(UUID id) {
    public static final Codec<MailId> CODEC = UUIDUtil.CODEC.xmap(MailId::new, MailId::id);
    public static final StreamCodec<ByteBuf, MailId> STREAM_CODEC = UUIDUtil.STREAM_CODEC.map(MailId::new, MailId::id);

    public static MailId createRandom() {
        return new MailId(UUID.randomUUID());
    }

    public static Optional<MailId> from(ItemStack mail) {
        return Optional.ofNullable(mail.get(Envelope.DataComponents.MAIL_ID));
    }

    public boolean matches(MailId another) {
        return id.equals(another.id);
    }

    public boolean matches(UUID another) {
        return id.equals(another);
    }

    public boolean matches(ItemStack stack) {
        @Nullable MailId another = stack.get(Envelope.DataComponents.MAIL_ID);
        if (another == null) return false;
        return matches(another);
    }
}
