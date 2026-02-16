package io.github.mortuusars.envelope.util;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

import java.util.function.Function;

public interface EnvelopeCodecs {
    Codec<Integer> HEX_COLOR = Codec.STRING.comapFlatMap(
          hex -> {
              try {
                  int color = (int) Long.parseLong(hex.replace("#", ""), 16);
                  return DataResult.success(color);
              } catch (NumberFormatException e) {
                  return DataResult.error(() -> "Invalid color value: " + hex);
              }
          },
          color -> "#" + String.format("%08X", color));

    Codec<Float> NON_NEGATIVE_FLOAT = Codec.FLOAT
          .validate(value -> value.compareTo(0.0f) >= 0.0f && value.compareTo(1.0f) <= 0
                ? DataResult.success(value)
                : DataResult.error(() -> "Value must be within 0.0 to 1.0 range."));
}
