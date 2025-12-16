package io.github.mortuusars.envelope.client.gui.tooltip;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.renderer.MultiBufferSource;
import org.joml.Matrix4f;

import java.util.List;

public class CompositeTooltipComponent implements ClientTooltipComponent {
    private final List<ClientTooltipComponent> components;

    public CompositeTooltipComponent(List<ClientTooltipComponent> components) {
        this.components = components;
    }

    @Override
    public int getWidth(Font font) {
        int width = 0;
        for (ClientTooltipComponent component : components) {
            width = Math.max(component.getWidth(font), width);
        }
        return width;
    }

    @Override
    public int getHeight() {
        int height = 0;
        for (ClientTooltipComponent component : components) {
            height += component.getHeight();
        }
        return height;
    }

    @Override
    public void renderImage(Font font, int x, int y, GuiGraphics guiGraphics) {
        for (ClientTooltipComponent component : components) {
            component.renderImage(font, x, y, guiGraphics);
            y += component.getHeight();
        }
    }

    @Override
    public void renderText(Font font, int mouseX, int mouseY, Matrix4f matrix, MultiBufferSource.BufferSource bufferSource) {
        for (ClientTooltipComponent component : components) {
            component.renderText(font, mouseX, mouseY, matrix, bufferSource);
            mouseY += component.getHeight();
        }
    }
}
