package io.github.mortuusars.envelope.client.gui.screen;

import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.mortaar.client.Minecrft;
import io.github.mortuusars.envelope.network.Packets;
import io.github.mortuusars.envelope.network.packet.serverbound.PackingMenuPresetAddressC2SP;
import io.github.mortuusars.envelope.world.inventory.PackingMenu;
import io.github.mortuusars.envelope.world.mail.address.Address;
import io.github.mortuusars.mortaar.client.gui.Sprites;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.util.Optional;

public class PackingScreen extends AbstractInHandContainerScreen<PackingMenu> {
    public static final ResourceLocation TEXTURE = Envelope.resource("textures/gui/packing.png");
    public static final WidgetSprites PACK_BUTTON_SPRITES = Sprites.threeStates(Envelope.resource("packing/pack_button"));
    public static final WidgetSprites PRESET_ADDRESS_BUTTON_SPRITES = Sprites.threeStates(Envelope.resource("packing/preset_address_button"));

    protected ImageButton packButton;
    protected ImageButton presetAddressButton;

    public PackingScreen(PackingMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, TEXTURE);
    }

    @Override
    protected void init() {
        imageWidth = 176;
        imageHeight = 178;
        super.init();

        packButton = addRenderableWidget(new ImageButton(leftPos + 126, topPos + 39, 26, 21,
              PACK_BUTTON_SPRITES,
              this::pack,
              Component.translatable("gui.envelope.packing.pack")));
        packButton.setTooltip(Tooltip.create(Component.translatable("gui.envelope.packing.pack")));

        presetAddressButton = addRenderableWidget(new ImageButton(leftPos + 152, topPos + 39, 20, 21,
              PRESET_ADDRESS_BUTTON_SPRITES,
              this::removePresetAddress,
              Component.translatable("gui.envelope.packing.preset_address")));
    }

    protected void pack(Button button) {
        getMenu().clickMenuButton(getMenu().getPlayer(), PackingMenu.PACK_BUTTON_ID);
        Minecrft.gameMode().handleInventoryButtonClick(getMenu().containerId, PackingMenu.PACK_BUTTON_ID);
        onClose();
    }

    protected void removePresetAddress(Button button) {
        getMenu().presetAddress(null);
        Packets.sendToServer(new PackingMenuPresetAddressC2SP(Optional.empty()));
    }

    @Override
    protected void updateButtons() {
        packButton.visible = getMenu().canPack();
        packButton.active = !getMenu().isPacked();
        presetAddressButton.visible = packButton.visible && getMenu().getPresetAddress() != null;
        presetAddressButton.active = packButton.active && !getMenu().getAddressTag().isEmpty();
        if (getMenu().getPresetAddress() instanceof Address presetAddress) {
            if (presetAddressButton.active) {
                presetAddressButton.setTooltip(Tooltip.create(
                      Component.empty()
                            .append(presetAddress.format().asNeutral().toComponent())
                            .append(CommonComponents.NEW_LINE)
                            .append(Component.translatable("gui.envelope.packing.preset_address.will_apply")
                                  .withStyle(ChatFormatting.GRAY))
                ));
            } else {
                presetAddressButton.setTooltip(Tooltip.create(
                      Component.empty()
                            .append(presetAddress.format().asNeutral().toComponent())
                            .append(CommonComponents.NEW_LINE)
                            .append(Component.translatable("gui.envelope.packing.preset_address.no_address_tag")
                                  .withStyle(ChatFormatting.GRAY))));
            }
        }
    }
}
