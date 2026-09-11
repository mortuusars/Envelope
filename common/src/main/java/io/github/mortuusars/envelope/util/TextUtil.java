package io.github.mortuusars.envelope.util;

import net.minecraft.client.StringSplitter;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;

public class TextUtil {
    public static FormattedText truncateStringToWidth(Font font, FormattedText text, int width) {
        if (font.width(text) <= width) {
            return text;
        }

        int ellipsisWidth = font.width("...");
        StringSplitter splitter = font.getSplitter();

        FormattedText truncatedText = font.substrByWidth(text, width - ellipsisWidth);

        Style style = splitter.componentStyleAtWidth(text, width - ellipsisWidth);
        if (style == null) {
            style = Style.EMPTY;
        }

        return FormattedText.composite(truncatedText, Component.literal("...").setStyle(style));
    }
}
