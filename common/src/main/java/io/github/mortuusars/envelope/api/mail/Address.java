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
    Address MAIL_SERVICE = new Npc("<Mail Service>", Component.translatable("address.envelope.mail_service"));

    Codec<Address> CODEC = Type.CODEC.dispatch(Address::type, Type::getCodec);
    StreamCodec<RegistryFriendlyByteBuf, Address> STREAM_CODEC = Type.STREAM_CODEC.dispatch(Address::type, Type::getStreamCodec);

    Type type();
    String id();
    Component getPresentableName();

    record Player(String name, UUID uuid) implements Address {
        public static final MapCodec<Player> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Codec.STRING.fieldOf("name").forGetter(Player::name),
                UUIDUtil.CODEC.fieldOf("uuid").forGetter(Player::uuid)
        ).apply(instance, Player::new));

        public static final StreamCodec<ByteBuf, Player> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, Player::name,
                UUIDUtil.STREAM_CODEC, Player::uuid,
                Player::new
        );

        public Player(net.minecraft.world.entity.player.Player player) {
            this(player.getScoreboardName(), player.getUUID());
        }

        @Override
        public Type type() {
            return Type.PLAYER;
        }

        @Override
        public String id() {
            return name;
        }

        @Override
        public Component getPresentableName() {
            return Component.literal(name);
        }
    }

    record Npc(String name, Optional<Component> displayName) implements Address {
        public static final MapCodec<Npc> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Codec.STRING.fieldOf("name").forGetter(Npc::name),
                ComponentSerialization.CODEC.optionalFieldOf("display_name").forGetter(Npc::displayName)
        ).apply(instance, Npc::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, Npc> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, Npc::name,
                ByteBufCodecs.optional(ComponentSerialization.STREAM_CODEC), Npc::displayName,
                Npc::new
        );

        public Npc(String name, Component displayName) {
            this(name, Optional.ofNullable(displayName));
        }

        @Override
        public Type type() {
            return Type.NPC;
        }

        @Override
        public String id() {
            return name;
        }

        @Override
        public Component getPresentableName() {
            return displayName.orElse(Component.literal(name));
        }
    }

    record Unknown(String name) implements Address {
        public static final MapCodec<Unknown> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Codec.STRING.fieldOf("name").forGetter(Unknown::name)
        ).apply(instance, Unknown::new));

        public static final StreamCodec<ByteBuf, Unknown> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, Unknown::name,
                Unknown::new
        );

        @Override
        public Type type() {
            return Type.UNKNOWN;
        }

        @Override
        public String id() {
            return name;
        }

        @Override
        public Component getPresentableName() {
            return Component.literal(name);
        }
    }

    enum Type implements StringRepresentable {
        PLAYER("player", Player.CODEC, Player.STREAM_CODEC.cast()),
        NPC("npc", Npc.CODEC, Npc.STREAM_CODEC),
        UNKNOWN("unknown", Unknown.CODEC, Unknown.STREAM_CODEC.cast());

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

        public String getName() {
            return name;
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
