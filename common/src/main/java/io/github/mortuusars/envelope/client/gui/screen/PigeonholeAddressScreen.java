package io.github.mortuusars.envelope.client.gui.screen;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.client.gui.Sprites;
import io.github.mortuusars.envelope.client.util.Minecrft;
import io.github.mortuusars.envelope.network.Packets;
import io.github.mortuusars.envelope.network.packet.serverbound.UpdatePigeonholeMenuAddressC2SP;
import io.github.mortuusars.envelope.world.inventory.PigeonholeAddressMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
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
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class PigeonholeAddressScreen extends AbstractContainerScreen<PigeonholeAddressMenu> {
    public static final ResourceLocation TEXTURE = Envelope.resource("textures/gui/pigeonhole_address.png");

    protected EditBox name;
    private ImageButton confirmButton;

    public PigeonholeAddressScreen(PigeonholeAddressMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    protected void init() {
        imageWidth = 164;
        imageHeight = 66;
        inventoryLabelX = -999;
        inventoryLabelY = -999;
        titleLabelX = 20;
        super.init();

        name = new EditBox(this.font, leftPos + 11, topPos + 21, 142, 12, Component.translatable("gui.exposure.pigeonhole.address_box.title"));
        name.setTextColor(-1);
        name.setTextColorUneditable(-1);
        name.setBordered(false);
        name.setMaxLength(PigeonholeAddressMenu.MAX_NAME_LENGTH);
        name.setResponder(this::onAddressChanged);
        name.setValue(getMenu().getAddress());
        addWidget(name);
        setInitialFocus(name);

        confirmButton = new ImageButton(leftPos + 112, topPos + 42, 19, 19,
                Sprites.CONFIRM_BUTTON_SPRITES,
                button -> confirm(), Component.translatable("gui.envelope.confirm"));
        MutableComponent confirmTooltip = Component.translatable("gui.envelope.confirm");
        if (getMenu().isRenaming()) {
            confirmTooltip.append("\n")
                    .append(Component.translatable("gui.envelope.pigeonhole_address.rename_warning.inbox")
                            .withStyle(Style.EMPTY.withColor(0xFFE76A6A)))
                    .append("\n")
                    .append(Component.translatable("gui.envelope.pigeonhole_address.rename_warning.traveling")
                            .withStyle(Style.EMPTY.withColor(0xFFE76A6A)));
        }
        confirmButton.setTooltip(Tooltip.create(confirmTooltip));
        addRenderableWidget(confirmButton);

        ImageButton cancelButton = new ImageButton(leftPos + 133, topPos + 42, 19, 19,
                Sprites.CANCEL_BUTTON_SPRITES,
                button -> cancel(), Component.translatable("gui.envelope.cancel"));
        cancelButton.setTooltip(Tooltip.create(Component.translatable("gui.envelope.cancel")));
        addRenderableWidget(cancelButton);
    }

    protected boolean canConfirm() {
        return getMenu().canConfirm();
    }

    protected void confirm() {
        if (!canConfirm()) {
            return;
        }

        getMenu().clickMenuButton(Minecraft.getInstance().player, PigeonholeAddressMenu.APPLY_BUTTON_ID);
        Objects.requireNonNull(Minecraft.getInstance().gameMode).handleInventoryButtonClick(getMenu().containerId, PigeonholeAddressMenu.APPLY_BUTTON_ID);
        onClose();
    }

    protected void cancel() {
        onClose();
    }

    @Override
    public void resize(Minecraft minecraft, int width, int height) {
        String string = this.name.getValue();
        this.init(minecraft, width, height);
        this.name.setValue(string);
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        confirmButton.active = canConfirm();
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.name.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderTooltip(GuiGraphics guiGraphics, int x, int y) {
        super.renderTooltip(guiGraphics, x, y);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
    }

    protected void onAddressChanged(String address) {
        getMenu().setAddress(address);
        Packets.sendToServer(new UpdatePigeonholeMenuAddressC2SP(address));
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == InputConstants.KEY_ESCAPE) {
            cancel();
        }

        if (keyCode == InputConstants.KEY_RETURN) {
            if (canConfirm()) {
                confirm();
                Minecrft.get().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(), 1f));
            }
            return true;
        }

        if (keyCode != InputConstants.KEY_TAB && (name.keyPressed(keyCode, scanCode, modifiers) || name.canConsumeInput())) {
            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == InputConstants.MOUSE_BUTTON_RIGHT && name.isMouseOver(mouseX, mouseY)) {
            name.setValue("");
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }
}
