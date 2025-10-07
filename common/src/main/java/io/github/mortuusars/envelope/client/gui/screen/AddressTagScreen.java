package io.github.mortuusars.envelope.client.gui.screen;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.client.gui.Sprites;
import io.github.mortuusars.envelope.client.gui.widget.textbox.TextBox;
import io.github.mortuusars.envelope.client.gui.widget.textbox.text.FormattedString;
import io.github.mortuusars.envelope.client.util.Minecrft;
import io.github.mortuusars.envelope.mail.Address;
import io.github.mortuusars.envelope.mail.AddressDisplay;
import io.github.mortuusars.envelope.network.Packets;
import io.github.mortuusars.envelope.network.packet.serverbound.AddressTagApplyC2SP;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public class AddressTagScreen extends Screen {
    public static final ResourceLocation TEXTURE = Envelope.resource("textures/gui/address_tag.png");

    public static final WidgetSprites CONFIRM_BUTTON_SPRITES =
            Sprites.threeStates(Envelope.resource("address_tag/confirm_button"));

    protected final ItemStack tag;
    protected final InteractionHand hand;
    protected final List<Address> knownAddresses;

    protected int imageWidth, imageHeight;
    protected int leftPos, topPos;
    protected int titleLabelX, titleLabelY;

    protected TextBox addressBox;
    protected ImageButton confirmButton;

    protected Address matchedKnownAddress = Address.UNKNOWN;

    public AddressTagScreen(InteractionHand hand, List<Address> knownAddresses) {
        super(Component.translatable("gui.envelope.address_tag.title"));
        this.tag = Minecrft.player().getItemInHand(hand).copy(); // Copying to not cause client/server desync if edits are canceled.
        this.hand = hand;
        this.knownAddresses = knownAddresses;
    }

    @Override
    protected void init() {
        imageWidth = 183;
        imageHeight = 33;
        leftPos = (width - imageWidth) / 2;
        topPos = (height - imageHeight) / 2;
        titleLabelX = leftPos + 12;
        titleLabelY = topPos + 7;

        @Nullable Address address = tag.get(Envelope.DataComponents.ADDRESS);
        String addressText = "";
        if (address != null) {
            addressText = address.id();
        }
        addressBox = new TextBox(font, leftPos + 20, topPos + 18, 140, 9)
                .setTextValidator(text -> text.length() <= 22 && !text.contains("\n"))
                .setFormattingEnabled(false)
                .setFontColor(0xFF7B593D)
                .setFontUnfocusedColor(0xFF7B593D)
                .setSelectionColor(0xFF664488)
                .setSelectionUnfocusedColor(0xFF696170)
                .setHintColor(0xFFC2A57F)
                .setOnTextChanged(this::onAddressTextChanged);
        addressBox.setTextAndUpdate(FormattedString.parse(addressText));
        addressBox.getEditor().setCursorToEnd(false);
        addRenderableWidget(addressBox);

        confirmButton = new ImageButton(leftPos + 162, topPos + 16, 11, 11, CONFIRM_BUTTON_SPRITES,
                button -> confirm(), Component.translatable("gui.envelope.confirm"));
        confirmButton.setTooltip(Tooltip.create(Component.translatable("gui.envelope.confirm")));
        addRenderableWidget(confirmButton);

        setInitialFocus(addressBox);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // --

    protected void fillLastAddress() {
        //TODO: fill last address
        String lastAddress = ChatFormatting.stripFormatting("Last address goes here");
        addressBox.setTextAndUpdate(FormattedString.parse(lastAddress));
        //TODO: move focus to addressBox, but it's not that simple,
        // it should be deferred somehow, as it's set to the button after handling click
    }

    protected void confirm() {
        Minecrft.get().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_CARTOGRAPHY_TABLE_TAKE_RESULT, 1f));
        int slot = this.hand == InteractionHand.MAIN_HAND ? Minecrft.player().getInventory().selected : Inventory.SLOT_OFFHAND;
        Packets.sendToServer(new AddressTagApplyC2SP(slot, getAddress()));
        onClose();
    }

    protected Optional<Address> getAddress() {
        String addressText = addressBox.getEditor().getText().toString().trim();
        if (addressText.isBlank()) {
            return Optional.empty();
        }

        Address address = matchedKnownAddress != Address.UNKNOWN
                ? matchedKnownAddress
                : new Address.Pigeonhole(addressText);
        return Optional.ofNullable(address);
    }

    // -- Events

    protected void onAddressTextChanged(FormattedString text) {
        String string = text.toString();
        matchedKnownAddress = Address.UNKNOWN;
        for (Address address : knownAddresses) {
            if (address.id().equalsIgnoreCase(string)) {
                matchedKnownAddress = address;
                break;
            }
        }

        getAddress().ifPresentOrElse(
                value -> tag.set(Envelope.DataComponents.ADDRESS, value),
                () -> tag.remove(Envelope.DataComponents.ADDRESS));
    }

    // -- Render

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.drawString(font, title, titleLabelX, titleLabelY, 0xFFB89B76, false);
        renderAddressType(guiGraphics, mouseX, mouseY, partialTick);
        renderTargetPreview(guiGraphics, mouseX, mouseY);
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderTransparentBackground(guiGraphics);
        guiGraphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
    }

    protected void renderAddressType(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        int color = matchedKnownAddress == Address.UNKNOWN ? 0xFFB89B76 : 0xFF7B593D;
        guiGraphics.drawString(font, AddressDisplay.getIcon(matchedKnownAddress),
                leftPos + 12, topPos + 18, color, false);
        if (matchedKnownAddress != Address.UNKNOWN && isHovering(9, 17, 9, 9, mouseX, mouseY)) {
            guiGraphics.renderTooltip(font, Component.translatable("address.envelope.type."
                    + matchedKnownAddress.type().getSerializedName()), mouseX, mouseY);
        }
    }

    protected void renderTargetPreview(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.renderItem(tag, leftPos - 20, topPos + 8);
        if (isHovering(-20, 8, 18, 18, mouseX, mouseY)) {
            guiGraphics.renderTooltip(font, tag, mouseX, mouseY);
        }
    }

    // -- Input

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == InputConstants.KEY_ESCAPE) {
            onClose();
        }

        if (keyCode == InputConstants.KEY_RETURN) {
            confirm();
            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == InputConstants.MOUSE_BUTTON_RIGHT && addressBox.isMouseOver(mouseX, mouseY)) {
            addressBox.setTextAndUpdate(FormattedString.parse(""));
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    // --

    protected boolean isHovering(int x, int y, int width, int height, int mouseX, int mouseY) {
        return mouseX >= leftPos + x && mouseX < leftPos + x + width
                && mouseY >= topPos + y && mouseY < topPos + y + height;
    }
}
