package io.github.mortuusars.envelope.fabric.api.event;

import io.github.mortuusars.envelope.world.mail.dropoff.MailDropOffHandler;
import io.github.mortuusars.envelope.world.mail.dropoff.MailDropOffResult;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

public interface EnvelopeFabricEvents {
    Event<MailDropOffHandler> HANDLE_MAIL_DROP_OFF = EventFactory.createArrayBacked(MailDropOffHandler.class,
          listeners -> context -> {
              for (MailDropOffHandler listener : listeners) {
                  MailDropOffResult result = listener.handle(context);
                  if (result.isHandled()) {
                      return result;
                  }
              }

              return MailDropOffResult.PASS;
          });
}
