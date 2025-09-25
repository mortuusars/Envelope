package io.github.mortuusars.envelope.client.gui.widget.textbox.text;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.mortuusars.envelope.Envelope;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.StringSplitter;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.util.Mth;
import net.minecraft.util.StringUtil;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class FormattedStringEditor {
    protected FormattedString string = new FormattedString();
    protected int cursorPos;
    protected int selectionAnchor;
    protected boolean formattingEnabled = true;
    protected Predicate<String> validator;

    public FormattedStringEditor(Predicate<String> validator) {
        this.validator = validator;
    }

    public FormattedStringEditor(Supplier<Predicate<String>> stringValidator) {
        this.validator = str -> stringValidator.get().test(str);
    }

    public FormattedString getText() {
        return string;
    }

    public void setText(FormattedString string) {
        this.string = string;
    }

    public int getCursorPos() {
        return cursorPos;
    }

    public int getSelectionAnchor() {
        return selectionAnchor;
    }

    public boolean isFormattingEnabled() {
        return formattingEnabled;
    }

    public FormattedStringEditor setFormattingEnabled(boolean formattingEnabled) {
        this.formattingEnabled = formattingEnabled;
        return this;
    }

    public Predicate<String> getValidator() {
        return validator;
    }

    public void setValidator(Predicate<String> validator) {
        this.validator = validator;
    }

    // -- String

    public int length() {
        return getText().size();
    }

    public List<Char> getSpan(int start, int end) {
        return getText().subList(start, end);
    }

    public List<Char> getSelectedSpan() {
        return getSpan(getSelectionStart(), getSelectionEnd());
    }

    // -- Cursor

    public boolean isCursorAtStart() {
        return getCursorPos() == 0;
    }

    public boolean isCursorAtEnd() {
        return getCursorPos() == length();
    }

    public void setCursorPos(int index) {
        setCursorPos(index, true);
    }

    public void setCursorPos(int index, boolean keepSelection) {
        cursorPos = clampIndex(index);
        resetSelectionIfNeeded(keepSelection);
    }

    public void setCursorToStart(boolean keepSelection) {
        setCursorPos(0, keepSelection);
    }

    public void setCursorToEnd(boolean keepSelection) {
        setCursorPos(length(), keepSelection);
    }

    public void moveCursorBy(int direction, boolean keepSelection, CursorStep cursorStep) {
        switch (cursorStep) {
            case CHARACTER -> this.moveCursorByChars(direction, keepSelection);
            case WORD -> this.moveCursorByWords(direction, keepSelection);
        }
    }

    public void moveCursorByChars(int direction, boolean keepSelection) {
        if (!keepSelection && isSelecting()) {
            setCursorPos(direction < 0 ? getSelectionStart() : getSelectionEnd(), false);
        } else {
            setCursorPos(Util.offsetByCodepoints(getText().toStringWithoutFormatting(), getCursorPos(), direction), keepSelection);
        }
    }

    public void moveCursorByWords(int direction, boolean keepSelection) {
        if (!keepSelection && isSelecting()) {
            setCursorPos(direction < 0 ? getSelectionStart() : getSelectionEnd(), false);
        } else {
            setCursorPos(StringSplitter.getWordPosition(getText().toStringWithoutFormatting(), direction, getCursorPos(), true), keepSelection);
        }
    }

    public Formatting getFormattingAtCursor() {
        int charBeforeCursor = getCursorPos() - 1;
        if (charBeforeCursor >= 0 && charBeforeCursor < length()) {
            return getText().get(charBeforeCursor).formatting();
        }
        return Formatting.EMPTY;
    }

    // -- Selection

    public boolean isSelecting() {
        return getCursorPos() != getSelectionAnchor();
    }

    public int getSelectionStart() {
        return Math.min(getCursorPos(), getSelectionAnchor());
    }

    public int getSelectionEnd() {
        return Math.max(getCursorPos(), getSelectionAnchor());
    }

    public void setSelectionAnchor(int index) {
        selectionAnchor = clampIndex(index);
    }

    public void setSelectionRange(int start, int end) {
        setSelectionAnchor(start);
        setCursorPos(end, true);
    }

    public void clearSelection() {
        setSelectionAnchor(getCursorPos());
    }

    public void selectWord(int index) {
        setSelectionRange(
                StringSplitter.getWordPosition(getText().toStringWithoutFormatting(), -1, index, false),
                StringSplitter.getWordPosition(getText().toStringWithoutFormatting(), 1, index, false));
    }

    public String getSelectedString(boolean withFormatting) {
        if (!isSelecting()) return "";
        FormattedString selectedString = getText().subString(getSelectionStart(), getSelectionEnd());
        return withFormatting ? selectedString.toString() : selectedString.toStringWithoutFormatting();
    }

    // --

    private boolean suppressNextCharTyped = false;

    public boolean keyPressed(int key) {
        if (onKeyPressed(key)) {
            suppressNextCharTyped = true;
            return true;
        }
        suppressNextCharTyped = false;
        return false;
    }

    private boolean onKeyPressed(int key) {
        if (Screen.isSelectAll(key)) {
            selectAll();
            return true;
        }
        if (key == InputConstants.KEY_C && Screen.hasControlDown() && !Screen.hasAltDown()) {
            copy(Screen.hasShiftDown());
            return true;
        }
        if (key == InputConstants.KEY_V && Screen.hasControlDown() && !Screen.hasAltDown()) {
            paste(Screen.hasShiftDown());
            return true;
        }
        if (key == InputConstants.KEY_X && Screen.hasControlDown() && !Screen.hasAltDown()) {
            cut(Screen.hasShiftDown());
            return true;
        }
        CursorStep cursorStep = Screen.hasControlDown() ? CursorStep.WORD : CursorStep.CHARACTER;
        if (key == InputConstants.KEY_BACKSPACE) {
            removeFromCursor(-1, cursorStep);
            return true;
        }
        if (key == InputConstants.KEY_DELETE) {
            removeFromCursor(1, cursorStep);
            return true;
        }
        if (key == InputConstants.KEY_LEFT) {
            moveCursorBy(-1, Screen.hasShiftDown(), cursorStep);
            return true;
        }
        if (key == InputConstants.KEY_RIGHT) {
            moveCursorBy(1, Screen.hasShiftDown(), cursorStep);
            return true;
        }
        if (key == InputConstants.KEY_HOME) {
            setCursorToStart(Screen.hasShiftDown());
            return true;
        }
        if (key == InputConstants.KEY_END) {
            setCursorToEnd(Screen.hasShiftDown());
            return true;
        }
        if (key == InputConstants.KEY_RETURN || key == InputConstants.KEY_NUMPADENTER) {
            insertTextAtCursor("\n");
            return true;
        }

        if (isFormattingEnabled()) {
            @Nullable Formatting formatting = handleFormattingKeys(key);
            if (formatting != null) {
                applyFormatting(formatting);
                return true;
            }
        }

        return false;
    }

    protected Formatting handleFormattingKeys(int key) {
        if (Screen.hasControlDown() && !Screen.hasShiftDown() && !Screen.hasAltDown()) {
            return switch (key) {
                case InputConstants.KEY_1 -> Formatting.of(Formatting.Color.BLACK);
                case InputConstants.KEY_2 -> Formatting.of(Formatting.Color.DARK_BLUE);
                case InputConstants.KEY_3 -> Formatting.of(Formatting.Color.DARK_GREEN);
                case InputConstants.KEY_4 -> Formatting.of(Formatting.Color.DARK_AQUA);
                case InputConstants.KEY_5 -> Formatting.of(Formatting.Color.DARK_RED);
                case InputConstants.KEY_6 -> Formatting.of(Formatting.Color.DARK_PURPLE);
                case InputConstants.KEY_7 -> Formatting.of(Formatting.Color.GOLD);
                case InputConstants.KEY_8 -> Formatting.of(Formatting.Color.GRAY);
                // --
                case InputConstants.KEY_K -> Formatting.of(Formatting.Format.OBFUSCATED);
                case InputConstants.KEY_L -> Formatting.of(Formatting.Format.BOLD);
                case InputConstants.KEY_M -> Formatting.of(Formatting.Format.STRIKETHROUGH);
                case InputConstants.KEY_N -> Formatting.of(Formatting.Format.UNDERLINE);
                case InputConstants.KEY_O -> Formatting.of(Formatting.Format.ITALIC);
                case InputConstants.KEY_R -> Formatting.EMPTY;
                default -> null;
            };
        }

        if (Screen.hasControlDown() && Screen.hasShiftDown() && !Screen.hasAltDown()) {
            return switch (key) {
                case InputConstants.KEY_1 -> Formatting.of(Formatting.Color.DARK_GRAY);
                case InputConstants.KEY_2 -> Formatting.of(Formatting.Color.BLUE);
                case InputConstants.KEY_3 -> Formatting.of(Formatting.Color.GREEN);
                case InputConstants.KEY_4 -> Formatting.of(Formatting.Color.AQUA);
                case InputConstants.KEY_5 -> Formatting.of(Formatting.Color.RED);
                case InputConstants.KEY_6 -> Formatting.of(Formatting.Color.LIGHT_PURPLE);
                case InputConstants.KEY_7 -> Formatting.of(Formatting.Color.YELLOW);
                case InputConstants.KEY_8 -> Formatting.of(Formatting.Color.WHITE);
                default -> null;
            };
        }

        return null;
    }

    public void applyFormatting(Formatting formatting) {
        // Prevents applying bold formatting if the text would overflow available space due to extra width:
        if (formatting.format().contains(Formatting.Format.BOLD)) {
            FormattedString string = new FormattedString(getText().stream().map(c -> new Char(c.character(), c.formatting().copy())).toList());
            List<Char> chars = string.subList(getSelectionStart(), getSelectionEnd());
            chars.replaceAll(c -> c.flipFormatting(formatting));

            if (!validator.test(string.toString())) {
                return;
            }
        }

        if (isSelecting()) {
            getSelectedSpan().replaceAll(c -> c.flipFormatting(formatting));
        } else if (formatting == Formatting.EMPTY) {
            getText().replaceAll(c -> c.withFormatting(Formatting.EMPTY));
        }
    }

    public boolean charTyped(char character) {
        return !suppressNextCharTyped
                && StringUtil.isAllowedChatCharacter(character)
                && insertTextAtCursor(Character.toString(character));
    }

    public void cut(boolean withFormatting) {
        copy(withFormatting);
        removeSelectedText();
    }

    public void paste(boolean keepFormatting) {
        try {
            String text = Minecraft.getInstance().keyboardHandler.getClipboard();
            if (!keepFormatting) {
                text = ChatFormatting.stripFormatting(text);
            }
            insertTextAtCursor(Objects.requireNonNull(text).replaceAll("\\r", ""));
        } catch (Exception e) {
            Envelope.LOGGER.error("Text Paste error: ", e);
        }
    }

    public void copy(boolean withFormatting) {
        Minecraft.getInstance().keyboardHandler.setClipboard(getSelectedString(withFormatting));
    }

    public void selectAll() {
        setSelectionAnchor(0);
        setCursorPos(length(), true);
    }

    // --

    public boolean insertTextAtCursor(String text) {
        if (isSelecting()) {
            removeSelectedText();
        }

        FormattedString insertedString = FormattedString.parse(text);
        insertedString.replaceAll(c -> {
            if (!c.hasFormatting()) {
                return c.withFormatting(getFormattingAtCursor());
            }
            return c;
        });

        FormattedString newString = new FormattedString(getText());
        newString.addAll(getCursorPos(), insertedString);

        String string = newString.toStringWithoutFormatting();
        if (validator.test(string)) {
            this.string = newString;
            setCursorPos(getCursorPos() + insertedString.length(), false);
            return true;
        }

        return false;
    }

    public void removeFromCursor(int direction, CursorStep step) {
        switch (step) {
            case CHARACTER: {
                this.removeCharsFromCursor(direction);
                break;
            }
            case WORD: {
                this.removeWordsFromCursor(direction);
            }
        }
    }

    public void removeWordsFromCursor(int direction) {
        int charsCount = StringSplitter.getWordPosition(getText().toStringWithoutFormatting(), direction, this.cursorPos, true);
        this.removeCharsFromCursor(charsCount - this.cursorPos);
    }

    public void removeCharsFromCursor(int direction) {
        String string = getText().toStringWithoutFormatting();
        if (!string.isEmpty()) {
            if (isSelecting()) {
                removeSelectedText();
            } else {
                int cursor = getCursorPos();
                int removePos = Util.offsetByCodepoints(string, cursor, direction);
                int start = Math.min(removePos, cursor);
                int end = Math.max(removePos, cursor);

                getSpan(start, end).clear();
                setCursorPos(start, false);
            }
        }
    }

    public void removeSelectedText() {
        if (!isSelecting()) return;

        int start = getSelectionStart();
        int end = getSelectionEnd();

        getSpan(start, end).clear();
        setCursorPos(start, false);
    }

    // --

    protected void resetSelectionIfNeeded(boolean keepSelection) {
        if (!keepSelection) {
            setSelectionAnchor(getCursorPos());
        }
    }

    protected int clampIndex(int index) {
        return Mth.clamp(index, 0, length());
    }

    public enum CursorStep {
        CHARACTER,
        WORD;
    }

    public interface Validator {
        static Predicate<String> fitInDimensions(Font font, int width, int height) {
            return string -> string.length() < 1024 && font.wordWrapHeight(string, width) + (string.endsWith("\n") ? font.lineHeight : 0) <= height;
        }
    }
}
