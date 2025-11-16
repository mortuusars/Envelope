package io.github.mortuusars.envelope.world.mail.address;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.mortuusars.envelope.world.mail.receiver.EntityMailReceiver;
import io.github.mortuusars.envelope.world.mail.Mail;
import io.github.mortuusars.envelope.world.mail.receiver.PigeonholeMailReceiver;
import io.github.mortuusars.envelope.world.mail.receiver.PlayerMailReceiver;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.IntFunction;

public interface Address {
    Address MAIL_SERVICE = new Entity("Mail Service", Component.translatable("address.envelope.mail_service"));
    Address UNKNOWN = new Entity("Unknown", Component.translatable("address.envelope.unknown"));

    Codec<Address> CODEC = Type.CODEC.dispatch(Address::type, Type::getCodec);
    StreamCodec<RegistryFriendlyByteBuf, Address> STREAM_CODEC = Type.STREAM_CODEC.dispatch(Address::type, Type::getStreamCodec);

    Type type();

    String id();

    /**
     * Compares "display" name of a current address to the given string (ignoring the case).<br>
     * Nothing special for Pigeonhole and Player addresses, but Entity address will be compared by its translation, if present.
     * @return {@code  true} if names look the same (ignoring the case).
     */
    default boolean matches(String name) {
        return toString().equalsIgnoreCase(name);
    }

    /**
     * Shortcut for {@link Address#matches(String)}
     */
    default boolean matches(Address address) {
        return matches(address.toString());
    }

    // --

    /**
     * @return "Display" name of an address. For Pigeonhole and Player addresses that's just id.<br>
     * Entity address returns its translation, if present.
     */
    default MutableComponent getName() {
        return Component.literal(id());
    }

    /**
     * @return "Display" name of an address. For Pigeonhole and Player addresses that's just id.<br>
     * Entity address returns its translation, if present.
     */
    String toString();

    default AddressFormatter format() {
        return AddressFormatter.of(this);
    }

    default <R> R map(Function<Pigeonhole, R> ifPigeonhole, Function<Player, R> ifPlayer, Function<Entity, R> ifEntity) {
        return switch (this) {
            case Pigeonhole pigeonhole -> ifPigeonhole.apply(pigeonhole);
            case Player player -> ifPlayer.apply(player);
            case Entity entity -> ifEntity.apply(entity);
            default -> throw new IllegalStateException("Unknown type of address. " + this.getClass());
        };
    }

    default Mail receiveMail(ServerLevel level, Mail mail) {
        if (mail.isEmpty()) return Mail.EMPTY;
        return map(PigeonholeMailReceiver::new, PlayerMailReceiver::new, EntityMailReceiver::new).receiveMail(level, mail);
    }

    enum Type implements StringRepresentable {
        PIGEONHOLE(0, "pigeonhole", Pigeonhole.CODEC, Pigeonhole.STREAM_CODEC),
        PLAYER(1, "player", Player.CODEC, Player.STREAM_CODEC),
        ENTITY(2, "entity", Entity.CODEC, Entity.STREAM_CODEC);

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

        public MutableComponent translate() {
            return Component.translatable("address.envelope.type." + getSerializedName());
        }
    }

    record Pigeonhole(String id) implements Address {
        public static final MapCodec<Pigeonhole> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
              Codec.STRING.fieldOf("id").forGetter(Pigeonhole::id)
        ).apply(instance, Pigeonhole::new));

        public static final Codec<Pigeonhole> STRING_CODEC = Codec.STRING.xmap(Pigeonhole::new, Pigeonhole::id);

        public static final StreamCodec<RegistryFriendlyByteBuf, Pigeonhole> STREAM_CODEC =
              ByteBufCodecs.STRING_UTF8.map(Pigeonhole::new, Pigeonhole::id).cast();

        public Pigeonhole(String id) {
            this.id = id.trim();
        }

        @Override
        public Type type() {
            return Type.PIGEONHOLE;
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
            return ("p" + id.toLowerCase(Locale.ROOT)).hashCode();
        }

        @Override
        public String toString() {
            return id;
        }
    }

    record Player(String id) implements Address {
        public static final MapCodec<Player> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
              Codec.STRING.fieldOf("id").forGetter(Player::id)
        ).apply(instance, Player::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, Player> STREAM_CODEC =
              ByteBufCodecs.STRING_UTF8.map(Player::new, Player::id).cast();

        public Player(String id) {
            this.id = id.trim();
        }

        public Player(net.minecraft.world.entity.player.Player player) {
            this(player.getScoreboardName());
        }

        @Override
        public Type type() {
            return Type.PLAYER;
        }

        @Override
        public String toString() {
            return id;
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
            return ("pl" + id.toLowerCase(Locale.ROOT)).hashCode();
        }
    }

    /**
     * Due to how address matching is implemented (by display name), and how choosing address in GUIs, etc, works -<br>
     * using translations in Entity addresses works as it should.<br>
     */
    record Entity(String id, Optional<Component> displayName) implements Address {
        public static final MapCodec<Entity> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
              Codec.STRING.fieldOf("id").forGetter(Entity::id),
              ComponentSerialization.CODEC.optionalFieldOf("display_name").forGetter(Entity::displayName)
        ).apply(instance, Entity::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, Entity> STREAM_CODEC = StreamCodec.composite(
              ByteBufCodecs.STRING_UTF8, Entity::id,
              ByteBufCodecs.optional(ComponentSerialization.STREAM_CODEC), Entity::displayName,
              Entity::new
        );

        public Entity(String id, Optional<Component> displayName) {
            this.id = id.trim();
            this.displayName = displayName;
        }

        public Entity(String id, @NotNull Component displayName) {
            this(id, Optional.of(displayName));
        }

        public Entity(String id) {
            this(id, Optional.empty());
        }

        @Override
        public Type type() {
            return Type.ENTITY;
        }

        @Override
        public MutableComponent getName() {
            return displayName.map(Component::plainCopy).orElseGet(Address.super::getName);
        }

        @Override
        public String toString() {
            return getName().getString();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Entity that = (Entity) o;
            return this.id.equalsIgnoreCase(that.id);
        }

        @Override
        public int hashCode() {
            return ("n" + id.toLowerCase(Locale.ROOT)).hashCode();
        }
    }
}
