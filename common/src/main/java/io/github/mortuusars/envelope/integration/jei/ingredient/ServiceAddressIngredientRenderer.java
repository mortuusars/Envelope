package io.github.mortuusars.envelope.integration.jei.ingredient;

import io.github.mortuusars.mortaar.client.Minecrft;
import io.github.mortuusars.envelope.util.Colors;
import io.github.mortuusars.envelope.util.EnvelopeSymbols;
import io.github.mortuusars.envelope.world.GameTime;
import io.github.mortuusars.envelope.world.mail.address.AddressFormatter;
import io.github.mortuusars.envelope.world.mail.address.type.ServiceAddress;
import mezz.jei.api.ingredients.IIngredientRenderer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class ServiceAddressIngredientRenderer implements IIngredientRenderer<ServiceAddress> {
    @Override
    public void render(GuiGraphics guiGraphics, ServiceAddress ingredient) {
        Font font = Minecrft.get().font;

        StringBuilder initials = new StringBuilder();
        for (String s : ingredient.getString().split("\\s")) {
            if (!s.isEmpty()) {
                initials.append(Character.toUpperCase(s.charAt(0)));
            }
        }

        String name = font.substrByWidth(FormattedText.of(initials.toString()), 16).getString();
        String icon = font.substrByWidth(FormattedText.of(AddressFormatter.getIcon(ingredient)), 16).getString();

        text(guiGraphics, font, icon, 9 - font.width(icon) / 2, 0);
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(8 - font.width(name) / 2f + 0.5f, 0, 0);
        text(guiGraphics, font, name, 0, 7);
        guiGraphics.pose().popPose();
    }

    private static void text(GuiGraphics guiGraphics, Font font, String string, int x, int y) {
        int mainColor = Colors.ADDRESS_NEUTRAL;
        int shadowColor = 0x453723;
        int outlineColor = 0x63533C;

        guiGraphics.drawString(font, string, x - 1, y, outlineColor, false);
        guiGraphics.drawString(font, string, x - 1, y - 1, outlineColor, false);
        guiGraphics.drawString(font, string, x, y - 1, outlineColor, false);
        guiGraphics.drawString(font, string, x + 1, y - 1, outlineColor, false);
        guiGraphics.drawString(font, string, x + 1, y, outlineColor, false);
        guiGraphics.drawString(font, string, x, y + 1, outlineColor, false);
        guiGraphics.drawString(font, string, x - 1, y + 1, outlineColor, false);

        guiGraphics.drawString(font, string, x + 1, y + 1, shadowColor, false);

        guiGraphics.drawString(font, string, x, y, mainColor, false);
    }

    @Override
    public @NotNull List<Component> getTooltip(ServiceAddress ingredient, TooltipFlag tooltipFlag) {
        if (tooltipFlag.isAdvanced()) {
            int travelDuration = ingredient.getDefinition().location().getTravelDurationTo(Minecrft.player().blockPosition()).ticks();
            return List.of(
                  ingredient.format().withIcon().toComponent(),
                  Component.literal("⌚" + EnvelopeSymbols.SMALL_SPACE)
                        .append(GameTime.format(travelDuration, true)).withStyle(ChatFormatting.GRAY));
        } else {
            return List.of(ingredient.format().withIcon().toComponent());
        }
    }
}
