package io.github.mortuusars.envelope.client.gui.screen;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.client.gui.Sprites;
import io.github.mortuusars.envelope.client.gui.widget.AddressBoxSuggestions;
import io.github.mortuusars.envelope.client.util.Minecrft;
import io.github.mortuusars.envelope.util.Colors;
import io.github.mortuusars.envelope.world.mail.address.Address;
import io.github.mortuusars.envelope.world.mail.address.AddressFormatter;
import io.github.mortuusars.envelope.world.mail.address.AllAddresses;
import io.github.mortuusars.envelope.network.Packets;
import io.github.mortuusars.envelope.network.packet.serverbound.AddressTagApplyC2SP;
import io.github.mortuusars.envelope.world.mail.address.type.BlockAddress;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
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

import java.util.Optional;
import java.util.stream.Collectors;

public class AddressTagScreen extends Screen {
    public static final ResourceLocation TEXTURE = Envelope.resource("textures/gui/address_tag.png");

    public static final WidgetSprites CONFIRM_BUTTON_SPRITES = Sprites.threeStates(Envelope.resource("address_tag/confirm"));

    protected final InteractionHand hand;
    protected final ItemStack tag;
    protected final AllAddresses knownAddresses;
    protected Optional<Address> existingAddress;

    protected int imageWidth, imageHeight;
    protected int leftPos, topPos;
    protected int titleLabelX, titleLabelY;

    protected EditBox addressBox;
    protected AddressBoxSuggestions suggestions;
    protected ImageButton confirmButton;

    protected Optional<Address> matchedKnownAddress = Optional.empty();
    protected @Nullable String currentSuggestion;

    public AddressTagScreen(InteractionHand hand, AllAddresses knownAddresses, Component title) {
        super(title);
        this.hand = hand;
        this.tag = Minecrft.player().getItemInHand(hand).copy(); // Copying to not cause client/server desync if edits are canceled.
        this.knownAddresses = knownAddresses;
        this.existingAddress = Optional.ofNullable(tag.get(Envelope.DataComponents.ADDRESS));
    }

    @Override
    protected void init() {
        imageWidth = 186;
        imageHeight = 40;
        leftPos = (width - imageWidth) / 2;
        topPos = (height - imageHeight) / 2;
        titleLabelX = leftPos + 12;
        titleLabelY = topPos + 6;

        addressBox = new EditBox(font, leftPos + 26, topPos + 21, 125, 9, Component.empty());
        addressBox.setValue(getInitialAddressValue());
        addressBox.setMaxLength(BlockAddress.MAX_LENGTH);
        addressBox.setTextColor(Colors.WHITE);
        addressBox.setResponder(this::addressTextChanged);
        addressBox.setBordered(false);
        addressBox.setCanLoseFocus(false);
        addRenderableWidget(addressBox);

        suggestions = new AddressBoxSuggestions(addressBox, getAddressesForSuggestions(), 6);
        suggestions.update();

        confirmButton = new ImageButton(leftPos + 158, topPos + 17, 16, 16, CONFIRM_BUTTON_SPRITES,
              button -> confirm(), Component.translatable("gui.envelope.confirm"));
        confirmButton.setTooltip(Tooltip.create(Component.translatable("gui.envelope.confirm")));
        addRenderableWidget(confirmButton);

        setInitialFocus(addressBox);

        addressTextChanged(addressBox.getValue());
    }

    // --

    protected ItemStack getTargetPreview() {
        return tag;
    }

    // -- Address

    protected String getCurrentAddressId() {
        return addressBox.getValue().trim();
    }

    protected String getInitialAddressValue() {
        return existingAddress
              .map(Address::getString)
              .orElse("");
    }

    protected Optional<Address> getMatchedKnownAddress() {
        return matchedKnownAddress;
    }

    protected Optional<Address> getOrCreateAddressFromCurrentValue() {
        String addressId = getCurrentAddressId().trim();
        if (addressId.isBlank()) {
            return Optional.empty();
        }

        return getMatchedKnownAddress().or(() -> Optional.of(new BlockAddress(addressId)));
    }

    protected AllAddresses getAddressesForSuggestions() {
        return new AllAddresses(
              knownAddresses.blocks(),
              knownAddresses.players(),
              knownAddresses.entities().stream()
                    .filter(address -> !address.getEntityHolder().is(Envelope.Tags.MailEntities.HIDDEN))
                    .collect(Collectors.toSet()));
    }

    protected boolean isRenaming() {
        return existingAddress.isPresent();
    }

    protected boolean isCurrentIdSameAsExistingAddress() {
        return existingAddress
              .map(address -> address.matches(getCurrentAddressId().trim()))
              .orElse(false);
    }

