package io.github.mortuusars.envelope;

import io.github.mortuusars.envelope.client.gui.tooltip.*;
import io.github.mortuusars.envelope.client.renderer.SealRenderer;
import io.github.mortuusars.envelope.util.bugger.EnvelopeBuggerPage;
import io.github.mortuusars.envelope.util.bugger.PigeonEntityDataDisplay;
import io.github.mortuusars.envelope.world.inventory.tooltip.SealDieTooltip;
import io.github.mortuusars.envelope.world.item.component.*;
import io.github.mortuusars.envelope.world.item.component.seal.Seal;
import io.github.mortuusars.envelope.world.item.tooltip.MailAddressTagTooltip;
import io.github.mortuusars.mortaar.bugger.screen.BuggerEntityOverhead;
import io.github.mortuusars.mortaar.bugger.screen.BuggerScreen;
import io.github.mortuusars.mortaar.client.gui.tooltip.Tooltips;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public class EnvelopeClient {
    private static final SealRenderer sealRenderer = new SealRenderer();

    public static void init() {
        ItemModelOverrides.register();

        Tooltips.register(MailAddressTagTooltip.class, mailAddress -> new MailAddressTagTooltipComponent(mailAddress.address()));
        Tooltips.register(PackageContents.class, PackageTooltipComponent::new);
        Tooltips.register(PaybackRequest.class, PaybackRequestTooltipComponent::new);
        Tooltips.register(Seal.class, SealTooltipComponent::new);
        Tooltips.register(SealDieTooltip.class, sealDie -> new SealDieTooltipComponent(sealDie.impression()));

        BuggerScreen.addPage(new EnvelopeBuggerPage());
        BuggerEntityOverhead.addData(new PigeonEntityDataDisplay());
    }

    public static SealRenderer getSealRenderer() {
        return sealRenderer;
    }

    // --

    public static class ItemModelOverrides {
        public static final ResourceLocation LETTER_TATTERED = Envelope.resource("letter_tattered");
        public static final ResourceLocation LETTER_UNFOLDED = Envelope.resource("letter_unfolded");
        public static final ResourceLocation LETTER_CONTENT = Envelope.resource("letter_content");

        public static final ResourceLocation PAYBACK_TAG_DURATION = Envelope.resource("payback_tag_duration");

        public static void register() {
            ItemProperties.register(Envelope.Items.LETTER_AND_QUILL.get(), LETTER_CONTENT, ItemModelOverrides::hasLetterContent);

            ItemProperties.register(Envelope.Items.LETTER.get(), LETTER_TATTERED, ItemModelOverrides::isLetterTattered);
            ItemProperties.register(Envelope.Items.LETTER.get(), LETTER_UNFOLDED, ItemModelOverrides::isLetterUnfolded);
            ItemProperties.register(Envelope.Items.LETTER.get(), LETTER_CONTENT, ItemModelOverrides::hasLetterContent);

            ItemProperties.register(Envelope.Items.SEALED_LETTER.get(), LETTER_TATTERED, ItemModelOverrides::isLetterTattered);

            ItemProperties.register(Envelope.Items.PAYBACK_TAG.get(), PAYBACK_TAG_DURATION, ItemModelOverrides::getPaybackDuration);
        }

        public static float isLetterTattered(ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity entity, int seed) {
            return stack.has(Envelope.DataComponents.LETTER_TATTERED) ? 1 : 0;
        }

        public static float isLetterUnfolded(ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity entity, int seed) {
            LetterContent content = stack.getOrDefault(Envelope.DataComponents.LETTER_CONTENT, LetterContent.EMPTY);
            return content.unfolded() ? 1 : 0;
        }

        public static float hasLetterContent(ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity entity, int seed) {
            if (!stack.getOrDefault(Envelope.DataComponents.LETTER_AND_QUILL_CONTENT, LetterAndQuillContent.EMPTY).isEmpty()) {
                return 1;
            }
            if (!stack.getOrDefault(Envelope.DataComponents.LETTER_CONTENT, LetterContent.EMPTY).isEmpty()) {
                return 1;
            }
            return 0;
        }

        public static float getPaybackDuration(ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity entity, int seed) {
            PaybackDuration duration = stack.get(Envelope.DataComponents.PAYBACK_TAG_CONTENTS) instanceof PaybackRequest request
                  ? request.duration()
                  : PaybackDuration.MEDIUM;
            return duration.ordinal() / 10f; // 0.0, 0.1, 0.2, etc.
        }
    }
}
