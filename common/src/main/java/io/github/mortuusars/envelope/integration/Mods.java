package io.github.mortuusars.envelope.integration;

import io.github.mortuusars.envelope.PlatformHelper;

public class Mods {
    public record Mod(String id) {
        public boolean isLoaded() {
            return PlatformHelper.isModLoaded(id);
        }

        public boolean isLoading() {
            return PlatformHelper.isModLoading(id);
        }
    }
}