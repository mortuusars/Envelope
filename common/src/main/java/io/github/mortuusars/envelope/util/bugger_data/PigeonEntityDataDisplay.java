package io.github.mortuusars.envelope.util.bugger_data;

import io.github.mortuusars.envelope.util.bugger.BuggerEntityOverhead;
import io.github.mortuusars.envelope.world.delivery.Delivery;
import io.github.mortuusars.envelope.world.entity.Pigeon;
import io.github.mortuusars.envelope.world.entity.ai.PigeonholeHandler;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;

import java.util.ArrayList;

public class PigeonEntityDataDisplay implements BuggerEntityOverhead.EntityDataDisplay {
    @Override
    public void addLines(Entity entity, ArrayList<Component> lines) {
        if (!(entity instanceof Pigeon pigeon)) return;

        if (pigeon.getDelivery().isPresent()) {
            Delivery delivery = pigeon.getDelivery().get();
            lines.add(Component.empty()
                  .append(delivery.getSender().getName())
                  .append(" → ")
                  .append(delivery.getRecipient().getName()));
            lines.add(line(delivery.getCurrentPhase().toPrettyString()));
        } else {
            PigeonholeHandler handler = pigeon.getPigeonholeHandler();
            if (handler.wantsToEnterPigeonhole(entity.level())) {
                lines.add(line(handler.getCurrentPos() != null ? "Going to Pigeonhole" : "Looking for Pigeonhole"));
            } else {
                lines.add(line("Would want to enter after: " + handler.getWantCooldown() / 20));
                if (handler.getEnterCooldown() > 0) {
                    lines.add(line("Could enter after: " + handler.getEnterCooldown() / 20));
                }
            }
        }
    }
}
