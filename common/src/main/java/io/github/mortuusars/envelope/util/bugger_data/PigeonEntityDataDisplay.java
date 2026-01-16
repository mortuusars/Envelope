package io.github.mortuusars.envelope.util.bugger_data;

import io.github.mortuusars.envelope.util.bugger.BuggerEntityOverhead;
import io.github.mortuusars.envelope.world.delivery.Delivery;
import io.github.mortuusars.envelope.world.entity.Pigeon;
import io.github.mortuusars.envelope.world.entity.ai.MailboxHandler;
import io.github.mortuusars.envelope.world.entity.ai.PigeonholeHandler;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;

import java.util.ArrayList;

public class PigeonEntityDataDisplay implements BuggerEntityOverhead.EntityDataDisplay {
    @Override
    public void addLines(Entity entity, ArrayList<Component> lines) {
        if (!(entity instanceof Pigeon pigeon)) return;

        if (pigeon.getCurrentDelivery().isPresent()) {
            Delivery delivery = pigeon.getCurrentDelivery().get();
            lines.add(Component.empty()
                  .append(delivery.getSender().getName())
                  .append(" → ")
                  .append(delivery.getRecipient().getName()));
            lines.add(line(delivery.getPhase().toPrettyString()));
            return;
        }

        if (pigeon.isTired()) {
            lines.add(line("Tired"));
        }

        PigeonholeHandler pigeonholeHandler = pigeon.getPigeonholeHandler();
        MailboxHandler mailboxHandler = pigeon.getMailboxHandler();

        if (mailboxHandler.getTargetPos() != null) {
            lines.add(line("Going to Mailbox for pickup"));
        }

        if (pigeonholeHandler.wantsToEnterPigeonhole(pigeon)) {
            lines.add(line(pigeonholeHandler.getTargetPos() != null ? "Going to Pigeonhole" : "Looking for Pigeonhole"));
        } else {
            lines.add(line("Would want to enter after: " + pigeonholeHandler.getWantCooldown() / 20));
            if (pigeonholeHandler.getEnterCooldown() > 0) {
                lines.add(line("Could enter after: " + pigeonholeHandler.getEnterCooldown() / 20));
            }
        }
    }
}
