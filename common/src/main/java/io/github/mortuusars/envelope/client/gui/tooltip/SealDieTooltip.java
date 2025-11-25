package io.github.mortuusars.envelope.client.gui.tooltip;

import io.github.mortuusars.envelope.EnvelopeClient;
import io.github.mortuusars.envelope.client.util.Minecrft;
import io.github.mortuusars.envelope.world.item.component.seal.SealImpression;
import io.github.mortuusars.envelope.world.item.component.seal.SealImpressionTheme;
import io.github.mortuusars.envelope.world.item.component.seal.SealImpressions;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;

import java.util.Optional;

public class SealDieTooltip implements ClientTooltipComponent {
    protected final SealImpression impression;

    public SealDieTooltip(Optional<SealImpression> impression) {
        this.impression = impression.orElseGet(() ->
              SealImpressions.firstCharOrDefault(Minecrft.player().getScoreboardName()));
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
        EnvelopeClient.getSealRenderer().renderDie(impression, SealImpressionTheme.IRON_DIE, guiGraphics, x - 1, y - 1);
    }
}
