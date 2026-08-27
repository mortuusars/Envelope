package io.github.mortuusars.envelope.client.gui.tooltip;

import io.github.mortuusars.envelope.EnvelopeClient;
import io.github.mortuusars.mortaar.client.Minecrft;
import io.github.mortuusars.envelope.world.item.component.seal.SealSymbol;
import io.github.mortuusars.envelope.world.item.component.seal.ShadingPalette;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;

import java.util.Optional;

public class SealDieTooltipComponent implements ClientTooltipComponent {
    protected final SealSymbol symbol;

    public SealDieTooltipComponent(SealSymbol symbol) {
        this.symbol = symbol;
    }

    public SealDieTooltipComponent(Optional<Holder<SealSymbol>> symbolHolder) {
        this(getSymbol(symbolHolder));
    }

    private static SealSymbol getSymbol(Optional<Holder<SealSymbol>> symbolHolder) {
        return symbolHolder
              .orElseGet(() -> {
                  ResourceKey<SealSymbol> key = SealSymbol.firstCharOrDefault(Minecrft.player());
                  return SealSymbol.getOrThrow(Minecrft.registryAccess(), key);
              })
              .value();
    }

    @Override
    public int getWidth(Font font) {
        return 31;
    }

    @Override
    public int getHeight() {
        return 31;
    }

    @Override
    public void renderImage(Font font, int x, int y, GuiGraphics guiGraphics) {
        EnvelopeClient.getSealRenderer().renderDie(symbol, ShadingPalette.IRON_DIE, guiGraphics, x - 1, y - 1);
    }
}
