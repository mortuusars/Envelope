package io.github.mortuusars.envelope.client.gui.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.client.gui.Sprites;
import io.github.mortuusars.envelope.client.util.Minecrft;
import io.github.mortuusars.envelope.client.util.Pos2i;
import io.github.mortuusars.envelope.world.inventory.PaybackPackageMenu;
import io.github.mortuusars.envelope.world.inventory.RequestedItem;
import io.github.mortuusars.envelope.world.item.component.Payback;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class PaybackPackageScreen extends AbstractContainerScreen<PaybackPackageMenu> {
    public static final ResourceLocation TEXTURE = Envelope.resource("textures/gui/payback_package.png");
    public static final WidgetSprites PACK_BUTTON_SPRITES = Sprites.threeStates(Envelope.resource("payback_package/pack_button"));

    protected ImageButton packButton;

    public PaybackPackageScreen(PaybackPackageMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    protected void init() {
        imageWidth = 176;
        imageHeight = 178;
        super.init();
        inventoryLabelY = imageHeight - 94;

        packButton = new ImageButton(leftPos + 126, topPos + 40, 26, 20,
              PACK_BUTTON_SPRITES,
              button -> pack(),
              Component.translatable("gui.envelope.payback_package.pack"));
        packButton.setTooltip(Tooltip.create(Component.translatable("gui.envelope.payback_package.pack")
              .append(CommonComponents.NEW_LINE)
              .append(Component.translatable("gui.envelope.package.pack.tooltip.packs_remaining",
                    getPrettyPacksRemaining()))));
        addRenderableWidget(packButton);
    }

    protected String getPrettyPacksRemaining() {
        int count = getMenu().getPackage().getRemainingPacks(getMenu().getPackageStack());
        if (count > 99) {
            return ">99";
        }
        return Integer.toString(count);
    }

    protected void pack() {
        Minecrft.gameMode().handleInventoryButtonClick(getMenu().containerId, PaybackPackageMenu.PACK_BUTTON_ID);
        onClose();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderSlotOverlays(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        packButton.visible = getMenu().needsPacking() && getMenu().canPack();
        guiGraphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);

        if (getMenu().isPackageDestroyedOnClose()) {
            guiGraphics.blit(TEXTURE, leftPos + 45, topPos + 17, 0, 178, 86, 64);
        }
    }

    @Override
    protected void renderSlot(GuiGraphics guiGraphics, Slot slot) {
        if (slot.isActive() && slot.index >= 0 && slot.index < Payback.SLOTS) {
            if (slot.getContainerSlot() < getMenu().getPayback().items().size()) {
                RequestedItem requestedItem = getMenu().getPayback().items().get(slot.getContainerSlot());

                if (!slot.hasItem()) {
                    Item item = requestedItem.item().map(
                          tag -> BuiltInRegistries.ITEM.getTag(tag)
                                .map(named -> named.stream().findFirst()
                                      .map(Holder::value).orElse(Items.BARRIER))
                                .orElse(Items.BARRIER),
                          Holder::value
                    );
                    ItemStack display = new ItemStack(item, requestedItem.count());

                    guiGraphics.pose().pushPose();
                    guiGraphics.pose().translate(0, 0, -100);
                    guiGraphics.renderItem(display, slot.x, slot.y);
                    guiGraphics.renderItemDecorations(Minecrft.get().font, display, slot.x, slot.y);
                    guiGraphics.pose().popPose();
                }

                boolean isFulfilled = requestedItem.matches(slot.getItem());

                guiGraphics.pose().pushPose();
                guiGraphics.pose().translate(0, 0, 150);
                RenderSystem.enableBlend();
                guiGraphics.blit(TEXTURE, slot.x - 1, slot.y - 1, 176, isFulfilled ? 36 : 18, 18, 18);
                RenderSystem.disableBlend();
                guiGraphics.pose().popPose();
            }
        }

        super.renderSlot(guiGraphics, slot);
    }

    protected void renderSlotOverlays(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Inactive package slots
        for (int i = 0; i < Payback.SLOTS; i++) {
            Slot packageSlot = getMenu().slots.get(i);
            if (!packageSlot.isActive()) {
                guiGraphics.pose().pushPose();
                guiGraphics.pose().translate(0, 0, 300);
                RenderSystem.enableBlend();
                guiGraphics.blit(TEXTURE, leftPos + packageSlot.x - 1, topPos + packageSlot.y - 1, 176, 0, 18, 18);
                RenderSystem.disableBlend();
                guiGraphics.pose().popPose();
            }
        }

        // Subject
        guiGraphics.pose().pushPose();
        RenderSystem.enableBlend();
        guiGraphics.blit(TEXTURE, leftPos + 20, topPos + 41, 194, 0, 18, 18);
        guiGraphics.pose().translate(0, 0, 300);
        guiGraphics.blit(TEXTURE, leftPos + 20, topPos + 41, 194, 0, 18, 18);
        RenderSystem.disableBlend();
        guiGraphics.pose().popPose();

        renderPackageItemInInventory(guiGraphics, mouseX, mouseY, partialTick);
    }

    protected void renderPackageItemInInventory(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        Pos2i packageSlotPos = getMenu().getPackageSlotPos();
        guiGraphics.renderItem(getMenu().getPackageStack(), leftPos + packageSlotPos.x, topPos + packageSlotPos.y);

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0, 0, 300);
        RenderSystem.enableBlend();
        guiGraphics.blit(TEXTURE, leftPos + packageSlotPos.x - 1, topPos + packageSlotPos.y - 1, 176, 0, 18, 18);
        RenderSystem.disableBlend();
        guiGraphics.pose().popPose();
    }
}
