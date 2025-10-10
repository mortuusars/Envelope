package io.github.mortuusars.envelope.core.address.validation;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;

public interface Validator<T> {
    interface Issue {
        String id();
        default MutableComponent translate() {
            return Component.translatable("gui.envelope.validation.issue." + id());
        }
    }

    ArrayList<Issue> validate(T value);

    class CachedValidator<T> implements Validator<T> {
        protected final Validator<T> validator;

        protected @Nullable T currentValue = null;
        protected ArrayList<Issue> currentIssues = new ArrayList<>();

        public CachedValidator(Validator<T> validator) {
            this.validator = validator;
        }

        public Validator<T> getValidator() {
            return validator;
        }

        public @Nullable T getCurrentValue() {
            return currentValue;
        }

        public ArrayList<Issue> getCurrentIssues() {
            return currentIssues;
        }

        public void setValue(T newValue) {
            if (currentValue != null && currentValue.equals(newValue)) {
                return;
            }

            currentValue = newValue;
            currentIssues = validate(currentValue);
        }

        @Override
        public ArrayList<Issue> validate(T value) {
            return validator.validate(value);
        }
    }
}
