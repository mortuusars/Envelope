package io.github.mortuusars.envelope.world.mail.delivery;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record CourierProperties(double travelSpeed) {
    public static final Codec<CourierProperties> CODEC = RecordCodecBuilder.create(i -> i.group(
          Codec.DOUBLE.optionalFieldOf("travel_speed", 25.0).forGetter(CourierProperties::travelSpeed)
    ).apply(i, CourierProperties::new));

    public static final CourierProperties DEFAULT = new CourierProperties(25.0);
}
