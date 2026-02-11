package io.github.mortuusars.envelope.world.mail.address.type;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.mortuusars.envelope.world.mail.address.Address;
import io.github.mortuusars.envelope.world.mail.address.BlockAddressValidation;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.Objects;

public final class BlockAddress implements Address {
    public static final Codec<String> ID_CODEC = Codec.STRING
          .xmap(String::trim, String::trim)
          .validate(BlockAddressValidation::validateId);

    public static final Codec<BlockAddress> STRING_CODEC = ID_CODEC.xmap(BlockAddress::new, BlockAddress::getString);

    public static final MapCodec<BlockAddress> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
          ID_CODEC.fieldOf("id").forGetter(BlockAddress::getString)
    ).apply(i, BlockAddress::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, BlockAddress> STREAM_CODEC =
          ByteBufCodecs.STRING_UTF8.map(BlockAddress::new, BlockAddress::getString).cast();

    public static final int MAX_LENGTH = 40;

    private final String id;

    public BlockAddress(String id) {
        this.id = BlockAddressValidation.throwIfInvalid(id);
    }

    @Override
    public Type getType() {
        return Type.BLOCK;
    }

    public String getString() {
        return id;
    }

    public MutableComponent getComponent() {
        return Component.literal(id);
    }

    // --

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        BlockAddress that = (BlockAddress) o;
        return matches(that.getString());
    }

    @Override
    public int hashCode() {
        return ("b" + id.toLowerCase(Locale.ROOT)).hashCode();
    }

    @Override
    public @NotNull String toString() {
        return "Block[" + id + "]";
    }
}
