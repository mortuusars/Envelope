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

public interface Address {
    Address MAIL_SERVICE = new Mailbox("<Mail Service>", Component.translatable("address.envelope.mail_service"));
    Address UNKNOWN = new Mailbox("<Unknown>", Component.translatable("address.envelope.unknown"));

    Codec<Address> CODEC = Type.CODEC.dispatch(Address::type, Type::getCodec);
    StreamCodec<RegistryFriendlyByteBuf, Address> STREAM_CODEC = Type.STREAM_CODEC.dispatch(Address::type, Type::getStreamCodec);

    Type type();
    String id();
    Component getDisplayName();

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

    record Mailbox(String id, Optional<Component> displayName) implements Address {
        public static final MapCodec<Mailbox> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Codec.STRING.fieldOf("id").forGetter(Mailbox::id),
                ComponentSerialization.CODEC.optionalFieldOf("display_name").forGetter(Mailbox::displayName)
        ).apply(instance, Mailbox::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, Mailbox> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, Mailbox::id,
                ByteBufCodecs.optional(ComponentSerialization.STREAM_CODEC), Mailbox::displayName,
                Mailbox::new
        );

        public Mailbox(String id, Component displayName) {
            this(id, Optional.ofNullable(displayName));
        }

        public Mailbox(String id) {
            this(id, Optional.empty());
        }

        @Override
        public Type type() {
            return Type.MAILBOX;
        }

        @Override
        public Component getDisplayName() {
            return displayName.orElse(Component.literal(id));
        }
    }

    enum Type implements StringRepresentable {
        PLAYER("player", Player.CODEC, Player.STREAM_CODEC.cast()),
        MAILBOX("mailbox", Mailbox.CODEC, Mailbox.STREAM_CODEC);

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
