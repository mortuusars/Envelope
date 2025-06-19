package io.github.mortuusars.envelope.api.mail;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.UUID;

public record Sender(String name, Optional<UUID> uuid, Type type) {
    public static final Codec<Sender> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                    Codec.STRING.fieldOf("name").forGetter(Sender::name),
                    UUIDUtil.CODEC.optionalFieldOf("uuid").forGetter(Sender::uuid),
                    Type.CODEC.fieldOf("type").forGetter(Sender::type))
            .apply(instance, Sender::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, Sender> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, Sender::name,
            ByteBufCodecs.optional(UUIDUtil.STREAM_CODEC), Sender::uuid,
            Sender.Type.STREAM_CODEC, Sender::type,
            Sender::new
    );

    public static Sender player(Player player) {
        return new Sender(player.getScoreboardName(), Optional.of(player.getUUID()), Type.PLAYER);
    }

    public static Sender npc(String name) {
        return new Sender(name, null, Type.NPC);
    }

    public Recipient toRecipient() {
        return new Recipient(name, uuid, type == Type.PLAYER ? Recipient.Type.PLAYER : Recipient.Type.NPC);
    }

    // --

    public enum Type implements StringRepresentable {
        PLAYER("player"),
        NPC("npc");

        public static final Codec<Type> CODEC = StringRepresentable.fromEnum(Type::values);
        public static final StreamCodec<ByteBuf, Type> STREAM_CODEC =
                ByteBufCodecs.idMapper(ByIdMap.continuous(Type::ordinal, values(), ByIdMap.OutOfBoundsStrategy.ZERO), Type::ordinal);

        private final String name;

        Type(String npc) {
            this.name = npc;
        }

        @Override
        public @NotNull String getSerializedName() {
            return name;
        }
    }
}
