package io.github.mortuusars.envelope.client.gui.screen;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.client.gui.Sprites;
import io.github.mortuusars.envelope.client.gui.widget.textbox.TextBox;
import io.github.mortuusars.envelope.client.gui.widget.textbox.text.FormattedString;
import io.github.mortuusars.envelope.client.util.Minecrft;
import io.github.mortuusars.envelope.mail.Address;
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

    public static final WidgetSprites ADDRESS_SPRITES = Sprites.normalAndHighlighted(Envelope.resource("address_tag/address"));
    public static final WidgetSprites UNKNOWN_PIGEONHOLE_SPRITES = Sprites.normalOnly(Envelope.resource("address_tag/unknown_pigeonhole"));
    public static final WidgetSprites PIGEONHOLE_SPRITES = Sprites.normalAndHighlighted(Envelope.resource("address_tag/pigeonhole"));
    public static final WidgetSprites PLAYER_SPRITES = Sprites.normalAndHighlighted(Envelope.resource("address_tag/player"));
    public static final WidgetSprites NPC_SPRITES = Sprites.normalAndHighlighted(Envelope.resource("address_tag/npc"));

    protected final ItemStack tag;
    protected final InteractionHand hand;
    protected final List<Address> knownAddresses;

    protected int imageWidth, imageHeight;
    protected int leftPos, topPos;
    protected int titleLabelX, titleLabelY;

    protected ImageButton addressButton;
    protected TextBox addressBox;
    protected ImageButton confirmButton;

    protected Address matchedKnownAddress = Address.UNKNOWN;

    public AddressTagScreen(ItemStack tag, InteractionHand hand, List<Address> knownAddresses) {
        super(Component.translatable("gui.envelope.address_tag.title"));
        this.tag = tag;
        this.hand = hand;
        this.knownAddresses = knownAddresses;
    }

    @Override
    protected void init() {
        imageWidth = 221;
        imageHeight = 32;
        leftPos = (width - imageWidth) / 2;
        topPos = (height - imageHeight) / 2;
        titleLabelX = leftPos + 42;
        titleLabelY = topPos + 5;

        addressButton = new ImageButton(leftPos + 29, topPos + 4, 10, 10, ADDRESS_SPRITES, b -> fillLastAddress());
        addRenderableWidget(addressButton);

        @Nullable Address address = tag.get(Envelope.DataComponents.ADDRESS);
        String addressText = "";
        if (address != null) {
            addressText = address.id();
        }
        addressBox = new TextBox(font, leftPos + 42, topPos + 17, 155, 9)
                .setTextValidator(text -> text.length() <= 25 && !text.contains("\n"))
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

        confirmButton = new ImageButton(leftPos + 202, topPos + 13, 19, 19,
                Sprites.CONFIRM_BUTTON_SPRITES,
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
        String lastAddress = ChatFormatting.stripFormatting("Placeholder");
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
        String addressText = addressBox.getEditor().getText().toString();
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

        guiGraphics.renderItem(tag, leftPos + 1, topPos + 7);

        if (mouseX >= leftPos + 1 && mouseX < leftPos + 19
                && mouseY >= topPos + 7 && mouseY < topPos + 25) {
            guiGraphics.renderTooltip(font, tag, mouseX, mouseY);
        }
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderTransparentBackground(guiGraphics);
        guiGraphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
    }

    protected void renderAddressType(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        WidgetSprites sprites = UNKNOWN_PIGEONHOLE_SPRITES;

        if (matchedKnownAddress != Address.UNKNOWN) {
            sprites = matchedKnownAddress.map(p -> PIGEONHOLE_SPRITES, pl -> PLAYER_SPRITES, npc -> NPC_SPRITES);
        }

        guiGraphics.blitSprite(sprites.enabled(), leftPos + 29, topPos + 16, 10, 10);
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
}
