package io.github.mortuusars.envelope.integration;

import io.github.mortuusars.mortaar.Platform;

public class Mods {
    public static final Mod SABLE = new Mod("sable");
    public static final Mod EVERY_COMPAT = new Mod("everycomp");

    public record Mod(String id) {
        public boolean isLoaded() {
            return Platform.isModLoaded(id);
        }

        public boolean isLoading() {
            return Platform.isModLoading(id);
        }
    }
}