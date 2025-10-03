package io.github.mortuusars.envelope.client.gui.screen;

import io.github.mortuusars.envelope.Config;
import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.client.gui.widget.textbox.display.HorizontalAlignment;
import io.github.mortuusars.envelope.mail.Address;
import io.github.mortuusars.envelope.client.gui.Sprites;
import io.github.mortuusars.envelope.client.gui.widget.textbox.TextBox;
import io.github.mortuusars.envelope.client.gui.widget.textbox.text.FormattedString;
import io.github.mortuusars.envelope.client.state.ClientStateManager;
import io.github.mortuusars.envelope.client.state.FillRecipientState;
import io.github.mortuusars.envelope.client.util.Minecrft;
import io.github.mortuusars.envelope.network.Packets;
import io.github.mortuusars.envelope.network.packet.serverbound.LetterEditC2SP;
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

    protected final ItemStack letter;
    protected final InteractionHand hand;

    protected int imageWidth, imageHeight, leftPos, topPos;
    protected TextBox subjectBox;
    protected TextBox messageBox;

    public LetterEditScreen(ItemStack letter, InteractionHand hand) {
        super(Component.empty());
        this.letter = letter;
        this.hand = hand;
    }

    @Override
    public boolean isPauseScreen() {
        return Config.Server.Letter.PAUSE.get();
    }

    @Override
    protected void init() {
        imageWidth = 196;
        imageHeight = 244;
        leftPos = (width - imageWidth) / 2;
        topPos = (height - imageHeight) / 2;

        subjectBox = new TextBox(font, leftPos + 18, topPos + 23, 160, 19)
                .setFontColor(0xFF7B593D)
                .setFontUnfocusedColor(0xFF7B593D)
                .setSelectionColor(0xFF664488)
                .setSelectionUnfocusedColor(0xFF696170)
                .setHintColor(0xFFC2A57F)
                .setHorizontalAlignment(HorizontalAlignment.CENTER)
                .setText(FormattedString.parse(letter.getOrDefault(Envelope.DataComponents.LETTER_SUBJECT, "")));
        addRenderableWidget(subjectBox);

        messageBox = new TextBox(font, leftPos + 18, topPos + 53, 160, 137)
                .setFontColor(0xFF7B593D)
                .setFontUnfocusedColor(0xFF7B593D)
                .setSelectionColor(0xFF664488)
                .setSelectionUnfocusedColor(0xFF696170)
                .setHintColor(0xFFC2A57F)
                .setText(FormattedString.parse(letter.getOrDefault(Envelope.DataComponents.LETTER_MESSAGE, "")));
        addRenderableWidget(messageBox);
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
        // Local

        String subject = subjectBox.getEditor().getText().toString();
        if (!subject.isBlank()) {
            letter.set(Envelope.DataComponents.LETTER_SUBJECT, subject);
        } else {
            letter.remove(Envelope.DataComponents.LETTER_SUBJECT);
        }

        String message = messageBox.getEditor().getText().toString();
        if (!message.isBlank()) {
            letter.set(Envelope.DataComponents.LETTER_MESSAGE, message);
        } else {
            letter.remove(Envelope.DataComponents.LETTER_MESSAGE);
        }

        // Send to server

        int slot = this.hand == InteractionHand.MAIN_HAND ? Minecrft.player().getInventory().selected : Inventory.SLOT_OFFHAND;
        Packets.sendToServer(new LetterEditC2SP(slot, subject, message));
    }

    // --

    @Override
    public boolean shouldBlockJeiInput() {
        // Always block JEI input, as this screen does not have jei showing,
        // but hotkeys such as Ctrl + O will still hide JEI if pressed.
        return true;
    }
}
