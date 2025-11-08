package io.github.mortuusars.envelope.util;

import net.minecraft.SharedConstants;

public abstract class Ticks {
    public static int fromSeconds(float seconds) {
        return (int)(seconds * SharedConstants.TICKS_PER_SECOND);
    }
}
