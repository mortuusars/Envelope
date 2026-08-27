package io.github.mortuusars.envelope.client.gui.widget.textbox.display;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.client.gui.widget.textbox.TextBox;
import io.github.mortuusars.envelope.client.gui.widget.textbox.text.Formatting;
import io.github.mortuusars.mortaar.util.Pos2i;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class FormattingToolbar {
    public static final ResourceLocation TEXTURE = Envelope.resource("textures/gui/formatting_toolbar.png");

    public static final Map<Character, String> HOTKEYS = new HashMap<>();

    protected final TextBox textBox;
    protected Positioner positioner = Positioner.ABOVE_SELECTION;
    protected int width, height, x, y;
    protected boolean visible = true;
    protected boolean shouldUpdate = true;

    protected final List<FormattingButton> buttons = new ArrayList<>();

    static {
        HOTKEYS.put(Formatting.Format.OBFUSCATED.getChar(), "Ctrl+K");
        HOTKEYS.put(Formatting.Format.BOLD.getChar(), "Ctrl+L");
        HOTKEYS.put(Formatting.Format.STRIKETHROUGH.getChar(), "Ctrl+M");
        HOTKEYS.put(Formatting.Format.UNDERLINE.getChar(), "Ctrl+N");
        HOTKEYS.put(Formatting.Format.ITALIC.getChar(), "Ctrl+O");
        HOTKEYS.put(Formatting.RESET.getChar(), "Ctrl+R");

        HOTKEYS.put(Formatting.Color.BLACK.getChar(), "Ctrl+1");
        HOTKEYS.put(Formatting.Color.DARK_BLUE.getChar(), "Ctrl+2");
        HOTKEYS.put(Formatting.Color.DARK_GREEN.getChar(), "Ctrl+3");
        HOTKEYS.put(Formatting.Color.DARK_AQUA.getChar(), "Ctrl+4");
        HOTKEYS.put(Formatting.Color.DARK_RED.getChar(), "Ctrl+5");
        HOTKEYS.put(Formatting.Color.DARK_PURPLE.getChar(), "Ctrl+6");
        HOTKEYS.put(Formatting.Color.GOLD.getChar(), "Ctrl+7");
        HOTKEYS.put(Formatting.Color.GRAY.getChar(), "Ctrl+8");
        HOTKEYS.put(Formatting.Color.DARK_GRAY.getChar(), "Ctrl+Shift+1");
        HOTKEYS.put(Formatting.Color.BLUE.getChar(), "Ctrl+Shift+2");
        HOTKEYS.put(Formatting.Color.GREEN.getChar(), "Ctrl+Shift+3");
        HOTKEYS.put(Formatting.Color.AQUA.getChar(), "Ctrl+Shift+4");
        HOTKEYS.put(Formatting.Color.RED.getChar(), "Ctrl+Shift+5");
        HOTKEYS.put(Formatting.Color.LIGHT_PURPLE.getChar(), "Ctrl+Shift+6");
        HOTKEYS.put(Formatting.Color.YELLOW.getChar(), "Ctrl+Shift+7");
        HOTKEYS.put(Formatting.Color.WHITE.getChar(), "Ctrl+Shift+8");
    }

    public FormattingToolbar(TextBox textBox) {
        this.textBox = textBox;
        setup();
    }

    protected void setup() {
        buttons.clear();

        int x = 0;

        buttons.add(new FormattingButton(Formatting.Format.BOLD, new Rect2i(x, 0, 12, 15), new Pos2i(x, 0)));
        x += 11;
        buttons.add(new FormattingButton(Formatting.Format.ITALIC, new Rect2i(x, 0, 12, 15), new Pos2i(x, 0)));
        x += 11;
        buttons.add(new FormattingButton(Formatting.Format.UNDERLINE, new Rect2i(x, 0, 12, 15), new Pos2i(x, 0)));
        x += 11;
        buttons.add(new FormattingButton(Formatting.Format.STRIKETHROUGH, new Rect2i(x, 0, 12, 15), new Pos2i(x, 0)));
        x += 11;
        buttons.add(new FormattingButton(Formatting.Format.OBFUSCATED, new Rect2i(x, 0, 12, 15), new Pos2i(x, 0)));
        x += 12;

        buttons.add(new FormattingButton(Formatting.RESET, new Rect2i(x - 8, -13, 19, 28), new Pos2i(x, 0)));

        x += 11;

        for (Formatting.Color color : Arrays.stream(Formatting.Color.values()).limit(8).toList()) {
            buttons.add(new FormattingButton(color, new Rect2i(x, 0, 7, 7), new Pos2i(x + 8, 0)));
            x += 6;
        }

        x += 1;

        for (Formatting.Color color : Arrays.stream(Formatting.Color.values()).skip(8).toList()) {
            buttons.add(new FormattingButton(color, new Rect2i(x - 49, 6, 7, 9), new Pos2i(x + 8, 0)));
            x += 6;
        }

        width = buttons.stream().mapToInt(button -> button.area().getX() + button.area.getWidth()).max().orElse(0);
        height = buttons.stream().mapToInt(button -> button.area().getY() + button.area.getHeight()).max().orElse(0);
    }

    // --

    public TextBox getTextBox() {
        return textBox;
    }

    public Positioner getPositioner() {
        return positioner;
    }

    public FormattingToolbar setPositioner(Positioner positioner) {
        this.positioner = positioner;
        return this;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public boolean isVisible() {
        return visible;
    }

    public FormattingToolbar setVisible(boolean visible) {
        this.visible = visible;
        return this;
    }

    // --

    public boolean shouldUpdate() {
        return shouldUpdate;
    }

    public void scheduleUpdate() {
        shouldUpdate = true;
    }

    public void update() {
        Pos2i pos = positioner.position(this, getTextBox());
        x = pos.x;
        y = pos.y;

        for (FormattingButton button : buttons) {
            if (button.formatting() == Formatting.RESET) continue;
            button.highlighted = getTextBox().getEditor().getSelectedSpan().stream()
                    .allMatch(c -> c.hasFormatting(button.formatting()));
        }

        this.shouldUpdate = false;
    }

    // -- Render

    public boolean shouldShow() {
        return getTextBox().isFocused() && getTextBox().getEditor().isSelecting();
    }

    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        if (!isVisible() || !shouldShow()) return;

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0, 0, 500);

        @Nullable FormattingButton hoveredButton = null;

        for (FormattingButton button : buttons) {
            int vOffset = 0;
            if (button.disabled()) {
                vOffset = button.area.getHeight() * 2;
            } else if (button.highlighted()) {
                vOffset = button.area.getHeight();
            }

            if (hoveredButton == null && button.isHovering(mouseX - x, mouseY - y)) {
                hoveredButton = button;

                if (!button.disabled()) {
                    vOffset = button.area.getHeight();
                }
            }

            guiGraphics.blit(TEXTURE, x + button.area.getX(), y + button.area.getY(),
                    button.uv.x, button.uv.y + vOffset, button.area.getWidth(), button.area.getHeight());
        }

        if (hoveredButton != null) {
            MutableComponent component = Component.translatable(
                            "gui.envelope.formatting." + hoveredButton.formatting.getName())
                    .append(" §8" + HOTKEYS.getOrDefault(hoveredButton.formatting.getChar(), ""));
            guiGraphics.renderTooltip(Minecraft.getInstance().font, component, mouseX, mouseY + 20);
        }

        guiGraphics.pose().popPose();
    }

    // -- Input

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return false;
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (isVisible() && shouldShow() && button == InputConstants.MOUSE_BUTTON_LEFT) {
            for (FormattingButton formattingButton : buttons) {
                if (formattingButton.isHovering((int) (mouseX - x), (int) (mouseY - y))) {
                    getTextBox().getEditor().applyFormatting(Formatting.of(formattingButton.formatting()));
                    Minecraft.getInstance().getSoundManager().play(
                            SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(), 1, 0.3f));
                    return true;
                }
            }
        }

        return false;
    }

    public interface Positioner {
        Pos2i position(FormattingToolbar toolbar, TextBox textBox);

        Positioner ABOVE_SELECTION = (toolbar, textBox) -> {
            int selectionStartY = textBox.getDisplayCache()
                    .getSelection()
                    .stream()
                    .mapToInt(Rect2i::getY)
                    .min()
                    .orElse(0);

            int x = textBox.getX() + HorizontalAlignment.CENTER.align(textBox.getWidth(), toolbar.getWidth());
            int y = textBox.getY() + selectionStartY - textBox.getFont().lineHeight - toolbar.getHeight();
            return new Pos2i(x, y);
        };
    }

    public static class FormattingButton {
        private final Formatting.Type formatting;
        private final Rect2i area;
        private final Pos2i uv;
        private boolean disabled;
        private boolean highlighted;

        public FormattingButton(Formatting.Type formatting, Rect2i area, Pos2i uv) {
            this.formatting = formatting;
            this.area = area;
            this.uv = uv;
        }

        public Formatting.Type formatting() {
            return formatting;
        }

        public Rect2i area() {
            return area;
        }

        public Pos2i uv() {
            return uv;
        }

        public boolean disabled() {
            return disabled;
        }

        public boolean highlighted() {
            return highlighted;
        }

        public boolean isHovering(int mouseX, int mouseY) {
            return (mouseX >= area.getX() && mouseX < area.getX() + area.getWidth())
                    && (mouseY >= area.getY() && mouseY < area.getY() + area.getHeight());
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) return true;
            if (obj == null || obj.getClass() != this.getClass()) return false;
            var that = (FormattingButton) obj;
            return Objects.equals(this.formatting, that.formatting) &&
                    Objects.equals(this.area, that.area) &&
                    Objects.equals(this.uv, that.uv) &&
                    this.disabled == that.disabled &&
                    this.highlighted == that.highlighted;
        }

        @Override
        public int hashCode() {
            return Objects.hash(formatting, area, uv, disabled, highlighted);
        }

        @Override
        public String toString() {
            return "FormattingButton[" +
                    "formatting=" + formatting + ", " +
                    "area=" + area + ", " +
                    "uv=" + uv + ", " +
                    "disabled=" + disabled + ", " +
                    "highlighted=" + highlighted + ']';
        }
    }
}
