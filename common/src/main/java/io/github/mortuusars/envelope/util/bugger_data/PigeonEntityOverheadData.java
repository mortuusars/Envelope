package io.github.mortuusars.envelope.util.bugger_data;

import io.github.mortuusars.envelope.util.bugger.BuggerEntityOverhead;
import io.github.mortuusars.envelope.world.delivery.Delivery;
import io.github.mortuusars.envelope.world.entity.Pigeon;
import io.github.mortuusars.envelope.world.entity.ai.PigeonholeHandler;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import org.apache.commons.lang3.text.WordUtils;

import java.util.ArrayList;

public class PigeonEntityOverheadData implements BuggerEntityOverhead.EntityOverheadData {
    @Override
    public void addLines(Entity entity, ArrayList<Component> lines) {
        if (!(entity instanceof Pigeon pigeon)) return;

        if (pigeon.getDelivery().isPresent()) {
            Delivery delivery = pigeon.getDelivery().get();
            lines.add(Component.empty()
                  .append(delivery.getSenderAddress().getDisplayName())
                  .append(" → ")
                  .append(delivery.getRecipientAddress().getDisplayName()));
            lines.add(line(WordUtils.capitalize(delivery.getPhase().getType().getSerializedName().replace('_', ' '))));
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
