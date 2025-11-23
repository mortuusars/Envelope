package io.github.mortuusars.envelope;

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
    public static final ResourceLocation ITEM_PROPERTY_LETTER_UNFOLDED = Envelope.resource("letter_unfolded");
    public static final ResourceLocation ITEM_PROPERTY_LETTER_HAS_CONTENT = Envelope.resource("letter_has_content");

    public static void init() {
        BuggerDebugScreen.addPage(new EnvelopeBuggerPage());
        BuggerEntityOverhead.addData(new PigeonEntityDataDisplay());
        registerItemModelProperties();
    }

    private static void registerItemModelProperties() {
        ItemProperties.register(Envelope.Items.LETTER_AND_QUILL.get(), ITEM_PROPERTY_LETTER_HAS_CONTENT, EnvelopeClient::hasContent);

        ItemProperties.register(Envelope.Items.LETTER.get(), ITEM_PROPERTY_LETTER_UNFOLDED, EnvelopeClient::isUnfolded);
        ItemProperties.register(Envelope.Items.LETTER.get(), ITEM_PROPERTY_LETTER_HAS_CONTENT, EnvelopeClient::hasContent);

        ItemProperties.register(Envelope.Items.OPENED_SEALED_LETTER.get(), ITEM_PROPERTY_LETTER_UNFOLDED, EnvelopeClient::isUnfolded);
        ItemProperties.register(Envelope.Items.OPENED_SEALED_LETTER.get(), ITEM_PROPERTY_LETTER_HAS_CONTENT, EnvelopeClient::hasContent);

        ItemProperties.register(Envelope.Items.TATTERED_LETTER.get(), ITEM_PROPERTY_LETTER_UNFOLDED, EnvelopeClient::isUnfolded);
        ItemProperties.register(Envelope.Items.TATTERED_LETTER.get(), ITEM_PROPERTY_LETTER_HAS_CONTENT, EnvelopeClient::hasContent);
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
