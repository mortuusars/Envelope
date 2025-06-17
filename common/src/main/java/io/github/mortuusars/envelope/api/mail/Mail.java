package io.github.mortuusars.envelope.api.mail;

import com.google.common.base.Preconditions;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public record Mail(Sender sender, Recipient recipient, ItemStack content, long sentAt, int travelDuration, Status status) {
    public Mail {
        Preconditions.checkArgument(!content.isEmpty(), "Content cannot be empty!");
    }

    public static final Codec<Mail> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Sender.CODEC.fieldOf("sender").forGetter(Mail::sender),
            Recipient.CODEC.fieldOf("recipient").forGetter(Mail::recipient),
            ItemStack.CODEC.fieldOf("content").forGetter(Mail::content),
            Codec.LONG.fieldOf("sent_at").forGetter(Mail::sentAt),
            Codec.INT.fieldOf("travel_duration").forGetter(Mail::travelDuration),
            Status.CODEC.fieldOf("status").forGetter(Mail::status)
    ).apply(instance, Mail::new));

    // --

    public enum Status implements StringRepresentable {
        REGULAR("regular"),
        RETURNED("returned"),
        REJECTED("rejected");

        public static final Codec<Status> CODEC = StringRepresentable.fromEnum(Status::values);

        private final String name;

        Status(String name) {
            this.name = name;
        }

        @Override
        public @NotNull String getSerializedName() {
            return name;
        }
    }
}
