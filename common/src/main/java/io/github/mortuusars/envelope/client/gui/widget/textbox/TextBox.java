package io.github.mortuusars.envelope.client.gui.widget.textbox;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import io.github.mortuusars.envelope.client.gui.widget.textbox.display.FormattedStringDisplayCache;
import io.github.mortuusars.envelope.client.gui.widget.textbox.display.FormattingToolbar;
import io.github.mortuusars.envelope.client.gui.widget.textbox.display.HorizontalAlignment;
import io.github.mortuusars.envelope.client.gui.widget.textbox.display.Line;
import io.github.mortuusars.envelope.client.gui.widget.textbox.text.FormattedString;
import io.github.mortuusars.envelope.client.gui.widget.textbox.text.FormattedStringEditor;
import io.github.mortuusars.envelope.client.gui.widget.textbox.text.Formatting;
import io.github.mortuusars.envelope.client.util.Pos2i;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class TextBox extends AbstractWidget {
    protected final Font font;

    protected FormattedStringEditor editor = new FormattedStringEditor(() ->
            FormattedStringEditor.Validator.fitInDimensions(getFont(), getWidth(), getHeight()));
    protected FormattedStringDisplayCache displayCache = new FormattedStringDisplayCache(editor);

    protected HorizontalAlignment horizontalAlignment = HorizontalAlignment.LEFT;
    protected int fontColor = 0xFF000000;
    protected int fontUnfocusedColor = 0xFF000000;
    protected int selectionColor = 0xFF0000FF;
    protected int selectionUnfocusedColor = 0x880000FF;
    protected Consumer<FormattedString> onTextChanged = text -> { };

    protected FormattingToolbar formattingToolbar = new FormattingToolbar(this);

    protected Pos2i lastClickPos = new Pos2i(0, 0);
    protected long lastActionTime;
    protected boolean canDrag;

    public TextBox(int x, int y, int width, int height) {
        this(Minecraft.getInstance().font, x, y, width, height);
    }

    public TextBox(Font font, int x, int y, int width, int height) {
        super(x, y, width, height, Component.empty());
        this.font = font;
    }

    public Font getFont() {
        return font;
    }

    public FormattedStringEditor getEditor() {
        return editor;
    }

    public FormattedStringDisplayCache getDisplayCache() {
        if (displayCache.shouldUpdate()) {
            displayCache.update(getFont(), getWidth(), getHeight(), getHorizontalAlignment());
        }

        return displayCache;
    }

    public FormattingToolbar getFormattingToolbar() {
        if (formattingToolbar.shouldUpdate()) {
            formattingToolbar.update();
        }
        return formattingToolbar;
    }

    public TextBox setFormattingToolbar(FormattingToolbar formattingToolbar) {
        this.formattingToolbar = formattingToolbar;
        this.formattingToolbar.scheduleUpdate();
        return this;
    }

    public HorizontalAlignment getHorizontalAlignment() {
        return horizontalAlignment;
    }

    public TextBox setHorizontalAlignment(HorizontalAlignment horizontalAlignment) {
        this.horizontalAlignment = horizontalAlignment;
        refreshDisplayCache();
        return this;
    }

    public int getFontColor() {
        return fontColor;
    }

    public TextBox setFontColor(int fontColor) {
        this.fontColor = fontColor;
        refreshDisplayCache();
        return this;
    }

    public int getFontUnfocusedColor() {
        return fontUnfocusedColor;
    }

    public TextBox setFontUnfocusedColor(int fontUnfocusedColor) {
        this.fontUnfocusedColor = fontUnfocusedColor;
        refreshDisplayCache();
        return this;
    }

    public int getSelectionColor() {
        return selectionColor;
    }

    public TextBox setSelectionColor(int selectionColor) {
        this.selectionColor = selectionColor;
        refreshDisplayCache();
        return this;
    }

    public int getSelectionUnfocusedColor() {
        return selectionUnfocusedColor;
    }

    public TextBox setSelectionUnfocusedColor(int selectionUnfocusedColor) {
        this.selectionUnfocusedColor = selectionUnfocusedColor;
        refreshDisplayCache();
        return this;
    }

    public int getCurrentFontColor() {
        return isFocused() ? fontColor : fontUnfocusedColor;
    }

    public int getCurrentSelectionColor() {
        return isFocused() ? selectionColor : selectionUnfocusedColor;
    }

    public TextBox setTextValidator(Predicate<String> validator) {
        getEditor().setValidator(validator);
        return this;
    }

    public Consumer<FormattedString> onTextChanged() {
        return onTextChanged;
    }

    public TextBox setOnTextChanged(Consumer<FormattedString> onTextChanged) {
        this.onTextChanged = onTextChanged;
        return this;
    }

    public TextBox setText(FormattedString text) {
        getEditor().setString(text);
        return this;
    }

    // -- Render

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        FormattedStringDisplayCache displayCache = getDisplayCache();

        renderLines(guiGraphics, mouseX, mouseY, partialTick, displayCache.getLines(), getCurrentFontColor());

        int cursorColor = getCurrentFontColor();
        Formatting currentFormatting = getEditor().getFormattingAtCursor();
        if (currentFormatting.color() != null) {
            //noinspection DataFlowIssue
            cursorColor = currentFormatting.color().asChatFormatting().getColor() | 0xFF000000;
        }

        renderCursor(guiGraphics, mouseX, mouseY, partialTick, getEditor(), displayCache.getCursor(), cursorColor);
        renderSelection(guiGraphics, mouseX, mouseY, partialTick, displayCache.getSelection(), getCurrentSelectionColor());

        getFormattingToolbar().render(guiGraphics, mouseX, mouseY, partialTick);
    }

    public void renderLines(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, List<Line> lines, int color) {
        for (Line line : lines) {
            guiGraphics.drawString(font, line.renderedString(), getX() + line.x(), getY() + line.y(), color, false);
        }
    }

    public void renderSelection(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, List<Rect2i> selection, int color) {
        for (Rect2i rect : selection) {
            int x0 = getX() + rect.getX();
            int y0 = getY() + rect.getY();
            int x1 = x0 + rect.getWidth();
            int y1 = y0 + rect.getHeight();
            guiGraphics.fill(RenderType.guiTextHighlight(), x0, y0, x1, y1, color);
        }
    }

    public void renderCursor(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, FormattedStringEditor editor, Pos2i cursor, int color) {
        if (!isFocused()) return;
        if (editor.isSelecting()) return;
        if (System.currentTimeMillis() - lastActionTime > 200 && (System.currentTimeMillis() - lastActionTime) % 600 > 300) // Blinking
            return;

        if (editor.isCursorAtEnd()) {
            Line line = getDisplayCache().getLine(getDisplayCache().getLines().size() - 1);
            if (cursor.y + font.lineHeight > getHeight()) {
                int x = line.x() + line.width();
                int y = line.y();
                guiGraphics.drawString(getFont(), "<", getX() + x, getY() + y,
                        color, false);
            } else {
                guiGraphics.drawString(getFont(), "_", getX() + cursor.x, getY() + cursor.y,
                        color, false);
            }
        } else {
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(0, 0, 50);
            RenderSystem.disableBlend();
            guiGraphics.fill(
                    getX() + cursor.x,
                    getY() + cursor.y - 1,
                    getX() + cursor.x + 1,
                    getY() + cursor.y + font.lineHeight,
                    color);
            guiGraphics.pose().popPose();
        }
    }

    protected void refreshDisplayCache() {
        displayCache.scheduleUpdate();
        formattingToolbar.scheduleUpdate();
    }

    // -- Input

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!isFocused() || !isActive() || !visible) return false;

        if (handleKeyPressed(keyCode, scanCode, modifiers)) {
            lastActionTime = System.currentTimeMillis();
            onTextChanged().accept(getEditor().getString());
            refreshDisplayCache();
            return true;
        }

        return false;
    }

    protected boolean handleKeyPressed(int keyCode, int scanCode, int modifiers) {
        if (getFormattingToolbar().keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        } else if (keyCode == InputConstants.KEY_UP) {
            changeLine(-1);
            return true;
        } else if (keyCode == InputConstants.KEY_DOWN) {
            changeLine(1);
            return true;
        } else if (keyCode == InputConstants.KEY_HOME) {
            keyHome();
            return true;
        } else if (keyCode == InputConstants.KEY_END) {
            keyEnd();
            return true;
        }

        return getEditor().keyPressed(keyCode);
    }

    public boolean charTyped(char codePoint, int modifiers) {
        if (isFocused() && getEditor().charTyped(codePoint)) {
            lastActionTime = System.currentTimeMillis();
            onTextChanged().accept(getEditor().getString());
            refreshDisplayCache();
            return true;
        }

        return false;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!isActive() || !visible) return false;

        if (formattingToolbarMouseClicked(mouseX, mouseY, button)) {
            return true;
        }

        if (isHovered && button == InputConstants.MOUSE_BUTTON_LEFT) {
            long currentTime = System.currentTimeMillis();
            FormattedStringDisplayCache display = getDisplayCache();

            int indexAtMousePos = display.getCharIndexAtPosition(font, (int) (mouseX - getX()), (int) (mouseY - getY()));

            if (Math.abs(lastClickPos.x - (int) mouseX) < 4 && Math.abs(lastClickPos.y - (int) mouseY) < 4 && currentTime - lastActionTime < 250L) {
                if (!getEditor().isSelecting()) {
                    getEditor().selectWord(indexAtMousePos);
                } else {
                    getEditor().selectAll();
                }
            } else {
                getEditor().setCursorPos(indexAtMousePos, Screen.hasShiftDown());
            }

            refreshDisplayCache();

            lastClickPos = new Pos2i((int) mouseX, (int) mouseY);
            lastActionTime = currentTime;
            canDrag = true;
            return true;
        }

        return false;
    }

    public boolean formattingToolbarMouseClicked(double mouseX, double mouseY, int button) {
        if (getFormattingToolbar().mouseClicked(mouseX, mouseY, button)) {
            refreshDisplayCache();
            onTextChanged().accept(getEditor().getString());
            canDrag = false;
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (button == 0 && canDrag) {
            FormattedStringDisplayCache display = getDisplayCache();
            int indexAtMousePos = display.getCharIndexAtPosition(font, (int) (mouseX - getX()), (int) (mouseY - getY()));
            getEditor().setCursorPos(indexAtMousePos, true);
            refreshDisplayCache();
            return true;
        }
        return false;
    }

    // --

    public void changeLine(int yChange) {
        Pos2i cursor = getDisplayCache().getCursor();
        int x = cursor.x;
        int y = cursor.y + (font.lineHeight * yChange);
        int index = getDisplayCache().getCharIndexAtPosition(font, x, y);

        getEditor().setCursorPos(index, Screen.hasShiftDown());
    }

    public void keyHome() {
        if (Screen.hasControlDown()) {
            getEditor().setCursorToStart(Screen.hasShiftDown());
        } else {
            int cursorPos = getEditor().getCursorPos();
            int lineIndex = getDisplayCache().findLineIndexByCharIndex(cursorPos);
            Line line = getDisplayCache().getLine(lineIndex);
            getEditor().setCursorPos(line.firstCharIndex(), Screen.hasShiftDown());
        }
    }

    public void keyEnd() {
        if (Screen.hasControlDown()) {
            getEditor().setCursorToEnd(Screen.hasShiftDown());
        } else {
            int cursorPos = getEditor().getCursorPos();
            int lineIndex = getDisplayCache().findLineIndexByCharIndex(cursorPos);
            Line line = getDisplayCache().getLine(lineIndex);
            getEditor().setCursorPos(line.lastCharIndex() + 1, Screen.hasShiftDown());
        }
    }

    // --

    @Override
    public @NotNull Component getMessage() {
        return Component.literal(getEditor().getString().toStringWithoutFormatting());
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
        narrationElementOutput.add(NarratedElementType.TITLE, createNarrationMessage());
    }
}
