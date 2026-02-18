package io.github.mortuusars.envelope.client.gui.screen;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.client.gui.Sprites;
import io.github.mortuusars.envelope.client.gui.widget.CycleButton;
import io.github.mortuusars.envelope.client.util.Minecrft;
import io.github.mortuusars.envelope.world.GameTime;
import io.github.mortuusars.envelope.world.inventory.PaybackTagMenu;
import io.github.mortuusars.envelope.world.inventory.slot.GhostSlot;
import io.github.mortuusars.envelope.world.item.component.PaybackDuration;
import io.github.mortuusars.envelope.world.item.component.PaybackRequest;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class PaybackTagScreen extends AbstractInHandContainerScreen<PaybackTagMenu> {
    public static final ResourceLocation TEXTURE = Envelope.resource("textures/gui/payback_tag.png");

    protected CycleButton<PaybackDuration> durationButton;
    protected ImageButton confirmButton;

    public PaybackTagScreen(PaybackTagMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, TEXTURE);
    }

    @Override
    protected void init() {
        imageWidth = 186;
        imageHeight = 161;
        super.init();
        inventoryLabelX = 12;

        titleLabelX = (imageWidth / 2) - (font.width(getTitle()) / 2);

        durationButton = addRenderableWidget(createDurationButton());

        confirmButton = new ImageButton(leftPos + 133, topPos + 31, 19, 19,
              Sprites.CONFIRM_BUTTON_SPRITES, button -> confirm(), Component.translatable("gui.envelope.confirm"));
        confirmButton.setTooltip(Tooltip.create(Component.translatable("gui.envelope.confirm")));
        addRenderableWidget(confirmButton);
    }

    protected CycleButton<PaybackDuration> createDurationButton() {
        Map<PaybackDuration, WidgetSprites> durationSprites = Map.of(
              PaybackDuration.SHORT, Sprites.threeStates(Envelope.resource("payback/duration_short")),
              PaybackDuration.MEDIUM, Sprites.threeStates(Envelope.resource("payback/duration_medium")),
              PaybackDuration.LONG, Sprites.threeStates(Envelope.resource("payback/duration_long"))
        );

        Map<PaybackDuration, Tooltip> durationTooltips = Map.of(
              PaybackDuration.SHORT, Tooltip.create(Component.translatable("gui.envelope.payback_tag.duration_short")
                    .append(CommonComponents.NEW_LINE)
                    .append(GameTime.format(PaybackDuration.SHORT.getTicks(), false).withStyle(ChatFormatting.GRAY))),
              PaybackDuration.MEDIUM, Tooltip.create(Component.translatable("gui.envelope.payback_tag.duration_medium")
                    .append(CommonComponents.NEW_LINE)
                    .append(GameTime.format(PaybackDuration.MEDIUM.getTicks(), false).withStyle(ChatFormatting.GRAY))),
              PaybackDuration.LONG, Tooltip.create(Component.translatable("gui.envelope.payback_tag.duration_long")
                    .append(CommonComponents.NEW_LINE)
                    .append(GameTime.format(PaybackDuration.LONG.getTicks(), false).withStyle(ChatFormatting.GRAY)))
        );

        return new CycleButton<>(leftPos + 24, topPos + 32, 29, 16,
              Arrays.stream(PaybackDuration.values()).toList(), getMenu().getPaybackDuration(), durationSprites)
              .setTooltips(durationTooltips)
              .setLooping(true)
              .setClickSound(SoundEvents.UI_BUTTON_CLICK.value())
              .onCycle(this::changeDuration);
    }

    protected void changeDuration(PaybackDuration duration) {
        getMenu().clickMenuButton(getMenu().getPlayer(), PaybackTagMenu.DURATION_BUTTON_ID + duration.ordinal());
        Minecrft.gameMode().handleInventoryButtonClick(getMenu().containerId, PaybackTagMenu.DURATION_BUTTON_ID + duration.ordinal());
    }

    protected void confirm() {
        getMenu().clickMenuButton(getMenu().getPlayer(), PaybackTagMenu.CONFIRM_BUTTON_ID);
        Minecrft.gameMode().handleInventoryButtonClick(getMenu().containerId, PaybackTagMenu.CONFIRM_BUTTON_ID);
        onClose();
    }

    @Override
    protected void containerTick() {
        PaybackDuration duration = getMenu().getPaybackDuration();
        if (durationButton.getCurrentValue() != duration) {
            durationButton.setCurrentValue(duration);
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTargetPreview(guiGraphics, mouseX, mouseY);
    }

    protected void renderTargetPreview(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY) {
        ItemStack target = getMenu().getTargetPreview();
        if (target.isEmpty()) {
            return;
        }

        float scale = 2f;
        int size = (int) (16 * scale);

        int x = leftPos - size - 4;
        int y = topPos + 38 - size / 2;

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(x + (float) size / 2, y + (float) size / 2, 0);
        guiGraphics.pose().scale(scale, scale, scale);
        guiGraphics.pose().translate(-8, -8, 0);
        guiGraphics.renderItem(target, 0, 0);
        guiGraphics.pose().popPose();

        if (isHovering(x - leftPos, y - topPos, size, size, mouseX, mouseY)) {
            guiGraphics.renderTooltip(font, target, mouseX, mouseY);
        }
    }

    // -- Input


    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        lastQuickMoved = ItemStack.EMPTY; // Prevents fast shift-clicking moving more items that needed
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (super.mouseScrolled(mouseX, mouseY, scrollX, scrollY)) {
            return true;
        }

        if (hoveredSlot != null && hoveredSlot.index < PaybackRequest.SLOTS) {
            boolean decreasing = scrollY < 0;
            boolean fast = Screen.hasShiftDown();
            changeRequestedItemCount(decreasing, fast);
            return true;
        }

        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (super.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }

        if (hoveredSlot != null && hoveredSlot.index < PaybackRequest.SLOTS) {
            if (keyCode == InputConstants.KEY_ADD || keyCode == InputConstants.KEY_EQUALS) {
                changeRequestedItemCount(false, Screen.hasShiftDown());
                return true;
            }

            if (keyCode == 333 /*KEY_SUBTRACT*/ || keyCode == InputConstants.KEY_MINUS) {
                changeRequestedItemCount(true, Screen.hasShiftDown());
                return true;
            }
        }

        return false;
    }

    @Override
    protected void renderTooltip(GuiGraphics guiGraphics, int x, int y) {
        if (hoveredSlot instanceof GhostSlot slot && slot.hasItem()) {
            // Prevents tooltip overlapping the slot, to not obstruct the item count
            x = Math.max(x, leftPos + slot.x + 11);
        }
        super.renderTooltip(guiGraphics, x, y);
    }

    @Override
    protected @NotNull List<Component> getTooltipFromContainerItem(ItemStack stack) {
        if (hoveredSlot instanceof GhostSlot slot && slot.getItem() == stack) {
            List<Component> components = super.getTooltipFromContainerItem(stack);
            components.add(Component.translatable("gui.envelope.payback_tag.tooltip.scroll"));
            return components;
        }
        return super.getTooltipFromContainerItem(stack);
    }

    protected void changeRequestedItemCount(boolean decreasing, boolean fast) {
        if (hoveredSlot == null) return;
        int startId = decreasing ? PaybackTagMenu.DECREASE_COUNT_START_BUTTON_ID : PaybackTagMenu.INCREASE_COUNT_START_BUTTON_ID;
        int id = startId + hoveredSlot.index + (fast ? PaybackRequest.SLOTS : 0);
        getMenu().clickMenuButton(getMenu().getPlayer(), id);
        Minecrft.gameMode().handleInventoryButtonClick(getMenu().containerId, id);
    }
}
