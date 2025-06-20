package io.github.mortuusars.envelope.api.mail;

import com.google.common.base.Preconditions;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public record Mail(Sender sender, Recipient recipient, ItemStack content, long sentAt, int travelTime, Status status) {
    public Mail {
        Preconditions.checkArgument(!content.isEmpty(), "Content cannot be empty!");
    }

    public static final Codec<Mail> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Sender.CODEC.fieldOf("sender").forGetter(Mail::sender),
            Recipient.CODEC.fieldOf("recipient").forGetter(Mail::recipient),
            ItemStack.CODEC.fieldOf("content").forGetter(Mail::content),
            Codec.LONG.fieldOf("sent_at").forGetter(Mail::sentAt),
            Codec.INT.fieldOf("travel_time").forGetter(Mail::travelTime),
            Status.CODEC.fieldOf("status").forGetter(Mail::status)
    ).apply(instance, Mail::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, Mail> STREAM_CODEC = StreamCodec.of(
            (buffer, mail) -> {
                Sender.STREAM_CODEC.encode(buffer, mail.sender());
                Recipient.STREAM_CODEC.encode(buffer, mail.recipient());
                ItemStack.STREAM_CODEC.encode(buffer, mail.content());
                ByteBufCodecs.VAR_LONG.encode(buffer, mail.sentAt());
                ByteBufCodecs.VAR_INT.encode(buffer, mail.travelTime());
                Status.STREAM_CODEC.encode(buffer, mail.status());
            },
            buffer -> new Mail(
                    Sender.STREAM_CODEC.decode(buffer),
                    Recipient.STREAM_CODEC.decode(buffer),
                    ItemStack.STREAM_CODEC.decode(buffer),
                    ByteBufCodecs.VAR_LONG.decode(buffer),
                    ByteBufCodecs.VAR_INT.decode(buffer),
                    Status.STREAM_CODEC.decode(buffer))
    );

    // --

    public enum Status implements StringRepresentable {
        REGULAR("regular"),
        RETURNED("returned"),
        REJECTED("rejected"),
        UNCLAIMED("unclaimed");

        public static final Codec<Status> CODEC = StringRepresentable.fromEnum(Status::values);
        public static final StreamCodec<ByteBuf, Status> STREAM_CODEC =
                ByteBufCodecs.idMapper(ByIdMap.continuous(Status::ordinal, values(), ByIdMap.OutOfBoundsStrategy.ZERO), Status::ordinal);

        private final String name;

        Status(String name) {
            this.name = name;
        }

        @Override
        public @NotNull String getSerializedName() {
            return name;
        }

        public MutableComponent translate() {
            return Component.translatable("gui.envelope.mail.status." + name);
        }
    }
}
