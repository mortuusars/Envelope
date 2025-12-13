package io.github.mortuusars.envelope.util.result;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Error {
    private static final Logger log = LoggerFactory.getLogger(Error.class);
    protected final String message;
    protected final @Nullable String translationKey;

    public Error(String message, @Nullable String translationKey) {
        this.message = message;
        this.translationKey = translationKey;
    }

    public Error(String message) {
        this(message, null);
    }

    public String getMessage() {
        return message;
    }

    public MutableComponent getTranslation() {
        return translationKey != null ? Component.translatable(translationKey) : Component.literal(message);
    }

    public void log(Logger logger) {
        logger.error(getMessage());
    }
}
