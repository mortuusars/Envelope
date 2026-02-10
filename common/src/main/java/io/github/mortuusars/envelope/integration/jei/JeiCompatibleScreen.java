package io.github.mortuusars.envelope.integration.jei;

import io.github.mortuusars.envelope.client.gui.widget.textbox.TextBox;
import net.minecraft.client.gui.screens.Screen;

public interface JeiCompatibleScreen {
    default boolean shouldBlockJeiInput() {
        return this instanceof Screen screen && screen.getFocused() instanceof TextBox textBox && textBox.getEditor().isSelecting();
    }
}
