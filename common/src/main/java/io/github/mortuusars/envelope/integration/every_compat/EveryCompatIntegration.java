package io.github.mortuusars.envelope.integration.every_compat;

import net.mehvahdjukaar.every_compat.api.EveryCompatAPI;

public class EveryCompatIntegration {
    public static void init() {
        EveryCompatAPI.registerModule(new EnvelopeModule());
    }
}
