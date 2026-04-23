package io.github.mortuusars.envelope.client.gui.tooltip;

import io.github.mortuusars.envelope.EnvelopeClient;
import io.github.mortuusars.envelope.client.util.Minecrft;
import io.github.mortuusars.envelope.world.item.component.seal.SealImpression;
import io.github.mortuusars.envelope.world.item.component.seal.ShadingPalette;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;

import java.util.Optional;

public class SealDieTooltipComponent implements ClientTooltipComponent {
    protected final SealImpression impression;

    public SealDieTooltipComponent(SealImpression impression) {
        this.impression = impression;
    }

    public SealDieTooltipComponent(Optional<Holder<SealImpression>> impressionHolder) {
        this(getImpression(impressionHolder));
    }

    private static SealImpression getImpression(Optional<Holder<SealImpression>> impressionHolder) {
        return impressionHolder
              .orElseGet(() -> {
                  ResourceKey<SealImpression> key = SealImpression.firstCharOrDefault(Minecrft.player());
                  return SealImpression.getOrThrow(Minecrft.registryAccess(), key);
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
        EnvelopeClient.getSealRenderer().renderDie(impression, ShadingPalette.IRON_DIE, guiGraphics, x - 1, y - 1);
    }
}
