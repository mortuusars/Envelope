package io.github.mortuusars.envelope.client.gui.screen;

import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.api.mail.Recipient;
import io.github.mortuusars.envelope.client.gui.widget.textbox.TextBox;
import io.github.mortuusars.envelope.client.gui.widget.textbox.display.HorizontalAlignment;
import io.github.mortuusars.envelope.client.gui.widget.textbox.text.FormattedString;
import io.github.mortuusars.envelope.client.util.Minecrft;
import io.github.mortuusars.envelope.network.Packets;
import io.github.mortuusars.envelope.network.packet.serverbound.EditLetterPacketC2SP;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public class LetterEditScreen extends Screen implements JeiKeyConflictResolverScreen {
    public static final ResourceLocation TEXTURE = Envelope.resource("textures/gui/letter.png");

    protected final ItemStack letter;
    protected final InteractionHand hand;
    protected final List<Recipient> knownRecipients;

    protected int imageWidth, imageHeight, leftPos, topPos;
    protected TextBox toBox;
    protected TextBox subjectBox;
    protected TextBox messageBox;

    public LetterEditScreen(ItemStack letter, InteractionHand hand, List<Recipient> knownRecipients) {
        super(Component.empty());
        this.letter = letter;
        this.hand = hand;
        this.knownRecipients = knownRecipients;
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

//        toBox = new EditBox(font,  leftPos + 30, topPos + 20, 140, 9, Component.empty());
//        toBox.setBordered(false);
//        toBox.setTextColor(0xFF886447);
//        @Nullable Recipient recipient = letter.get(Envelope.DataComponents.RECIPIENT);
//        if (recipient != null) {
//            toBox.setValue(recipient.name());
//        }
//        addRenderableWidget(toBox);

        @Nullable Recipient recipient = letter.get(Envelope.DataComponents.RECIPIENT);
        String recipientText = "";
        if (recipient != null) {
            recipientText = recipient.name();
        }
        toBox = new TextBox(font, leftPos + 30, topPos + 20, 140, 9)
                .setFontColor(0xFF7B593D)
                .setFontUnfocusedColor(0xFF7B593D)
                .setSelectionColor(0xFF664488)
                .setSelectionUnfocusedColor(0xFF696170)
                .setHorizontalAlignment(HorizontalAlignment.CENTER)
                .setText(FormattedString.parse(recipientText));
        addRenderableWidget(toBox);

        subjectBox = new TextBox(font, leftPos + 20, topPos + 44, 160, 19)
                .setFontColor(0xFF7B593D)
                .setFontUnfocusedColor(0xFF7B593D)
                .setSelectionColor(0xFF664488)
                .setSelectionUnfocusedColor(0xFF696170)
                .setHorizontalAlignment(HorizontalAlignment.CENTER)
                .setText(FormattedString.parse(letter.getOrDefault(Envelope.DataComponents.LETTER_SUBJECT, "")));
        addRenderableWidget(subjectBox);

        messageBox = new TextBox(font, leftPos + 20, topPos + 77, 160, 137)
                .setFontColor(0xFF7B593D)
                .setFontUnfocusedColor(0xFF7B593D)
                .setSelectionColor(0xFF664488)
                .setSelectionUnfocusedColor(0xFF696170)
                .setHorizontalAlignment(HorizontalAlignment.CENTER)
                .setText(FormattedString.parse(letter.getOrDefault(Envelope.DataComponents.LETTER_MESSAGE, "")));
        addRenderableWidget(messageBox);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        guiGraphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderLabels(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderTransparentBackground(guiGraphics);
        guiGraphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
    }

    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        Component to = Component.translatable("gui.envelope.letter.to");
        guiGraphics.drawString(font, to, leftPos + 100 - (font.width(to) / 2), topPos + 10, 0xFFCFAF88, false);

        Component subject = Component.translatable("gui.envelope.letter.subject");
        guiGraphics.drawString(font, subject, leftPos + 100 - (font.width(subject) / 2), topPos + 33, 0xFFCFAF88, false);

        Component message = Component.translatable("gui.envelope.letter.message");
        guiGraphics.drawString(font, message, leftPos + 100 - (font.width(message) / 2), topPos + 66, 0xFFCFAF88, false);
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
        GuiEventListener lastFocused = getFocused();
        super.setFocused(focused);
        if (lastFocused instanceof TextBox textBox) {
            textBox.getEditor().setSelectionAnchor(textBox.getEditor().getCursorPos());
            textBox.getDisplayCache().scheduleUpdate();
        }
    }

    // --

    protected void saveChanges() {
        Optional<Recipient> recipient = getOrCreateRecipient(toBox.getEditor().getString().toStringWithoutFormatting());

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

    protected Optional<Recipient> getOrCreateRecipient(String name) {
        if (name.isBlank()) {
            return Optional.empty();
        }

        name = name.trim();
        for (Recipient recipient : knownRecipients) {
            if (recipient.name().equalsIgnoreCase(name)) {
                return Optional.of(recipient);
            }
        }

        return Optional.of(new Recipient(name, Optional.empty(), Recipient.Type.UNKNOWN));
    }

    // --

    @Override
    public boolean shouldBlockJeiInput() {
        // Always block JEI input, as this screen does not have jei showing,
        // but hotkeys such as Ctrl + O will still hide JEI if pressed.
        return true;
    }
}
