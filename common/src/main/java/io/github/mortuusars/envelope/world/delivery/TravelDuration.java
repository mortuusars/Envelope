package io.github.mortuusars.envelope.world.delivery;

import com.google.common.base.Preconditions;
import com.mojang.serialization.Codec;
import io.github.mortuusars.envelope.Config;
import io.github.mortuusars.envelope.util.Ticks;
import io.github.mortuusars.envelope.world.mail.address.Address;
import io.netty.buffer.ByteBuf;
import net.minecraft.SharedConstants;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerLevel;

public record TravelDuration(int ticks) {
    public TravelDuration {
        Preconditions.checkArgument(ticks > 0, "Duration must be larger than 0.");
    }

    public static final Codec<TravelDuration> CODEC = Codec.intRange(1, Integer.MAX_VALUE)
          .xmap(TravelDuration::new, TravelDuration::ticks);

    public static final StreamCodec<ByteBuf, TravelDuration> STREAM_CODEC =
          ByteBufCodecs.INT.map(TravelDuration::new, TravelDuration::ticks);

    public int seconds() {
        return ticks / SharedConstants.TICKS_PER_SECOND;
    }

    // --

    public static Supplier basedOnDistance(int distanceInBlocks) {
        return (level, sender, recipient) -> {
            int distance = distanceInBlocks;
            distance = Math.min(distance, Config.Server.DELIVERY_TRAVEL_DURATION_DISTANCE_CAP.get());
            double seconds = distance / Config.Server.DELIVERY_COURIER_TRAVEL_SPEED.get();
            return new TravelDuration(Math.max(1, (int) Ticks.fromSeconds(seconds)));
        };
    }

    public static Supplier basedOnSenderToRecipientDistance() {
        return (level, sender, recipient) -> {
            int distance = level.getEnvelopeContext().addresses().getDistanceTo(sender, recipient)
                  .orElse(Config.Server.DELIVERY_DEFAULT_DISTANCE.get());
            return basedOnDistance(distance).get(level, sender, recipient);
        };
    }

    public static Supplier ticks(int ticks) {
        TravelDuration travelDuration = new TravelDuration(ticks);
        return (level, sender, recipient) -> travelDuration;
    }

    public interface Supplier {
        TravelDuration get(ServerLevel level, Address sender, Address recipient);
    }
}
