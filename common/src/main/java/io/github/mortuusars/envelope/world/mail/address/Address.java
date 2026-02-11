package io.github.mortuusars.envelope.world.mail.address;

import com.mojang.serialization.*;
import io.github.mortuusars.envelope.world.mail.MailService;
import io.github.mortuusars.envelope.world.mail.address.type.*;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;

import java.util.function.IntFunction;

public interface Address {
    Codec<Address> CODEC = Type.CODEC.dispatch(Address::getType, Type::getCodec);
    StreamCodec<RegistryFriendlyByteBuf, Address> STREAM_CODEC = Type.STREAM_CODEC.dispatch(Address::getType, Type::getStreamCodec);

    UnknownAddress UNKNOWN = UnknownAddress.INSTANCE;

    Type getType();

    String getString();

    MutableComponent getComponent();

    default boolean matches(String name) {
        return getString().equalsIgnoreCase(name);
    }

    default AddressFormatter format() {
        return AddressFormatter.of(this);
    }

    default boolean isMailService() {
        return this instanceof EntityAddress entityAddress && entityAddress.getEntityHolder().is(EntityAddresses.MAIL_SERVICE);
    }

    default boolean isUnknown() {
        return this instanceof UnknownAddress;
    }

    // --

    /**
     * Returns an endpoint from a given address. Registered block address, default address of a player, etc. Or unknown if missing.
     * @return Endpoint address or Unknown address.
     */
    default Address resolve(MailService service) {
        return switch (this) {
            case BlockAddress blockAddress -> service.getKnownAddresses().isKnown(blockAddress)
                  ? blockAddress
                  : UNKNOWN;
            case PlayerAddress playerAddress -> service.getPlayerDefaultAddress(playerAddress)
                  .map(Address.class::cast)
                  .orElse(UNKNOWN);
            default -> this;
        };
    }

    // --

    enum Type implements StringRepresentable {
        BLOCK("block", BlockAddress.CODEC, BlockAddress.STREAM_CODEC),
        PLAYER("player", PlayerAddress.CODEC, PlayerAddress.STREAM_CODEC),
        ENTITY("entity", EntityAddress.CODEC, EntityAddress.STREAM_CODEC),
        CUSTOM("custom", CustomAddress.CODEC, CustomAddress.STREAM_CODEC),
        UNKNOWN("unknown", MapCodec.unit(UnknownAddress.INSTANCE), StreamCodec.unit(UnknownAddress.INSTANCE));

        public static final Codec<Type> CODEC = StringRepresentable.fromEnum(Type::values);
        public static final IntFunction<Type> BY_ID = ByIdMap.continuous(Type::ordinal, values(), ByIdMap.OutOfBoundsStrategy.ZERO);
        public static final StreamCodec<RegistryFriendlyByteBuf, Type> STREAM_CODEC = ByteBufCodecs.idMapper(BY_ID, Type::ordinal).cast();

        private final String name;
        private final MapCodec<? extends Address> codec;
        private final StreamCodec<RegistryFriendlyByteBuf, ? extends Address> streamCodec;

        Type(String name, MapCodec<? extends Address> codec, StreamCodec<RegistryFriendlyByteBuf, ? extends Address> streamCodec) {
            this.name = name;
            this.codec = codec;
            this.streamCodec = streamCodec;
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
            return Component.translatable("address_type.envelope." + getSerializedName());
        }
    }
}
