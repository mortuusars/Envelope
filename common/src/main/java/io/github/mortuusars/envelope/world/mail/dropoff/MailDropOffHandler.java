package io.github.mortuusars.envelope.world.mail.dropoff;

import java.util.Arrays;
import java.util.List;

@FunctionalInterface
public interface MailDropOffHandler {
    /**
     * Decides what to do with incoming or returning mail.
     * @return Mail result that will continue its journey along the delivery path.
     */
    MailDropOffResult handle(MailDropOffContext context);

    static MailDropOffHandler chain(MailDropOffHandler... handlers) {
        return new Chain(Arrays.stream(handlers).toList());
    }

    record Chain(List<MailDropOffHandler> handlers) implements MailDropOffHandler {
        @Override
        public MailDropOffResult handle(MailDropOffContext context) {
            for (MailDropOffHandler handler : handlers) {
                MailDropOffResult result = handler.handle(context);
                if (result.isHandled()) {
                    return result;
                }
            }
            return MailDropOffResult.PASS;
        }
    }
}
