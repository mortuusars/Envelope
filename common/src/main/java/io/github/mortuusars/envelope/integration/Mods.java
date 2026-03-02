package io.github.mortuusars.envelope.integration;

import io.github.mortuusars.envelope.Platform;

public class Mods {
    public record Mod(String id) {
        public boolean isLoaded() {
            return Platform.isModLoaded(id);
        }

        public boolean isLoading() {
            return Platform.isModLoading(id);
        }
    }
}