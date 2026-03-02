package io.github.mortuusars.envelope.neoforge.integration.kubejs.event;

import dev.latvian.mods.kubejs.level.KubeLevelEvent;
import io.github.mortuusars.envelope.neoforge.api.event.HandleMailDropOffEvent;
import io.github.mortuusars.envelope.world.mail.dropoff.MailDropOffContext;
import net.minecraft.world.level.Level;

public class HandleMailDropOffEventJS extends HandleMailDropOffEvent implements KubeLevelEvent {
    public HandleMailDropOffEventJS(MailDropOffContext context) {
        super(context);
    }

    @Override
    public Level getLevel() {
        return getContext().getService().getLevel();
    }
}
