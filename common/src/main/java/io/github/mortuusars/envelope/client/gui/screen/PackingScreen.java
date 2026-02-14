package io.github.mortuusars.envelope.client.gui.screen;

import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.client.gui.Sprites;
import io.github.mortuusars.envelope.client.util.Minecrft;
import io.github.mortuusars.envelope.world.inventory.PackingMenu;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class PackingScreen extends AbstractInHandContainerScreen<PackingMenu> {
    public static final ResourceLocation TEXTURE = Envelope.resource("textures/gui/packing.png");
    public static final WidgetSprites PACK_BUTTON_SPRITES = Sprites.threeStates(Envelope.resource("packing/pack_button"));

    protected ImageButton packButton;

    public PackingScreen(PackingMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, TEXTURE);
    }

    @Override
    protected void init() {
        imageWidth = 176;
        imageHeight = 178;
        super.init();

        packButton = addRenderableWidget(new ImageButton(leftPos + 126, topPos + 40, 26, 20,
              PACK_BUTTON_SPRITES,
              this::pack,
              Component.translatable("gui.envelope.package.pack")));
        packButton.setTooltip(Tooltip.create(Component.translatable("gui.envelope.package.pack")));
    }

    protected void pack(Button button) {
        getMenu().clickMenuButton(getMenu().getPlayer(), PackingMenu.PACK_BUTTON_ID);
        Minecrft.gameMode().handleInventoryButtonClick(getMenu().containerId, PackingMenu.PACK_BUTTON_ID);
        onClose();
    }

    @Override
    protected void updateButtons() {
        packButton.active = !getMenu().isPacked();
        packButton.visible = getMenu().canPack();
    }
}
