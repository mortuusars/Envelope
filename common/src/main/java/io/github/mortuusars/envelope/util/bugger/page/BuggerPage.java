package io.github.mortuusars.envelope.util.bugger.page;

import net.minecraft.client.gui.GuiGraphics;

import java.util.Collections;
import java.util.List;

public interface BuggerPage {
    String getTitle();
    default List<String> getLeftLines() {
        return Collections.emptyList();
    }
    default List<String> getRightLines() {
        return Collections.emptyList();
    }

    default void render(GuiGraphics guiGraphics, float partialTicks, float scale) {

    }

    default boolean onKeyPress(int key, int scanCode, int modifiers) {
        return false;
    }

    default boolean onKeyRepeat(int key, int scanCode, int modifiers) {
        return false;
    }

    default boolean onKeyRelease(int key, int scanCode, int modifiers) {
        return false;
    }

    default boolean onMousePress(int button, int action, int modifiers) {
        return false;
    }

    default boolean onMouseScroll(double amount) {
        return false;
    }
}
