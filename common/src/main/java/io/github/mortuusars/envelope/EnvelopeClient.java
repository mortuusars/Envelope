package io.github.mortuusars.envelope;

import io.github.mortuusars.envelope.client.renderer.SealRenderer;
import io.github.mortuusars.envelope.util.bugger.BuggerDebugScreen;
import io.github.mortuusars.envelope.util.bugger.BuggerEntityOverhead;
import io.github.mortuusars.envelope.util.bugger_data.EnvelopeBuggerPage;
import io.github.mortuusars.envelope.util.bugger_data.PigeonEntityDataDisplay;
import io.github.mortuusars.envelope.world.item.component.LetterAndQuillContent;
import io.github.mortuusars.envelope.world.item.component.LetterContent;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public class EnvelopeClient {
    private static final SealRenderer sealRenderer = new SealRenderer();

    public static void init() {
        BuggerDebugScreen.addPage(new EnvelopeBuggerPage());
        BuggerEntityOverhead.addData(new PigeonEntityDataDisplay());
        ItemModelOverrides.register();
    }

    public static SealRenderer getSealRenderer() {
        return sealRenderer;
    }

    // --

    public static class ItemModelOverrides {
        public static final ResourceLocation LETTER_TATTERED = Envelope.resource("letter_tattered");
        public static final ResourceLocation LETTER_UNFOLDED = Envelope.resource("letter_unfolded");
        public static final ResourceLocation LETTER_CONTENT = Envelope.resource("letter_content");

        public static void register() {
            ItemProperties.register(Envelope.Items.LETTER_AND_QUILL.get(), LETTER_CONTENT, EnvelopeClient.ItemModelOverrides::hasContent);

            ItemProperties.register(Envelope.Items.LETTER.get(), LETTER_TATTERED, EnvelopeClient.ItemModelOverrides::isTattered);
            ItemProperties.register(Envelope.Items.LETTER.get(), LETTER_UNFOLDED, EnvelopeClient.ItemModelOverrides::isUnfolded);
            ItemProperties.register(Envelope.Items.LETTER.get(), LETTER_CONTENT, EnvelopeClient.ItemModelOverrides::hasContent);

            ItemProperties.register(Envelope.Items.SEALED_LETTER.get(), LETTER_TATTERED, EnvelopeClient.ItemModelOverrides::isTattered);
        }

        public static float isTattered(ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity entity, int seed) {
            return stack.has(Envelope.DataComponents.LETTER_TATTERED) ? 1 : 0;
        }

        public static float isUnfolded(ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity entity, int seed) {
            LetterContent content = stack.getOrDefault(Envelope.DataComponents.LETTER_CONTENT, LetterContent.EMPTY);
            return content.unfolded() ? 1 : 0;
        }

        public static float hasContent(ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity entity, int seed) {
            if (!stack.getOrDefault(Envelope.DataComponents.LETTER_AND_QUILL_CONTENT, LetterAndQuillContent.EMPTY).isEmpty()) {
                return 1;
            }
            if (!stack.getOrDefault(Envelope.DataComponents.LETTER_CONTENT, LetterContent.EMPTY).isEmpty()) {
                return 1;
            }
            return 0;
        }
    }
}
