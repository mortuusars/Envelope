package io.github.mortuusars.envelope.integration.jei.category;

import io.github.mortuusars.envelope.integration.jei.EnvelopeJeiPlugin;
import io.github.mortuusars.envelope.util.TextUtil;
import io.github.mortuusars.envelope.world.mail.address.type.ServiceAddress;
import io.github.mortuusars.mortaar.client.Minecrft;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.widgets.IRecipeWidget;
import mezz.jei.api.ingredients.IIngredientRenderer;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.ScreenPosition;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.NotNull;

public class ServiceAddressRecipeWidget implements IRecipeWidget {
    protected ScreenRectangle area;
    protected final ServiceAddress address;
    protected Font font;
    protected final FormattedText text;
    protected final int textWidth;

    public ServiceAddressRecipeWidget(ScreenRectangle area, ServiceAddress address) {
        this.area = area;
        this.address = address;
        Component addressComponent = address
              .format()
              .withIcon()
              .toComponent();
        this.font = Minecrft.get().font;
        this.text = TextUtil.truncateStringToWidth(font, addressComponent, area.width());
        this.textWidth = font.width(text);
    }

    @Override
    public @NotNull ScreenPosition getPosition() {
        return area.position();
    }

    @SuppressWarnings("removal")
    @Override
    public void draw(GuiGraphics guiGraphics, double mouseX, double mouseY) {
        int x = area.position().x() + area.width() / 2 - textWidth / 2;
        int y = area.position().y();

        int color = 0xFF808080;
        if (isHovering(mouseX, mouseY)) {
            color = 0xFFFFFFFF;
        }

        guiGraphics.drawString(font, Language.getInstance().getVisualOrder(text), x, y, color, false);
    }

    @Override
    public void getTooltip(ITooltipBuilder tooltip, double mouseX, double mouseY) {
        if (isHovering(mouseX, mouseY) && EnvelopeJeiPlugin.runtime != null) {
            // All that is to render the same tooltip as JEI renders on ingredients (with blue mod-name text, etc.)
            IIngredientRenderer<ServiceAddress> renderer =
                  EnvelopeJeiPlugin.runtime.getIngredientManager().getIngredientRenderer(EnvelopeJeiPlugin.SERVICE_ADDRESS_INGREDIENT);

            //noinspection removal
            renderer.getTooltip(tooltip, address,
                  Minecrft.options().advancedItemTooltips ? TooltipFlag.ADVANCED : TooltipFlag.NORMAL);

            EnvelopeJeiPlugin.runtime.getIngredientManager()
                  .createTypedIngredient(EnvelopeJeiPlugin.SERVICE_ADDRESS_INGREDIENT, address, false)
                  .ifPresent(tooltip::setIngredient);
        }
    }

    public boolean isHovering(double mouseX, double mouseY) {
        double textX = area.position().x() + area.width() / 2.0 - textWidth / 2.0;
        return mouseX >= textX && mouseX <= textX + textWidth
              && mouseY >= area.position().y() && mouseY <= area.height();
    }
}
