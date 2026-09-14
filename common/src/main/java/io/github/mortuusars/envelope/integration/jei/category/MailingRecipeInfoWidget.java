package io.github.mortuusars.envelope.integration.jei.category;

import com.mojang.logging.LogUtils;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.widgets.IRecipeWidget;
import mezz.jei.common.util.ImmutableRect2i;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.ScreenPosition;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

public class MailingRecipeInfoWidget implements IRecipeWidget {
    private final IDrawable icon;
    private final Component tooltip;
    private final Component info;
    private final ImmutableRect2i area;

    public MailingRecipeInfoWidget(IDrawable icon, int x, int y, Component info) {
        this.icon = icon;
        this.tooltip = Component.translatable("envelope.jei.info")
              .append(CommonComponents.NEW_LINE)
              .append(info.copy().withStyle(ChatFormatting.GRAY));
        this.info = info;
        this.area = new ImmutableRect2i(x, y, icon.getWidth(), icon.getHeight());
    }

    @Override
    public @NotNull ScreenPosition getPosition() {
        return area.getScreenPosition();
    }

    @SuppressWarnings("removal")
    @Override
    public void draw(GuiGraphics guiGraphics, double mouseX, double mouseY) {
        icon.draw(guiGraphics, 0, 0);
    }

    @Override
    public void getTooltip(ITooltipBuilder tooltip, double mouseX, double mouseY) {
        if (isMouseOver(mouseX, mouseY)) {
            tooltip.add(Component.translatable("envelope.jei.info"));
            tooltip.add(this.info.copy().withStyle(ChatFormatting.GRAY));
        }
    }

    public boolean isMouseOver(double mouseX, double mouseY) {
        return area.contains(mouseX + area.getX(), mouseY + area.getY());
    }
}
