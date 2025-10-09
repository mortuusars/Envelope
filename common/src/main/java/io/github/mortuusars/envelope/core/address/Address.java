package io.github.mortuusars.envelope.core.address;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntFunction;

public interface Address {
    Address MAIL_SERVICE = new Npc("‹Mail Service›", Component.translatable("address.envelope.mail_service"));
    Address UNKNOWN = new Npc("‹Unknown›", Component.translatable("address.envelope.unknown"));

    Codec<Address> CODEC = Type.CODEC.dispatch(Address::type, Type::getCodec);
    StreamCodec<RegistryFriendlyByteBuf, Address> STREAM_CODEC = Type.STREAM_CODEC.dispatch(Address::type, Type::getStreamCodec);

    Type type();
    String id();
    MutableComponent getDisplayName();

    default boolean matchesName(String name) {
        return getDisplayName().getString().equalsIgnoreCase(name);
    }

    default Address ifPigeonhole(Consumer<Pigeonhole> consumer) {
        if (this instanceof Pigeonhole pigeonhole) {
            consumer.accept(pigeonhole);
        }
        return this;
    }

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

    default <R> R map(Function<Pigeonhole, R> ifPigeonhole, Function<Player, R> ifPlayer, Function<Npc, R> ifNpc) {
        return switch (this) {
            case Pigeonhole pigeonhole -> ifPigeonhole.apply(pigeonhole);
            case Player player -> ifPlayer.apply(player);
            case Npc npc -> ifNpc.apply(npc);
            default -> throw new IllegalStateException("Unknown type of address. " + this.getClass());
        };
    }

    record Pigeonhole(String id) implements Address {
        public static final MapCodec<Pigeonhole> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Codec.STRING.fieldOf("id").forGetter(Pigeonhole::id)
        ).apply(instance, Pigeonhole::new));

        public static final Codec<Pigeonhole> STRING_CODEC = Codec.STRING.xmap(Pigeonhole::new, Pigeonhole::id);

        public static final StreamCodec<RegistryFriendlyByteBuf, Pigeonhole> STREAM_CODEC =
                ByteBufCodecs.STRING_UTF8.map(Pigeonhole::new, Pigeonhole::id).cast();

        @Override
        public Type type() {
            return Type.PIGEONHOLE;
        }

        @Override
        public MutableComponent getDisplayName() {
            return Component.literal(id);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Pigeonhole that = (Pigeonhole) o;
            return this.id.equalsIgnoreCase(that.id);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode("p" + id.toLowerCase(Locale.ROOT));
        }
    }

    record Player(String id) implements Address {
        public static final MapCodec<Player> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Codec.STRING.fieldOf("id").forGetter(Player::id)
        ).apply(instance, Player::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, Player> STREAM_CODEC =
                ByteBufCodecs.STRING_UTF8.map(Player::new, Player::id).cast();

        public Player(net.minecraft.world.entity.player.Player player) {
            this(player.getScoreboardName());
        }

        @Override
        public Type type() {
            return Type.PLAYER;
        }

        @Override
        public MutableComponent getDisplayName() {
            return Component.literal(id);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Player that = (Player) o;
            return this.id.equalsIgnoreCase(that.id);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode("pl" + id.toLowerCase(Locale.ROOT));
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

        public Npc(String id) {
            this(id, Optional.empty());
        }

        @Override
        public Type type() {
            return Type.NPC;
        }

        @Override
        public MutableComponent getDisplayName() {
            return displayName.map(component -> Component.empty().append(component))
                    .orElseGet(() -> Component.literal(id));
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Npc that = (Npc) o;
            return this.id.equalsIgnoreCase(that.id);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode("npc" + id.toLowerCase(Locale.ROOT));
        }
    }

    enum Type implements StringRepresentable {
        PIGEONHOLE(0, "pigeonhole", Pigeonhole.CODEC, Pigeonhole.STREAM_CODEC),
        PLAYER(1, "player", Player.CODEC, Player.STREAM_CODEC),
        NPC(2, "npc", Npc.CODEC, Npc.STREAM_CODEC);

        public static final Codec<Type> CODEC = StringRepresentable.fromEnum(Type::values);
        public static final IntFunction<Type> BY_ID = ByIdMap.continuous(Type::getId, values(), ByIdMap.OutOfBoundsStrategy.ZERO);
        public static final StreamCodec<RegistryFriendlyByteBuf, Type> STREAM_CODEC = ByteBufCodecs.idMapper(BY_ID, Type::ordinal).cast();

        private final int id;
        private final String name;
        private final MapCodec<? extends Address> codec;
        private final StreamCodec<RegistryFriendlyByteBuf, ? extends Address> streamCodec;

        Type(int id, String name, MapCodec<? extends Address> codec, StreamCodec<RegistryFriendlyByteBuf, ? extends Address> streamCodec) {
            this.id = id;
            this.name = name;
            this.codec = codec;
            this.streamCodec = streamCodec;
        }

        public int getId() {
            return id;
        }

        @Override
        public @NotNull String getSerializedName() {
            return name;
        }

        public MapCodec<? extends Address> getCodec() {
            return codec;
        }

        public StreamCodec<RegistryFriendlyByteBuf, ? extends Address> getStreamCodec() {
            return streamCodec;
        }
    }
}
