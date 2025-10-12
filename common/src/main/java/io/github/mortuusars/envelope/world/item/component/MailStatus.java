package io.github.mortuusars.envelope.world.item.component;

import com.mojang.serialization.Codec;
import io.github.mortuusars.envelope.Envelope;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public enum MailStatus implements StringRepresentable {
    RETURNED("returned"),
    REJECTED("rejected"),
    UNCLAIMED("unclaimed");

    public static final Codec<MailStatus> CODEC = StringRepresentable.fromEnum(MailStatus::values);
    public static final StreamCodec<ByteBuf, MailStatus> STREAM_CODEC =
        ByteBufCodecs.idMapper(ByIdMap.continuous(MailStatus::ordinal, MailStatus.values(), ByIdMap.OutOfBoundsStrategy.ZERO), MailStatus::ordinal);

    private final String name;

    MailStatus(String name) {
        this.name = name;
    }

    public static Optional<MailStatus> of(ItemStack stack) {
        return Optional.ofNullable(stack.get(Envelope.DataComponents.MAIL_STATUS));
    }

    @Override
    public @NotNull String getSerializedName() {
        return name;
    }

    public MutableComponent translate() {
        return Component.translatable("gui.envelope.mail.status." + getSerializedName());
    }
}
