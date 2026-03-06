package io.github.mortuusars.envelope.world.mail.delivery;

import com.google.common.base.Preconditions;
import com.mojang.serialization.Codec;
import io.github.mortuusars.envelope.Config;
import io.github.mortuusars.envelope.util.Ticks;
import io.netty.buffer.ByteBuf;
import net.minecraft.SharedConstants;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;

public record TravelDuration(int ticks) {
    public static final Codec<TravelDuration> CODEC =
          ExtraCodecs.POSITIVE_INT.xmap(TravelDuration::new, TravelDuration::ticks);

    public static final StreamCodec<ByteBuf, TravelDuration> STREAM_CODEC =
          ByteBufCodecs.INT.map(TravelDuration::new, TravelDuration::ticks);

    public static final TravelDuration DEFAULT = new TravelDuration(600); // 30 sec

    public TravelDuration {
        Preconditions.checkArgument(ticks > 0, "Duration must be larger than 0.");
    }

    public int seconds() {
        return ticks / SharedConstants.TICKS_PER_SECOND;
    }

    // --

    public static TravelDuration basedOnDistance(int distanceInBlocks) {
        distanceInBlocks = Math.min(distanceInBlocks, Config.Server.DELIVERY_TRAVEL_DURATION_DISTANCE_CAP.get());
        double seconds = distanceInBlocks / Config.Server.DELIVERY_COURIER_TRAVEL_SPEED.get();
        return new TravelDuration(Math.max(1, (int) Ticks.fromSeconds(seconds)));
    }
}
