package io.github.mortuusars.envelope.client.gui.screen;

import io.github.mortuusars.envelope.Config;
import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.client.util.Minecrft;
import io.github.mortuusars.envelope.util.EnvelopeSymbols;
import io.github.mortuusars.envelope.util.validation.CachedValidator;
import io.github.mortuusars.envelope.world.mail.address.Address;
import io.github.mortuusars.envelope.world.mail.address.BlockAddressValidation;
import io.github.mortuusars.envelope.world.mail.address.AllAddresses;
import io.github.mortuusars.envelope.world.mail.address.type.BlockAddress;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public abstract class AbstractMailboxAddressScreen extends AddressTagScreen {
    protected final LocalPlayer player;
    protected CachedValidator<String> addressValidator;

    public AbstractMailboxAddressScreen(InteractionHand hand, AllAddresses knownAddresses,
                                        Optional<BlockAddress> existingAddress, Component title) {
        super(hand, knownAddresses, title);
        this.player = Minecrft.player();
        this.existingAddress = existingAddress.map(Address.class::cast);
        this.addressValidator = BlockAddressValidation.forMailbox(knownAddresses, player).cached();
    }

    @Override
    protected abstract ItemStack getTargetPreview();
    protected abstract void onConfirm();
    protected abstract boolean stillValid();

    // -- Address

    @Override
    protected String getInitialAddressValue() {
        return existingAddress
              .map(Address::getString)
              .orElseGet(() -> Optional.ofNullable(player.getItemInHand(hand).get(Envelope.DataComponents.ADDRESS))
                    .map(Address::getString)
                    .orElse(""));
    }

    @Override
    protected AllAddresses getAddressesForSuggestions() {
        return AllAddresses.EMPTY; // Don't suggest anything
    }

    // -- Validation

    public CachedValidator<String> getAddressValidator() {
        return addressValidator;
    }

    protected boolean canConfirm() {
        return isCurrentIdSameAsExistingAddress() || getAddressValidator().getErrors().isEmpty();
    }

    // --

    protected void updateConfirmButton() {
        if (confirmButton == null) return; // Not initialized yet

        confirmButton.active = canConfirm();

        MutableComponent confirmTooltip = Component.translatable("gui.envelope.confirm");

        if (!confirmButton.active) {
            getAddressValidator().getErrors()
                  .forEach(issue -> confirmTooltip.append("\n")
                        .append(Component.literal("• ").withStyle(ChatFormatting.RED))
                        .append(issue.getTranslation().withStyle(ChatFormatting.RED)));
            confirmButton.setTooltip(Tooltip.create(confirmTooltip));
            return;
        }

        if (isRenaming() && !isCurrentIdSameAsExistingAddress()) {
            confirmTooltip.append("\n")
                  .append(Component.translatable("gui.envelope.mailbox_address.rename_warning.inbox")
                        .withStyle(Style.EMPTY.withColor(0xFFE76A6A)))
                  .append("\n")
                  .append(Component.translatable("gui.envelope.mailbox_address.rename_warning.traveling")
                        .withStyle(Style.EMPTY.withColor(0xFFE76A6A)));
        }
        confirmButton.setTooltip(Tooltip.create(confirmTooltip));
    }

    // -- Events

    @Override
    protected void addressTextChanged(String text) {
        super.addressTextChanged(text);
        getAddressValidator().testAll(getCurrentAddressId());
    }

    @Override
    protected boolean confirm() {
        if (!canConfirm()) {
            return false;
        }

        if (!isCurrentIdSameAsExistingAddress()) {
            onConfirm();
        }
        close();
        return true;
    }

    // -- Render

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        if (!stillValid()) {
            close();
        }

        updateConfirmButton();

        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderExperienceCost(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderAddressIcon(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        int color = canConfirm() ? 0xFFFFFFFF : 0xAAFFFFFF;
        guiGraphics.drawString(font, EnvelopeSymbols.ADDRESS_BLOCK, leftPos + 17, topPos + 21, color, true);
    }

    protected void renderExperienceCost(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (isCurrentIdSameAsExistingAddress() || player.isCreative()) return;

        int cost = Config.Server.MAILBOX_ADDRESS_EXPERIENCE_LEVELS_COST.get();
        if (cost <= 0) return;

        boolean hasEnough = player.experienceLevel >= cost;
        ResourceLocation sprite = Envelope.resource("address_tag/experience" + (hasEnough ? "" : "_disabled"));

        int x = 159;
        int y = 4;

        guiGraphics.blitSprite(sprite, leftPos + x, topPos + y, 11, 11);

        // Below is rendering of a xp level number with outline
        // Mojang did this with texture, but in our case it needs to be dynamic (because it's configurable)

        String text = Integer.toString(cost);
        int centerColor = hasEnough ? 0xFFC8FF8F : 0xFF8C605D;
        int outlineColor = hasEnough ? 0xFF2D2102 : 0xFF47352F;

        x += 7;
        y += 2;

        guiGraphics.drawString(font, text, leftPos + x, topPos + y - 1, outlineColor, false);
        guiGraphics.drawString(font, text, leftPos + x + 1, topPos + y - 1, outlineColor, false);
        guiGraphics.drawString(font, text, leftPos + x + 1, topPos + y, outlineColor, false);
        guiGraphics.drawString(font, text, leftPos + x + 1, topPos + y + 1, outlineColor, false);
        guiGraphics.drawString(font, text, leftPos + x, topPos + y + 1, outlineColor, false);
        guiGraphics.drawString(font, text, leftPos + x - 1, topPos + y + 1, outlineColor, false);
        guiGraphics.drawString(font, text, leftPos + x - 1, topPos + y, outlineColor, false);

        guiGraphics.drawString(font, text, leftPos + x, topPos + y, centerColor, false);
    }
}