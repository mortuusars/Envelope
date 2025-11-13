package io.github.mortuusars.envelope.world.mail;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.StreamCodec;

import java.util.UUID;

public record MailId(UUID id) {
    public static final Codec<MailId> CODEC = UUIDUtil.CODEC.xmap(MailId::new, MailId::id);
    public static final StreamCodec<ByteBuf, MailId> STREAM_CODEC = UUIDUtil.STREAM_CODEC.map(MailId::new, MailId::id);

    public static MailId createRandom() {
        return new MailId(UUID.randomUUID());
    }

    public boolean matches(MailId another) {
        return id.equals(another.id);
    }

    public boolean matches(UUID another) {
        return id.equals(another);
    }
}