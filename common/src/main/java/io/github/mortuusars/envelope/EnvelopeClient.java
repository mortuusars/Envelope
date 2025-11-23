package io.github.mortuusars.envelope;

import io.github.mortuusars.envelope.util.bugger.BuggerDebugScreen;
import io.github.mortuusars.envelope.util.bugger.BuggerEntityOverhead;
import io.github.mortuusars.envelope.util.bugger_data.EnvelopeBuggerPage;
import io.github.mortuusars.envelope.util.bugger_data.PigeonEntityDataDisplay;
import io.github.mortuusars.envelope.world.item.component.LetterContent;
import net.minecraft.client.renderer.item.ClampedItemPropertyFunction;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public class EnvelopeClient {
    public static final ResourceLocation MODEL_PROPERTY_LETTER_STATE = Envelope.resource("letter_state");

    public static void init() {
        BuggerDebugScreen.addPage(new EnvelopeBuggerPage());
        BuggerEntityOverhead.addData(new PigeonEntityDataDisplay());
        registerItemModelProperties();
    }

    private static void registerItemModelProperties() {
        ItemProperties.register(Envelope.Items.LETTER.get(), MODEL_PROPERTY_LETTER_STATE, unfoldedPropertyFunction());
        ItemProperties.register(Envelope.Items.OPENED_SEALED_LETTER.get(), MODEL_PROPERTY_LETTER_STATE, unfoldedPropertyFunction());
        ItemProperties.register(Envelope.Items.TATTERED_LETTER.get(), MODEL_PROPERTY_LETTER_STATE, unfoldedPropertyFunction());
    }

    private static @NotNull ClampedItemPropertyFunction unfoldedPropertyFunction() {
        return (stack, level, entity, seed) -> {
            LetterContent content = stack.getOrDefault(Envelope.DataComponents.LETTER_CONTENT, LetterContent.EMPTY);
            if (content.unfolded()) {
                return content.text().getString().isEmpty() ? 0.2f : 0.1f;
            }
            return 0;
        };
    }
}
