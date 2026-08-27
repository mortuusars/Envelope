package io.github.mortuusars.envelope.client.gui.widget.textbox.display;

import io.github.mortuusars.envelope.client.gui.widget.textbox.text.Char;
import io.github.mortuusars.envelope.client.gui.widget.textbox.text.FormattedString;
import io.github.mortuusars.envelope.client.gui.widget.textbox.text.FormattedStringEditor;
import io.github.mortuusars.mortaar.util.Pos2i;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.Rect2i;

import java.util.ArrayList;
import java.util.List;

public class FormattedStringDisplayCache {
    protected FormattedStringEditor editor;

    protected Font font;
    protected int width = 0;
    protected int height = 0;
    protected HorizontalAlignment alignment;

    protected Pos2i cursor = new Pos2i(0, 0);
    protected ArrayList<Line> lines = new ArrayList<>();
    protected ArrayList<Rect2i> selection = new ArrayList<>();

    boolean shouldUpdate = true;

    public FormattedStringDisplayCache(FormattedStringEditor editor) {
        this.editor = editor;
    }

    // --

    public boolean shouldUpdate() {
        return shouldUpdate;
    }

    public void scheduleUpdate() {
        shouldUpdate = true;
    }

    public void update(Font font, int width, int height, HorizontalAlignment alignment) {
        this.font = font;
        this.width = width;
        this.height = height;
        this.alignment = alignment;

        updateLines();
        updateCursor();
        updateSelectionAreas();

        this.shouldUpdate = false;
    }

    protected void updateLines() {
        lines.clear();
        lines.addAll(splitLines(editor.getText(), font, width, alignment));
    }

    protected void updateCursor() {
        Line cursorLine = this.lines.get(findLineIndexByCharIndex(editor.getCursorPos()));
        int lineCursorIndex = editor.getCursorPos() - cursorLine.firstCharIndex();
        int x = cursorLine.x + cursorLine.widthToIndex(font, lineCursorIndex);
        int y = cursorLine.y;
        cursor = new Pos2i(x, y);

        if (editor.isCursorAtEnd() && cursorLine.isEmpty()) {
            cursor.x -= switch (alignment) {
                case LEFT -> 0;
                case CENTER -> font.width("_") / 2;
                case RIGHT -> font.width("_");
            };
        }
    }

    protected void updateSelectionAreas() {
        selection.clear();

        if (!editor.isSelecting()) return;

        int selectionStartIndex = editor.getSelectionStart();
        int selectionEndIndex = editor.getSelectionEnd();
        int lineAtStart = findLineIndexByCharIndex(selectionStartIndex);
        int lineAtEnd = findLineIndexByCharIndex(selectionEndIndex);


        for (int lineIndex = lineAtStart; lineIndex <= lineAtEnd; lineIndex++) {
            Line line = lines.get(lineIndex);

            if (line.isEmpty() && lineIndex == lineAtEnd) {
                continue;
            }

            int firstCharIndex = line.firstCharIndex();

            int firstLineChar = Math.max(selectionStartIndex - firstCharIndex, 0);
            int lastLineChar = Math.min(selectionEndIndex - 1 - firstCharIndex, line.getString().size() - 1);

            int width = line.width(font, firstLineChar, lastLineChar);

            int height = font.lineHeight;
            int x = line.x + line.widthToIndex(font, firstLineChar);
            int y = line.y;

            if (line.renderedString().isEmpty()) {
                x -= switch (alignment) {
                    case LEFT -> 0;
                    case CENTER -> width / 2;
                    case RIGHT -> width;
                };
            }

            selection.add(new Rect2i(x, y, width, height));
        }
    }

    // --

    public List<Line> getLines() {
        return lines;
    }

    public Line getLine(int line) {
        return lines.get(line);
    }

    public ArrayList<Rect2i> getSelection() {
        return selection;
    }

    public Pos2i getCursor() {
        return cursor;
    }

    public int findLineIndexByCharIndex(int index) {
        for (int i = 0; i < lines.size(); i++) {
            Line line = lines.get(i);
            if (line.firstCharIndex() <= index && index <= line.lastCharIndex()) {
                return i;
            }
        }
        return Math.max(0, lines.size() - 1);
    }

    public int getCharIndexAtPosition(Font font, int x, int y) {
        int lineIndex = y / font.lineHeight;

        if (lineIndex < 0) {
            return 0;
        } else if (lineIndex >= lines.size()) {
            return editor.length();
        }

        Line line = getLine(lineIndex);
        return line.indexAtWidth(font, x - line.x);
    }

    // --

    public List<Line> splitLines(FormattedString string, Font font, int width, HorizontalAlignment alignment) {
        ArrayList<Line> lines = new ArrayList<>();

        if (string.isEmpty()) {
            int x = alignment.align(width, 0);
            int y = 0;
            lines.add(new Line(font, new FormattedString(), 0, 0, "", 0, x, y));
            return lines;
        }

        int i = 0;
        while (i < string.size()) {
            int firstCharIndex = i;
            int lineWidth = 0;
            int lastSpaceIndex = -1;

            while (i < string.size()) {
                Char character = string.get(i);
                int charWidth = character.getWidth(font, true);

                if (character.character() == ' ') {
                    lastSpaceIndex = i;
                }

                if (lineWidth + charWidth > width) {
                    // If space exists, break at last space
                    if (lastSpaceIndex != -1) {
                        i = lastSpaceIndex + 1; // Move to next word after space
                    }
                    break;
                }

                lineWidth += charWidth;
                i++;

                if (character.character() == '\n') {
                    break;
                }
            }

            FormattedString lineString = string.subString(firstCharIndex, i);
            String renderedString = lineString.toString(true);
            int x = alignment.align(width, font.width(renderedString));
            int y = lines.size() * font.lineHeight;

            lines.add(new Line(font, lineString, firstCharIndex, i - 1, renderedString, lineWidth, x, y));
        }

        Line lastLine = lines.getLast();
        if (lastLine.endsWithNewLine()) {
            int x = alignment.align(width, 0);
            int y = lines.size() * font.lineHeight;
            lines.add(new Line(font, new FormattedString(), lastLine.lastCharIndex() + 1, lastLine.lastCharIndex() + 1, "", 0, x, y));
        }

        return lines;
    }
}