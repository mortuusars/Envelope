package io.github.mortuusars.envelope.integration.wood_good;

import net.mehvahdjukaar.every_compat.api.EveryCompatAPI;

public class EveryCompatIntegration {
    public static void init() {
        EveryCompatAPI.registerModule(new EnvelopeModule());
    }
}
