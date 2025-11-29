package io.github.mortuusars.envelope.util;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

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
}
