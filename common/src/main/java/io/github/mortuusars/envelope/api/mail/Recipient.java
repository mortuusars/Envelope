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

public record Recipient(String name, Optional<UUID> uuid, Type type) {
    public static final Codec<Recipient> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                    Codec.STRING.fieldOf("name").forGetter(Recipient::name),
                    UUIDUtil.CODEC.optionalFieldOf("uuid").forGetter(Recipient::uuid),
                    Type.CODEC.fieldOf("type").forGetter(Recipient::type))
            .apply(instance, Recipient::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, Recipient> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, Recipient::name,
            ByteBufCodecs.optional(UUIDUtil.STREAM_CODEC), Recipient::uuid,
            Type.STREAM_CODEC, Recipient::type,
            Recipient::new
    );

    public static Recipient player(Player player) {
        return new Recipient(player.getScoreboardName(), Optional.of(player.getUUID()), Type.PLAYER);
    }

    public static Recipient npc(String name) {
        return new Recipient(name, null, Type.NPC);
    }

    public Sender toSender() {
        return new Sender(name, uuid, type == Type.PLAYER ? Sender.Type.PLAYER : Sender.Type.NPC);
    }

    public enum Type implements StringRepresentable {
        PLAYER("player"),
        NPC("npc"),
        UNKNOWN("unknown");

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
