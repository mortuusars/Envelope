package io.github.mortuusars.envelope.world.item.component.seal;

import io.github.mortuusars.envelope.util.HardLightColor;

public record SealImpressionTheme(HardLightColor base, HardLightColor highlight, HardLightColor shadow, HardLightColor side) {
    public static final SealImpressionTheme RED_WAX = new SealImpressionTheme(0xFFA73A34, 0xFFF18E78, 0xFF660C0A, 0xFF8A2622);
    public static final SealImpressionTheme GOLD = new SealImpressionTheme(0xFFD98F2E, 0xFFFFECA1, 0xFF77340F, 0xFFB36725);

    public static final SealImpressionTheme IRON_DIE = new SealImpressionTheme(0xFF9A9CA1, 0xFFDFE0E3, 0xFF5D6068, 0xFFC4C5C8);

    public SealImpressionTheme(int base, int highlight, int shadow, int side) {
        this(HardLightColor.of(base), HardLightColor.of(highlight), HardLightColor.of(shadow), HardLightColor.of(side));
    }
}
