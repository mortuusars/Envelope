package io.github.mortuusars.envelope.world.pigeonhole;

import net.minecraft.server.level.ServerLevel;

public class PigeonholeManager {
    private final ServerLevel level;

    public PigeonholeManager(ServerLevel level) {
        this.level = level;
    }



    // --

    private PigeonholeSavedData data() {
        return PigeonholeSavedData.get(level);
    }
}
