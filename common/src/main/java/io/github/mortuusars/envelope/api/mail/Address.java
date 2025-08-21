package io.github.mortuusars.envelope.api.mail;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;

public interface Address {
    Address MAIL_SERVICE = new Npc("<Mail Service>", Component.translatable("address.envelope.mail_service"));
    Address UNKNOWN = new Npc("<Unknown>", Component.translatable("address.envelope.unknown"));

    Codec<Address> CODEC = Type.CODEC.dispatch(Address::type, Type::getCodec);
    StreamCodec<RegistryFriendlyByteBuf, Address> STREAM_CODEC = Type.STREAM_CODEC.dispatch(Address::type, Type::getStreamCodec);

    Type type();
    String id();
    Component getDisplayName();

    default Address ifPlayer(Consumer<Player> consumer) {
        if (this instanceof Player player) {
            consumer.accept(player);
        }
        return this;
    }

    default Address ifNpc(Consumer<Npc> consumer) {
        if (this instanceof Npc npc) {
            consumer.accept(npc);
        }
        return this;
    }

    default Address ifPigeonhole(Consumer<Pigeonhole> consumer) {
        if (this instanceof Pigeonhole pigeonhole) {
            consumer.accept(pigeonhole);
        }
        return this;
    }

    default <R> R map(Function<Player, R> ifPlayer, Function<Npc, R> ifNpc, Function<Pigeonhole, R> ifPigeonhole) {
        return switch (this) {
            case Player player -> ifPlayer.apply(player);
            case Npc npc -> ifNpc.apply(npc);
            case Pigeonhole pigeonhole -> ifPigeonhole.apply(pigeonhole);
            default -> throw new IllegalStateException("Unknown type of address. " + this.getClass());
        };
    }

    record Player(String id, Optional<UUID> uuid) implements Address {
        public static final MapCodec<Player> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Codec.STRING.fieldOf("id").forGetter(Player::id),
                UUIDUtil.CODEC.optionalFieldOf("uuid").forGetter(Player::uuid)
        ).apply(instance, Player::new));

        public static final StreamCodec<ByteBuf, Player> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, Player::id,
                ByteBufCodecs.optional(UUIDUtil.STREAM_CODEC), Player::uuid,
                Player::new
        );

        public Player(net.minecraft.world.entity.player.Player player) {
            this(player.getScoreboardName(), Optional.of(player.getUUID()));
        }

        @Override
        public Type type() {
            return Type.PLAYER;
        }

        @Override
        public String id() {
            return id;
        }

        @Override
        public Component getDisplayName() {
            return Component.literal(id);
        }
    }

    record Npc(String id, Optional<Component> displayName) implements Address {
        public static final MapCodec<Npc> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Codec.STRING.fieldOf("id").forGetter(Npc::id),
                ComponentSerialization.CODEC.optionalFieldOf("display_name").forGetter(Npc::displayName)
        ).apply(instance, Npc::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, Npc> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, Npc::id,
                ByteBufCodecs.optional(ComponentSerialization.STREAM_CODEC), Npc::displayName,
                Npc::new
        );

        public Npc(String id, Component displayName) {
            this(id, Optional.ofNullable(displayName));
        }

        @Override
        public Type type() {
            return Type.PLAYER;
        }

        @Override
        public String id() {
            return id;
        }

        @Override
        public Component getDisplayName() {
            return Component.literal(id);
        }
    }

    record Pigeonhole(String id) implements Address {
        public static final MapCodec<Pigeonhole> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Codec.STRING.fieldOf("id").forGetter(Pigeonhole::id)
        ).apply(instance, Pigeonhole::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, Pigeonhole> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, Pigeonhole::id,
                Pigeonhole::new
        );

        @Override
        public Type type() {
            return Type.PIGEONHOLE;
        }

        @Override
        public Component getDisplayName() {
            return Component.literal(id);
        }
    }

    enum Type implements StringRepresentable {
        PLAYER("player", Player.CODEC, Player.STREAM_CODEC.cast()),
        NPC("npc", Npc.CODEC, Npc.STREAM_CODEC),
        PIGEONHOLE("pigeonhole", Pigeonhole.CODEC, Pigeonhole.STREAM_CODEC);

        public static final Codec<Type> CODEC = StringRepresentable.fromEnum(Type::values);
        public static final StreamCodec<RegistryFriendlyByteBuf, Type> STREAM_CODEC =
                ByteBufCodecs.idMapper(ByIdMap.continuous(Type::ordinal, values(), ByIdMap.OutOfBoundsStrategy.ZERO), Type::ordinal).cast();

        private final String name;
        private final MapCodec<? extends Address> codec;
        private final StreamCodec<RegistryFriendlyByteBuf, ? extends Address> streamCodec;

        Type(String name, MapCodec<? extends Address> codec, StreamCodec<RegistryFriendlyByteBuf, ? extends Address> streamCodec) {
            this.name = name;
            this.codec = codec;
            this.streamCodec = streamCodec;
        }

        public MapCodec<? extends Address> getCodec() {
            return codec;
        }

        public StreamCodec<RegistryFriendlyByteBuf, ? extends Address> getStreamCodec() {
            return streamCodec;
        }

        @Override
        public @NotNull String getSerializedName() {
            return name;
        }
    }
}
