package io.github.mortuusars.envelope.client.gui.widget.textbox.text;

import net.minecraft.client.gui.Font;

public record Char(char character, Formatting formatting) {
    public static final Char EMPTY = new Char('\u0000', Formatting.EMPTY);

    public Char(char c) {
        this(c, Formatting.EMPTY);
    }

    public Char(int c) {
        this((char) c);
    }

    public boolean hasFormatting() {
        return !formatting.isEmpty();
    }

    public boolean hasFormatting(Formatting.Type type) {
        if (type.isColor()) return type.equals(formatting.color());
        else if (type.isFormat()) return formatting.format().contains(((Formatting.Format) type));
        return false;
    }

    public Char withFormatting(Formatting.Color color) {
        return new Char(character, formatting.with(color));
    }

    public Char withFormatting(Formatting.Format format) {
        return new Char(character, formatting.with(format));
    }

    public Char withFormatting(Formatting formatting) {
        return new Char(character, formatting);
    }

    public Char flipFormatting(Formatting formatting) {
        return new Char(character, formatting().flip(formatting));
    }

    public int getWidth(Font font, boolean ignoreNewLine) {
        return ignoreNewLine && (character == '\n') ? 0 : font.width(toStringWithFormatting());
    }

    public String toStringWithFormatting() {
        if (!hasFormatting()) {
            return Character.toString(character);
        }

        StringBuilder sb = new StringBuilder();
        formatting.append(sb);
        sb.append(character);
        if (hasFormatting()) {
            Formatting.RESET.append(sb);
        }
        return sb.toString();
    }
}