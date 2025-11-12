package io.github.mortuusars.envelope.util;

import net.minecraft.SharedConstants;

public abstract class Ticks {
    public static final int PER_SECOND = SharedConstants.TICKS_PER_SECOND;

    public static int fromSeconds(double seconds) {
        return (int)(seconds * SharedConstants.TICKS_PER_SECOND);
    }
}
