package io.github.mortuusars.envelope.integration.jei.ingredient;

import io.github.mortuusars.envelope.client.util.Minecrft;
import io.github.mortuusars.envelope.util.Colors;
import io.github.mortuusars.envelope.util.EnvelopeSymbols;
import io.github.mortuusars.envelope.world.GameTime;
import io.github.mortuusars.envelope.world.mail.address.AddressFormatter;
import io.github.mortuusars.envelope.world.mail.address.type.EntityAddress;
import mezz.jei.api.ingredients.IIngredientRenderer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class EntityAddressIngredientRenderer implements IIngredientRenderer<EntityAddress> {
    @Override
    public void render(GuiGraphics guiGraphics, EntityAddress ingredient) {
        String address = ingredient.getString();
        String string = AddressFormatter.getIcon(ingredient)
              + EnvelopeSymbols.SMALL_SPACE
              + (!address.isEmpty() ? address.charAt(0) : "");

        guiGraphics.drawCenteredString(Minecrft.get().font, string, 9, 5, Colors.GUI_LABEL);
        guiGraphics.drawCenteredString(Minecrft.get().font, string, 8, 5, Colors.ADDRESS_NEUTRAL);
    }

    @Override
    public @NotNull List<Component> getTooltip(EntityAddress ingredient, TooltipFlag tooltipFlag) {
        if (tooltipFlag.isAdvanced()) {
            int travelDuration = ingredient.getEntity().location().getTravelDurationTo(Minecrft.player().blockPosition()).ticks();
            return List.of(
                  ingredient.format().withIcon().toComponent(),
                  Component.literal("⌚" + EnvelopeSymbols.SMALL_SPACE)
                        .append(GameTime.format(travelDuration, true)).withStyle(ChatFormatting.GRAY));
        } else {
            return List.of(ingredient.format().withIcon().toComponent());
        }
    }
}
