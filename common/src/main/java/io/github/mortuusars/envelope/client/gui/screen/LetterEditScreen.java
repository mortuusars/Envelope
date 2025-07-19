package io.github.mortuusars.envelope.client.gui.screen;

import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.api.mail.Address;
import io.github.mortuusars.envelope.client.gui.Sprites;
import io.github.mortuusars.envelope.client.gui.widget.textbox.TextBox;
import io.github.mortuusars.envelope.client.gui.widget.textbox.text.FormattedString;
import io.github.mortuusars.envelope.client.state.ClientStateManager;
import io.github.mortuusars.envelope.client.state.FillRecipientState;
import io.github.mortuusars.envelope.client.util.Minecrft;
import io.github.mortuusars.envelope.network.Packets;
import io.github.mortuusars.envelope.network.packet.serverbound.EditLetterPacketC2SP;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public class LetterEditScreen extends Screen implements JeiKeyConflictResolverScreen {
    public static final ResourceLocation TEXTURE = Envelope.resource("textures/gui/letter.png");

    public static final WidgetSprites FILL_RECIPIENT_SPRITES = Sprites.normalAndHighlighted(Envelope.resource("widgets/fill_recipient"));

    protected final ItemStack letter;
    protected final InteractionHand hand;
    protected final List<Address> knownRecipients;
    protected final FillRecipientState fillRecipientState;

    protected int imageWidth, imageHeight, leftPos, topPos;
    protected TextBox recipientBox;
    protected TextBox subjectBox;
    protected TextBox messageBox;
    protected ImageButton fillRecipientButton;

    public LetterEditScreen(ItemStack letter, InteractionHand hand, List<Address> knownRecipients) {
        super(Component.empty());
        this.letter = letter;
        this.hand = hand;
        this.knownRecipients = knownRecipients;
        this.fillRecipientState = ClientStateManager.getFillRecipientState();
    }

    @Override
    public boolean isPauseScreen() {
        //TODO: config option
        return false;
    }

    @Override
    protected void init() {
        imageWidth = 200;
        imageHeight = 244;
        leftPos = (width - imageWidth) / 2;
        topPos = (height - imageHeight) / 2;

        fillRecipientButton = new ImageButton(leftPos + 18, topPos + 18, 11, 9, FILL_RECIPIENT_SPRITES, this::fillRecipient);
        addRenderableWidget(fillRecipientButton);

        @Nullable Address recipient = letter.get(Envelope.DataComponents.RECIPIENT);
        String recipientText = "";
        if (recipient != null) {
            recipientText = recipient.name();
        }
        recipientBox = new TextBox(font, leftPos + 30, topPos + 18, 140, 9)
                .setFormattingEnabled(false)
                .setFontColor(0xFF7B593D)
                .setFontUnfocusedColor(0xFF7B593D)
                .setSelectionColor(0xFF664488)
                .setSelectionUnfocusedColor(0xFF696170)
                .setHintColor(0xFFC2A57F)
                .setText(FormattedString.parse(recipientText))
                .setOnTextChanged(this::recipientTextChanged);
        addRenderableWidget(recipientBox);

        subjectBox = new TextBox(font, leftPos + 20, topPos + 39, 160, 19)
                .setFontColor(0xFF7B593D)
                .setFontUnfocusedColor(0xFF7B593D)
                .setSelectionColor(0xFF664488)
                .setSelectionUnfocusedColor(0xFF696170)
                .setHintColor(0xFFC2A57F)
                .setText(FormattedString.parse(letter.getOrDefault(Envelope.DataComponents.LETTER_SUBJECT, "")));
        addRenderableWidget(subjectBox);

        messageBox = new TextBox(font, leftPos + 20, topPos + 69, 160, 137)
                .setFontColor(0xFF7B593D)
                .setFontUnfocusedColor(0xFF7B593D)
                .setSelectionColor(0xFF664488)
                .setSelectionUnfocusedColor(0xFF696170)
                .setHintColor(0xFFC2A57F)
                .setText(FormattedString.parse(letter.getOrDefault(Envelope.DataComponents.LETTER_MESSAGE, "")));
        addRenderableWidget(messageBox);

        updateFillRecipientButton();
    }

    protected void recipientTextChanged(FormattedString chars) {
        updateFillRecipientButton();
    }

    private void updateFillRecipientButton() {
        MutableComponent tooltip = Component.translatable("gui.envelope.fill_recipient.tooltip.title");
        boolean canFillWithLastRecipient = !fillRecipientState.recipient.isBlank()
                && recipientBox != null
                && !fillRecipientState.recipient.equalsIgnoreCase(recipientBox.getEditor().getString().toString());
        boolean canFillWithLastSender = !fillRecipientState.sender.isBlank()
                && recipientBox != null
                && !fillRecipientState.sender.equalsIgnoreCase(recipientBox.getEditor().getString().toString());

        if (canFillWithLastRecipient) {
            tooltip.append("\n").append(Component.translatable("gui.envelope.fill_recipient.tooltip.lclick_last_recipient"));
        }
        if (canFillWithLastSender) {
            tooltip.append("\n").append(Component.translatable("gui.envelope.fill_recipient.tooltip.rclick_last_sender"));
        }

        fillRecipientButton.setTooltip(Tooltip.create(tooltip));

        fillRecipientButton.active = canFillWithLastRecipient || canFillWithLastSender;
    }

    protected void fillRecipient(Button button) {
        recipientBox.setText(Screen.hasShiftDown()
                ? FormattedString.parse(fillRecipientState.sender)
                : FormattedString.parse(fillRecipientState.recipient));
        recipientBox.getDisplayCache().scheduleUpdate();
        updateFillRecipientButton();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        guiGraphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderTransparentBackground(guiGraphics);
        guiGraphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
    }

    @Override
    public void onClose() {
        super.onClose();
        saveChanges();
    }

    // -- Input

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Special handling for toolbar, without this clicks will go to another element under the cursor,
        // if toolbar is over that element.
        if (getFocused() instanceof TextBox textBox && textBox.formattingToolbarMouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!(getFocused() instanceof TextBox)) {
            if (Minecraft.getInstance().options.keyInventory.matches(keyCode, scanCode)) {
                this.onClose();
                return true;
            }
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void setFocused(@Nullable GuiEventListener focused) {
        @Nullable GuiEventListener lastFocused = getFocused();
        super.setFocused(focused);
        // Clear selection if focus changes:
        if (lastFocused != null && !lastFocused.equals(getFocused()) && lastFocused instanceof TextBox textBox) {
            textBox.getEditor().clearSelection();
            textBox.getDisplayCache().scheduleUpdate();
        }
    }

    // --

    protected void saveChanges() {
        Optional<Address> recipient = getOrCreateRecipient(recipientBox.getEditor().getString().toStringWithoutFormatting());

        recipient.ifPresent(value -> {
            if (!fillRecipientState.recipient.equals(value.name())) {
                fillRecipientState.recipient = value.name();
                ClientStateManager.save();
            }
        });

        // Local

        recipient.ifPresentOrElse(
                value -> letter.set(Envelope.DataComponents.RECIPIENT, value),
                () -> letter.remove(Envelope.DataComponents.RECIPIENT));

        String subject = subjectBox.getEditor().getString().toString();
        if (!subject.isBlank()) {
            letter.set(Envelope.DataComponents.LETTER_SUBJECT, subject);
        } else {
            letter.remove(Envelope.DataComponents.LETTER_SUBJECT);
        }

        String message = messageBox.getEditor().getString().toString();
        if (!message.isBlank()) {
            letter.set(Envelope.DataComponents.LETTER_MESSAGE, message);
        } else {
            letter.remove(Envelope.DataComponents.LETTER_MESSAGE);
        }

        // Send to server

        int slot = this.hand == InteractionHand.MAIN_HAND ? Minecrft.player().getInventory().selected : Inventory.SLOT_OFFHAND;
        Packets.sendToServer(new EditLetterPacketC2SP(slot, recipient, subject, message));
    }

    protected Optional<Address> getOrCreateRecipient(String name) {
        if (name.isBlank()) {
            return Optional.empty();
        }

        name = name.trim();
        for (Address recipient : knownRecipients) {
            if (recipient.name().equalsIgnoreCase(name)) {
                return Optional.of(recipient);
            }
        }

        return Optional.of(new Address.Mailbox(name, Optional.empty()));
    }

    // --

    @Override
    public boolean shouldBlockJeiInput() {
        // Always block JEI input, as this screen does not have jei showing,
        // but hotkeys such as Ctrl + O will still hide JEI if pressed.
        return true;
    }
}
