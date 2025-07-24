package io.github.mortuusars.envelope.util.result;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public class Failure {
    protected final String message;

    public Failure(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public static class Translatable extends Failure {
        protected final String translationKey;

        public Translatable(String message, String translationKey) {
            super(message);
            this.translationKey = translationKey;
        }

        public String getTranslationKey() {
            return translationKey;
        }

        public MutableComponent getTranslatedMessage() {
            return Component.translatable(getTranslationKey());
        }
    }
}
