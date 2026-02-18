package io.github.mortuusars.envelope.world.item.component;

import com.mojang.serialization.Codec;
import io.github.mortuusars.envelope.Config;
import io.github.mortuusars.envelope.util.Ticks;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;

import java.util.function.IntFunction;

public enum PaybackDuration implements StringRepresentable {
    SHORT("short"),
    MEDIUM("medium"),
    LONG("long");

    public static final IntFunction<PaybackDuration> BY_ID = ByIdMap.continuous(PaybackDuration::ordinal, PaybackDuration.values(), ByIdMap.OutOfBoundsStrategy.ZERO);

    public static final Codec<PaybackDuration> CODEC = StringRepresentable.fromEnum(PaybackDuration::values);
    public static final StreamCodec<ByteBuf, PaybackDuration> STREAM_CODEC = ByteBufCodecs.idMapper(BY_ID, PaybackDuration::ordinal);

    private final String name;

    PaybackDuration(String name) {
        this.name = name;
    }

    @Override
    public @NotNull String getSerializedName() {
        return name;
    }

    public int getMinutes() {
        return switch (this) {
            case SHORT -> Config.Server.PAYBACK_REQUEST_DURATION_SHORT.get();
            case MEDIUM -> Config.Server.PAYBACK_REQUEST_DURATION_MEDIUM.get();
            case LONG -> Config.Server.PAYBACK_REQUEST_DURATION_LONG.get();
        };
    }

    public long getTicks() {
        return Ticks.fromMinutes(getMinutes());
    }
}