    protected void updateItem() {
        getOrCreateAddressFromCurrentValue().ifPresentOrElse(
              value -> tag.set(Envelope.DataComponents.ADDRESS, value),
              () -> tag.remove(Envelope.DataComponents.ADDRESS));
    }

    // -- Events

    protected void addressTextChanged(String text) {
        String addressId = text.trim();
        matchedKnownAddress = knownAddresses.byName(addressId);
        updateItem();

//        boolean showSuggestion = matchedKnownAddress.isPresent() || text.isBlank();
//        @Nullable String suggestion = !showSuggestion ? getAutocompleteSuggestion(text) : null;
//        currentSuggestion = suggestion;
//        if (suggestion != null && !suggestion.equalsIgnoreCase(addressBox.getValue())) {
//            addressBox.setSuggestion(suggestion.substring(addressBox.getValue().length()));
//        } else {
//            addressBox.setSuggestion(suggestion);
//        }

        suggestions.update();
    }

    protected boolean confirm() {
        if (!isCurrentIdSameAsExistingAddress()) {
            Minecrft.get().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_CARTOGRAPHY_TABLE_TAKE_RESULT, 1f));
            int slot = this.hand == InteractionHand.MAIN_HAND ? Minecrft.player().getInventory().selected : Inventory.SLOT_OFFHAND;
            Packets.sendToServer(new AddressTagApplyC2SP(slot, getOrCreateAddressFromCurrentValue()));
        }
        close();
        return true;
    }

    @Override
    public void resize(Minecraft minecraft, int width, int height) {
        // Prevent contents reset when window is resized
        String text = addressBox.getValue();
        int cursorPosition = addressBox.getCursorPosition();
        super.resize(minecraft, width, height);
        addressBox.setValue(text);
        addressBox.setCursorPosition(cursorPosition);
        suggestions.update();
    }

    protected void close() {
        onClose();
    }

    // -- Render

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0.0F, 0.0F, 200.0F);
        suggestions.render(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.pose().popPose();

        renderLabels(guiGraphics);
        renderAddressIcon(guiGraphics, mouseX, mouseY, partialTick);
        renderTargetPreview(guiGraphics, mouseX, mouseY);
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderTransparentBackground(guiGraphics);
        guiGraphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
    }

    protected void renderLabels(@NotNull GuiGraphics guiGraphics) {
        guiGraphics.drawString(font, title, titleLabelX, titleLabelY, Colors.GUI_LABEL, false);
    }

    protected void renderAddressIcon(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        Address address = getMatchedKnownAddress().orElse(Address.UNKNOWN);

        int color = !address.equals(Address.UNKNOWN) ? 0xFFFFEAC2 : 0x88FFEAC2;
        guiGraphics.drawString(font, AddressFormatter.getIcon(address), leftPos + 17, topPos + 21, color, true);

        if (address != Address.UNKNOWN && isHovering(15, 20, 9, 9, mouseX, mouseY)) {
            guiGraphics.renderTooltip(font, address.getType().translate(), mouseX, mouseY);
        }
    }

    protected void renderTargetPreview(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY) {
        ItemStack target = getTargetPreview();
        if (target.isEmpty()) {
            return;
        }

        float scale = 2f;
        int size = (int) (16 * scale);

        int x = leftPos - size - 4;
        int y = topPos + (imageHeight - size) / 2;

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
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (suggestions.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }

        if (keyCode == InputConstants.KEY_ESCAPE) {
            close();
            return true;
        }

        if (keyCode == InputConstants.KEY_RETURN || keyCode == InputConstants.KEY_NUMPADENTER) {
            if (confirm()) {
                Minecrft.get().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1f));
            }
            return true;
        }

        if (keyCode == InputConstants.KEY_TAB && currentSuggestion != null) {
            addressBox.setValue(currentSuggestion);
            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (suggestions.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }

        if (button == InputConstants.MOUSE_BUTTON_RIGHT && addressBox.isMouseOver(mouseX, mouseY)) {
            if (!addressBox.getValue().isEmpty()) {
                addressBox.setValue("");
            }
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        suggestions.mouseMoved(mouseX, mouseY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (suggestions.mouseScrolled(mouseX, mouseY, scrollX, scrollY)) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    // --

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    /**
     * Used by mixin to change EditBox suggestion color.
     */
    public int getSuggestionTextColor() {
        return 0x88FFFFFF;
    }

    protected boolean isHovering(int x, int y, int width, int height, int mouseX, int mouseY) {
        return mouseX >= leftPos + x && mouseX < leftPos + x + width
              && mouseY >= topPos + y && mouseY < topPos + y + height;
    }
}