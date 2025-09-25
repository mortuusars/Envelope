package io.github.mortuusars.envelope.client.gui.screen;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.client.gui.Sprites;
import io.github.mortuusars.envelope.client.gui.widget.textbox.TextBox;
import io.github.mortuusars.envelope.client.gui.widget.textbox.text.FormattedString;
import io.github.mortuusars.envelope.mail.Address;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

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

    protected Address matchedAddress = Address.UNKNOWN;

    public AddressTagScreen(ItemStack tag, InteractionHand hand, List<Address> knownAddresses) {
        super(Component.translatable("gui.envelope.address_tag.title"));
        this.tag = tag;
        this.hand = hand;
        this.knownAddresses = knownAddresses;
    }

    @Override
    protected void init() {
        imageWidth = 175;
        imageHeight = 33;
        leftPos = (width - imageWidth) / 2;
        topPos = (height - imageHeight) / 2;
        titleLabelX = 19;
        titleLabelY = 6;

//        name = new EditBox(this.font, leftPos + 11, topPos + 21, 142, 12, Component.translatable("gui.exposure.pigeonhole.address_box.title"));
//        name.setTextColor(-1);
//        name.setTextColorUneditable(-1);
//        name.setBordered(false);
//        name.setMaxLength(PigeonholeAddressMenu.MAX_NAME_LENGTH);
//        name.setResponder(this::onAddressChanged);
//        name.setValue(getMenu().getAddress());
//        addWidget(name);
//        setInitialFocus(name);

        addressButton = new ImageButton(leftPos + 6, topPos + 4, 10, 10, ADDRESS_SPRITES, b -> fillLastAddress());
        addRenderableWidget(addressButton);

        @Nullable Address address = tag.get(Envelope.DataComponents.ADDRESS);
        String addressText = "";
        if (address != null) {
            addressText = address.id();
        }
        addressBox = new TextBox(font, leftPos + 18, topPos + 17, 155, 9)
                .setTextValidator(text -> text.length() <= 25)
                .setFormattingEnabled(false)
                .setFontColor(0xFF7B593D)
                .setFontUnfocusedColor(0xFF7B593D)
                .setSelectionColor(0xFF664488)
                .setSelectionUnfocusedColor(0xFF696170)
                .setHintColor(0xFFC2A57F)
                .setOnTextChanged(this::onAddressTextChanged);
        addressBox.setTextAndUpdate(FormattedString.parse(addressText));
        addRenderableWidget(addressBox);

//        confirmButton = new ImageButton(leftPos + 112, topPos + 42, 19, 19,
//                Sprites.CONFIRM_BUTTON_SPRITES,
//                button -> confirm(), Component.translatable("gui.envelope.confirm"));
//        MutableComponent confirmTooltip = Component.translatable("gui.envelope.confirm");
//        if (getMenu().isRenaming()) {
//            confirmTooltip.append("\n")
//                    .append(Component.translatable("gui.envelope.pigeonhole_address.rename_warning.inbox")
//                            .withStyle(Style.EMPTY.withColor(0xFFE76A6A)))
//                    .append("\n")
//                    .append(Component.translatable("gui.envelope.pigeonhole_address.rename_warning.traveling")
//                            .withStyle(Style.EMPTY.withColor(0xFFE76A6A)));
//        }
//        confirmButton.setTooltip(Tooltip.create(confirmTooltip));
//        addRenderableWidget(confirmButton);
//
//        ImageButton cancelButton = new ImageButton(leftPos + 133, topPos + 42, 19, 19,
//                Sprites.CANCEL_BUTTON_SPRITES,
//                button -> cancel(), Component.translatable("gui.envelope.cancel"));
//        cancelButton.setTooltip(Tooltip.create(Component.translatable("gui.envelope.cancel")));
//        addRenderableWidget(cancelButton);
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

    // -- Events

    protected void onAddressTextChanged(FormattedString text) {
        String string = addressBox.getEditor().getText().toString();
        for (Address address : knownAddresses) {
            if (address.id().equalsIgnoreCase(string)) {
                matchedAddress = address;
                return;
            }
        }
        matchedAddress = Address.UNKNOWN;
    }

    // -- Render

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderAddressType(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
    }

    protected void renderAddressType(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        WidgetSprites sprites = UNKNOWN_PIGEONHOLE_SPRITES;

        if (matchedAddress != Address.UNKNOWN) {
            sprites = matchedAddress.map(p -> PIGEONHOLE_SPRITES, pl -> PLAYER_SPRITES, npc -> NPC_SPRITES);
        }

        guiGraphics.blitSprite(sprites.enabled(), leftPos + 6, topPos + 16, 10, 10);
    }

    // -- Input

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == InputConstants.KEY_ESCAPE) {
            onClose();
        }

//        if (keyCode == InputConstants.KEY_RETURN) {
//            if (canConfirm()) {
//                confirm();
//                Minecrft.get().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(), 1f));
//            }
//            return true;
//        }

//        if (keyCode != InputConstants.KEY_TAB && (name.keyPressed(keyCode, scanCode, modifiers) || name.canConsumeInput())) {
//            return true;
//        }

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
