package io.github.mortuusars.envelope.client.gui.screen;

import io.github.mortuusars.envelope.Config;
import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.client.gui.widget.textbox.TextBox;
import io.github.mortuusars.envelope.client.gui.widget.textbox.text.FormattedString;
import io.github.mortuusars.mortaar.client.Minecrft;
import io.github.mortuusars.envelope.integration.jei.JeiCompatibleScreen;
import io.github.mortuusars.envelope.network.packet.serverbound.ServerboundLetterEditPacket;
import io.github.mortuusars.envelope.util.ItemAndStack;
import io.github.mortuusars.envelope.world.item.LetterAndQuillItem;
import io.github.mortuusars.mortaar.client.gui.Sprites;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public class LetterEditScreen extends Screen implements JeiCompatibleScreen {
    public static final ResourceLocation TEXTURE = Envelope.resource("textures/gui/letter_and_quill.png");
    public static final WidgetSprites FOLD_BUTTON_SPRITES = Sprites.threeStates(Envelope.resource("letter_and_quill/fold_button"));

    protected final ItemAndStack<LetterAndQuillItem> letter;
    protected final InteractionHand hand;

    protected int imageWidth, imageHeight, leftPos, topPos;
    protected TextBox textBox;
    protected ImageButton foldButton;

    public LetterEditScreen(ItemStack letter, InteractionHand hand) {
        super(Component.empty());
        this.letter = new ItemAndStack<>(letter);
        this.hand = hand;
    }

    @Override
    public boolean isPauseScreen() {
        return Config.Server.LETTER_PAUSE.get();
    }

    @Override
    protected void init() {
        imageWidth = 176;
        imageHeight = 192;
        leftPos = (width - imageWidth) / 2;
        topPos = (height - imageHeight) / 2;

        textBox = addRenderableWidget(new TextBox(font, leftPos + 17, topPos + 21, 142, 144)
              .setFontColor(0xFF7B593D)
              .setFontUnfocusedColor(0xFF7B593D)
              .setSelectionColor(0xFF664488)
              .setSelectionUnfocusedColor(0xFF696170)
              .setHintColor(0xFFC2A57F)
              .setText(FormattedString.parse(letter.map(LetterAndQuillItem::getContent).text())));

        foldButton = addRenderableWidget(new ImageButton(
              leftPos + imageWidth + 5, topPos + 144,
              18, 18,
              FOLD_BUTTON_SPRITES,
              btn -> {
                  saveChanges(true);
                  Minecrft.get().setScreen(null);
              },
              Component.translatable("envelope.letter_and_quill.fold_button")));
        foldButton.setTooltip(Tooltip.create(Component.translatable("envelope.letter_and_quill.fold_button")
              .append(CommonComponents.NEW_LINE)
              .append(Component.translatable("envelope.letter_and_quill.fold_warning").withStyle(ChatFormatting.GRAY))));

        setInitialFocus(textBox);
    }

    @Override
    public void resize(Minecraft minecraft, int width, int height) {
        // Prevent contents reset when window is resized
        FormattedString message = textBox.getEditor().getText();
        int messageCursorPos = textBox.getEditor().getCursorPos();
        super.resize(minecraft, width, height);
        textBox.getEditor().setText(message);
        textBox.getEditor().setCursorPos(messageCursorPos, false);
    }

    // -- Render

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderTransparentBackground(guiGraphics);
        guiGraphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
    }

    // -- Input

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Special handling for toolbar, without this clicks will go to another element under the cursor,
        // if toolbar is over that element.
        if (getFocused() instanceof TextBox box && box.formattingToolbarMouseClicked(mouseX, mouseY, button)) {
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
        if (lastFocused != null && !lastFocused.equals(getFocused()) && lastFocused instanceof TextBox box) {
            box.getEditor().clearSelection();
            box.getDisplayCache().scheduleUpdate();
        }
    }

    // --

    @Override
    public void onClose() {
        super.onClose();
        saveChanges(false);
    }

    protected void saveChanges(boolean fold) {
        String text = textBox.getEditor().getText().toString();

        int slot = this.hand == InteractionHand.MAIN_HAND ? Minecrft.player().getInventory().selected : Inventory.SLOT_OFFHAND;
        new ServerboundLetterEditPacket(slot, text, fold).sendToServer();
    }

    // --

    @Override
    public boolean shouldBlockJeiInput() {
        // Always block JEI input, as this screen does not have jei showing,
        // but hotkeys such as Ctrl + O will still hide JEI if pressed.
        return true;
    }
}
