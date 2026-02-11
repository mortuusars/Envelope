package io.github.mortuusars.envelope.world.mail.address.type;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.mortuusars.envelope.world.mail.address.Address;
import io.github.mortuusars.envelope.world.mail.address.AddressValidation;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.Objects;

public final class BlockAddress implements Address.Presentable {
    public static final MapCodec<BlockAddress> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
          ID_CODEC.fieldOf("id").forGetter(BlockAddress::getId)
    ).apply(instance, BlockAddress::new));

    public static final Codec<BlockAddress> STRING_CODEC = Address.ID_CODEC.xmap(BlockAddress::new, BlockAddress::getId);

    public static final StreamCodec<RegistryFriendlyByteBuf, BlockAddress> STREAM_CODEC =
          ByteBufCodecs.STRING_UTF8.map(BlockAddress::new, BlockAddress::getId).cast();

    private final String id;

    public BlockAddress(String id) {
        AddressValidation.validateId(id).getOrThrow();
        this.id = id;
    }

    @Override
    public Type getType() {
        return Type.BLOCK;
    }

    @Override
    public String getId() {
        return id;
    }

    public String getDisplayString() {
        return id;
    }

    public MutableComponent getDisplayComponent() {
        return Component.literal(id);
    }

    // --

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        BlockAddress that = (BlockAddress) o;
        return Objects.equals(id, that.id);
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
