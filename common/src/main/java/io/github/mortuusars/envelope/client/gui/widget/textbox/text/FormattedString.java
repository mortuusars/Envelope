package io.github.mortuusars.envelope.client.gui.widget.textbox.text;

import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;

public class FormattedString extends ArrayList<Char> implements CharSequence, FormattedText {
    public FormattedString() {
        super();
    }

    public FormattedString(@NotNull Collection<? extends Char> chars) {
        super(chars);
    }

    public static FormattedString parse(String string) {
        return new FormattedString(parseChars(string));
    }

    public static FormattedString parseWithoutFormatting(String string) {
        return new FormattedString(string.chars().mapToObj(Char::new).toList());
    }

    public static ArrayList<Char> parseChars(String string) {
        ArrayList<Char> chars = new ArrayList<>();

        Formatting formatting = Formatting.EMPTY;

        boolean grabbingFormattingChar = false;

        for (int i = 0; i < string.length(); i++) {
            char c = string.charAt(i);

            if (c == Formatting.SECTION_SIGN) {
                grabbingFormattingChar = true;
                continue;
            }

            if (grabbingFormattingChar) {
                formatting = formatting.with(c);
                grabbingFormattingChar = false;
                continue;
            }

            chars.add(new Char(c, formatting));
        }

        return chars;
    }

    public FormattedString subString(int start, int end) {
        return new FormattedString(subList(start, end));
    }

//    public @Nullable Char getCharBefore(int index) {
//        if (isEmpty()) return null;
//        if (index < 0) return null;
//        if (index >= length()) return null;
//        return
//    }

    // -- CharSequence

    @Override
    public int length() {
        return size();
    }

    @Override
    public char charAt(int index) {
        return get(index).character();
    }

    @NotNull
    @Override
    public CharSequence subSequence(int start, int end) {
        return subString(start, end);
    }

    // -- FormattedText

    @Override
    public <T> @NotNull Optional<T> visit(ContentConsumer<T> acceptor) {
        return acceptor.accept(toStringWithoutFormatting());
    }

    @Override
    public <T> @NotNull Optional<T> visit(StyledContentConsumer<T> acceptor, Style style) {
        return acceptor.accept(style, toString());
    }

    // --

    @Override
    public @NotNull String toString() {
        return toString(false);
    }

    public @NotNull String toString(boolean skipNewLines) {
        if (isEmpty()) return "";

        StringBuilder sb = new StringBuilder();

        Char previousChar = Char.EMPTY;

        for (Char character : this) {
            if (skipNewLines && character.character() == '\n') continue;

            if (!character.formatting().equals(previousChar.formatting())) {
                if (previousChar.hasFormatting()) {
                    Formatting.RESET.append(sb);
                }
                character.formatting().append(sb);
            }

            sb.append(character.character());
            previousChar = character;
        }

        Char lastChar = get(size() - 1);
        if (lastChar.hasFormatting()) {
            Formatting.RESET.append(sb);
        }

        return sb.toString();
    }

    public @NotNull String toStringWithoutFormatting() {
        return toStringWithoutFormatting(false);
    }

    public @NotNull String toStringWithoutFormatting(boolean skipNewLines) {
        if (isEmpty()) return "";

        StringBuilder sb = new StringBuilder();

        for (Char character : this) {
            if (skipNewLines && character.character() == '\n') continue;
            sb.append(character.character());
        }

        return sb.toString();
    }
}