package io.github.mortuusars.envelope.world.mail.address;

import com.mojang.serialization.*;
import io.github.mortuusars.envelope.world.mail.MailService;
import io.github.mortuusars.envelope.world.mail.address.type.*;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.function.IntFunction;

public interface Address {
    int MAX_LENGTH = 40;

    Codec<String> ID_CODEC = Codec.STRING.xmap(String::trim, String::trim).validate(AddressValidation::validateId);

    Codec<Address> CODEC = Type.CODEC.dispatch(Address::getType, Type::getCodec);
    StreamCodec<RegistryFriendlyByteBuf, Address> STREAM_CODEC = Type.STREAM_CODEC.dispatch(Address::getType, Type::getStreamCodec);

    UnknownAddress UNKNOWN = UnknownAddress.INSTANCE;

    Type getType();

    String getId();

    default boolean isMailService() {
        return this instanceof EntityAddress entityAddress && entityAddress.getEntity().is(EntityAddresses.MAIL_SERVICE);
    }

    default boolean isUnknown() {
        return this instanceof UnknownAddress;
    }

    // --

    default Address resolve(MailService service) {
        if (this instanceof PlayerAddress player) {
            return service.getPlayerDefaultAddress(player)
                  .map(Address.class::cast)
                  .orElse(Address.UNKNOWN);
        }
        return this;
    }

    // --

    default Presentable represent(RegistryAccess access) {
        if (this instanceof Presentable presentable) return presentable;
        if (this instanceof Representable representable) return representable.represent(access);
        throw new IllegalStateException("Address " + this + " cannot be realized.");
    }

    default Presentable represent(Level level) {
        return represent(level.registryAccess());
    }

    default Presentable represent(MailService service) {
        return represent(service.getLevel());
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

    // --

    interface Presentable extends Address {
        MutableComponent getDisplayComponent();
        String getDisplayString();

        default boolean matches(String name) {
            return getDisplayString().equalsIgnoreCase(name);
        }

        default AddressFormatter format() {
            return AddressFormatter.of(this);
        }
    }

    interface Representable extends Address {
        Presentable represent(RegistryAccess access);
    }
}
