package io.github.mortuusars.envelope.client.gui.screen;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.mortuusars.envelope.Config;
import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.client.gui.widget.textbox.TextBox;
import io.github.mortuusars.envelope.client.gui.widget.textbox.text.FormattedString;
import io.github.mortuusars.envelope.client.util.Minecrft;
import io.github.mortuusars.envelope.network.Packets;
import io.github.mortuusars.envelope.network.packet.serverbound.UpdatePigeonholeMenuAddressC2SP;
import io.github.mortuusars.envelope.util.EasingFunction;
import io.github.mortuusars.envelope.util.EnvelopeSymbols;
import io.github.mortuusars.envelope.world.inventory.PigeonholeAddressTagMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

public class PigeonholeAddressTagScreen extends AbstractContainerScreen<PigeonholeAddressTagMenu> {
    protected TextBox addressBox;
    protected ImageButton confirmButton;

    public PigeonholeAddressTagScreen(PigeonholeAddressTagMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    protected void init() {
        imageWidth = 183;
        imageHeight = 33;
        inventoryLabelX = -999;
        inventoryLabelY = -999;
        titleLabelX = 12;
        titleLabelY = 7;
        super.init();

        addressBox = new TextBox(font, leftPos + 20, topPos + 18, 140, 9)
                .setTextValidator(text -> text.length() <= 22 && !text.contains("\n"))
                .setFormattingEnabled(false)
                .setFontColor(0xFF7B593D)
                .setFontUnfocusedColor(0xFF7B593D)
                .setSelectionColor(0xFF664488)
                .setSelectionUnfocusedColor(0xFF696170)
                .setHintColor(0xFFC2A57F)
                .setOnTextChanged(this::onAddressTextChanged);
        addressBox.setTextAndUpdate(FormattedString.parse(getMenu().getAddress()));
        addressBox.getEditor().setCursorToEnd(false);
        addRenderableWidget(addressBox);

        confirmButton = new ImageButton(leftPos + 162, topPos + 16, 11, 11, AddressTagScreen.CONFIRM_BUTTON_SPRITES,
                button -> confirm(), Component.translatable("gui.envelope.confirm"));
        addRenderableWidget(confirmButton);

        setInitialFocus(addressBox);
    }

    protected void updateConfirmButtonTooltip() {
        if (confirmButton == null) return; // If not initialized yet
        if (!getMenu().canConfirm()) {
            confirmButton.setTooltip(Tooltip.create(getMenu().getValidationState().translate()));
            return;
        }
        MutableComponent confirmTooltip = Component.translatable("gui.envelope.confirm");
        if (getMenu().isRenaming() && !getMenu().isSameAsCurrentAddress(getMenu().getAddress())) {
            confirmTooltip.append("\n")
                    .append(Component.translatable("gui.envelope.pigeonhole_address.rename_warning.inbox")
                            .withStyle(Style.EMPTY.withColor(0xFFE76A6A)))
                    .append("\n")
                    .append(Component.translatable("gui.envelope.pigeonhole_address.rename_warning.traveling")
                            .withStyle(Style.EMPTY.withColor(0xFFE76A6A)));
        }
        confirmButton.setTooltip(Tooltip.create(confirmTooltip));
    }

    protected void onAddressTextChanged(FormattedString text) {
        String string = text.toString();
        getMenu().setAddress(string);
        Packets.sendToServer(new UpdatePigeonholeMenuAddressC2SP(string));
    }

    protected boolean canConfirm() {
        return getMenu().canConfirm();
    }

    protected void confirm() {
        if (!canConfirm()) {
            return;
        }

        getMenu().clickMenuButton(getMenu().getPlayer(), PigeonholeAddressTagMenu.APPLY_BUTTON_ID);
        Minecrft.gameMode().handleInventoryButtonClick(getMenu().containerId, PigeonholeAddressTagMenu.APPLY_BUTTON_ID);
        onClose();
    }

    protected void cancel() {
        onClose();
    }

    // -- Render

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        updateConfirmButtonTooltip();
        confirmButton.active = canConfirm();
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderAddressType(guiGraphics, mouseX, mouseY, partialTick);
        renderTargetPreview(guiGraphics, mouseX, mouseY);
        renderExperienceCost(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(AddressTagScreen.TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(font, title, titleLabelX, titleLabelY, 0xFFB89B76, false);
    }

    protected void renderAddressType(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        boolean isValid = getMenu().getValidationState() != PigeonholeAddressTagMenu.AddressValidation.ERR_TAKEN;
        int color = isValid ? 0xFF7B593D : 0xFFFA5951;
        guiGraphics.drawString(font, EnvelopeSymbols.ADDRESS_PIGEONHOLE,
                leftPos + 12, topPos + 18, color, false);
        if (!isValid && isHovering(9, 17, 9, 9, mouseX, mouseY)) {
            guiGraphics.renderTooltip(font, getMenu().getValidationState().translate(), mouseX, mouseY);
        }
    }

    protected void renderTargetPreview(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY) {
        BlockState state = getMenu().getPlayer().level().getBlockState(getMenu().getPos());
        ItemStack stack = new ItemStack(state.getBlock().asItem());
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(leftPos - 28 + 8, topPos + 8 + 8, 0);
        float anim =  (System.currentTimeMillis() % 1000 / 1000f);
        anim = (float)EasingFunction.EASE_IN_OUT_QUAD.ease(anim);
        if (anim > 0.5f) {
            anim = 1f - anim;
        }
        float scale = 2f + anim * 0.35f;
        guiGraphics.pose().scale(scale, scale, scale);
        guiGraphics.pose().translate(-(leftPos - 28 + 8), -(topPos + 8 + 8), 0);
        guiGraphics.renderItem(stack, leftPos - 28, topPos + 8);
        guiGraphics.pose().popPose();
        if (isHovering(-36, 0, 32, 34, mouseX, mouseY)) {
            guiGraphics.renderTooltip(font, state.getBlock().getName(), mouseX, mouseY);
        }
    }

    protected void renderExperienceCost(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (getMenu().isSameAsCurrentAddress(getMenu().getAddress())) return;

        int cost = Config.Server.Pigeonhole.ADDRESS_EXPERIENCE_LEVELS_COST.get();
        if (cost <= 0) return;

        boolean hasEnough = getMenu().getPlayer().experienceLevel >= cost;
        ResourceLocation sprite = Envelope.resource("address_tag/experience" + (hasEnough ? "" : "_disabled"));

        int x = 150;
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

    // -- Input

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == InputConstants.KEY_ESCAPE) {
            cancel();
            return true;
        }

        if (keyCode == InputConstants.KEY_RETURN) {
            if (canConfirm()) {
                confirm();
                Minecrft.get().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(), 1f));
            }
            return true;
        }

        return this.addressBox.keyPressed(keyCode, scanCode, modifiers) || this.addressBox.canConsumeInput() || super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == InputConstants.MOUSE_BUTTON_RIGHT && addressBox.isMouseOver(mouseX, mouseY)) {
            addressBox.clearText();
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }
}
