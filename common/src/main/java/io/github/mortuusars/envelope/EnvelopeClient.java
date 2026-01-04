package io.github.mortuusars.envelope;

import io.github.mortuusars.envelope.client.gui.tooltip.*;
import io.github.mortuusars.envelope.client.renderer.SealRenderer;
import io.github.mortuusars.envelope.util.bugger.BuggerDebugScreen;
import io.github.mortuusars.envelope.util.bugger.BuggerEntityOverhead;
import io.github.mortuusars.envelope.util.bugger_data.EnvelopeBuggerPage;
import io.github.mortuusars.envelope.util.bugger_data.PigeonEntityDataDisplay;
import io.github.mortuusars.envelope.world.item.component.*;
import io.github.mortuusars.envelope.world.item.component.seal.Seal;
import io.github.mortuusars.envelope.world.item.tooltip.CompositeTooltip;
import io.github.mortuusars.envelope.world.item.tooltip.MailAddressTagTooltip;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

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
            ItemProperties.register(Envelope.Items.LETTER_AND_QUILL.get(), LETTER_CONTENT, ItemModelOverrides::hasLetterContent);

            ItemProperties.register(Envelope.Items.LETTER.get(), LETTER_TATTERED, ItemModelOverrides::isLetterTattered);
            ItemProperties.register(Envelope.Items.LETTER.get(), LETTER_UNFOLDED, ItemModelOverrides::isLetterUnfolded);
            ItemProperties.register(Envelope.Items.LETTER.get(), LETTER_CONTENT, ItemModelOverrides::hasLetterContent);

            ItemProperties.register(Envelope.Items.SEALED_LETTER.get(), LETTER_TATTERED, ItemModelOverrides::isLetterTattered);
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
    }

    public static class TooltipComponents {
        public static ClientTooltipComponent create(TooltipComponent component) {
            return switch (component) {
                case MailAddressTagTooltip mailAddress -> new MailAddressTagTooltipComponent(mailAddress.address());
                case PackageContents packageContents -> new PackageTooltipComponent(packageContents);
                case Payback payback -> new PaybackTooltipComponent(payback);
                case PaybackTagContents paybackTagContents -> new PaybackTagContentsTooltipComponent(paybackTagContents);
                case Seal seal -> new SealTooltipComponent(seal);
                case io.github.mortuusars.envelope.world.inventory.tooltip.SealDieTooltipComponent die -> new SealDieTooltipComponent(die.impression());
                case CompositeTooltip composite -> new CompositeTooltipComponent(
                      composite.components().stream().map(ClientTooltipComponent::create).toList()
                );
                default -> null;
            };
        }

        public static Optional<TooltipComponent> modifyTooltipImage(ItemStack stack, Optional<TooltipComponent> original) {
            if (stack.is(Envelope.Tags.Items.MAILABLE)) {
                return CompositeTooltip.of(
                      original,
                      Optional.ofNullable(stack.get(Envelope.DataComponents.PAYBACK_TAG)),
                      Optional.ofNullable(stack.get(Envelope.DataComponents.ADDRESS_TAG)).map(MailAddressTagTooltip::new)
                );
            }

            return original;
        }
    }
}
