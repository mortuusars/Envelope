package io.github.mortuusars.envelope.util.bugger;

import io.github.mortuusars.envelope.world.GameTime;
import io.github.mortuusars.envelope.world.mail.delivery.Delivery;
import io.github.mortuusars.envelope.world.entity.Pigeon;
import io.github.mortuusars.envelope.world.entity.ai.MailboxHandler;
import io.github.mortuusars.envelope.world.entity.ai.PigeonholeHandler;
import io.github.mortuusars.mortaar.bugger.screen.BuggerEntityOverhead;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;

import java.util.ArrayList;

public class PigeonEntityDataDisplay implements BuggerEntityOverhead.EntityOverheadDisplay {
    @Override
    public void addLines(Entity entity, ArrayList<Component> lines) {
        if (!(entity instanceof Pigeon pigeon)) return;

        if (pigeon.getCurrentDelivery().isPresent()) {
            Delivery delivery = pigeon.getCurrentDelivery().get();
            lines.add(Component.empty()
                  .append(delivery.getSender().format().asSender().toComponent())
                  .append(" → ")
                  .append(delivery.getRecipient().format().asRecipient().toComponent()));
            lines.add(line(delivery.getPhase().toPrettyString()).withStyle(ChatFormatting.GRAY));
            return;
        }

        if (pigeon.isTired()) {
            lines.add(line("Tired [" + GameTime.format(pigeon.getTiredTicks(), true).getString() + "]"));
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

        if (pigeonholeHandler.getTargetPos() != null) {
            lines.add(line("Target: [" + pigeonholeHandler.getTargetPos().toShortString() + "]"));
        }
        if (pigeonholeHandler.getHomePos() != null) {
            lines.add(line("Home: [" + pigeonholeHandler.getHomePos().toShortString() + "]"));
        }
    }
}
