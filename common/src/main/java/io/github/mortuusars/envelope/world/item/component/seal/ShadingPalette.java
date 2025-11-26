package io.github.mortuusars.envelope.world.item.component.seal;

import io.github.mortuusars.envelope.util.TintColor;

public record ShadingPalette(TintColor base, TintColor highlight, TintColor shadow, TintColor side) {
    public ShadingPalette(int base, int highlight, int shadow, int side) {
        this(TintColor.of(base), TintColor.of(highlight), TintColor.of(shadow), TintColor.of(side));
    }

    public static final ShadingPalette RED_WAX = new ShadingPalette(0xFFA73A34, 0xFFF18E78, 0xFF660C0A, 0xFF8A2622);
    public static final ShadingPalette GOLD = new ShadingPalette(0xFFD79736, 0xFFFFEAAD, 0xFF75340B, 0xFFB56D24);

    public static final ShadingPalette IRON_DIE = new ShadingPalette(0xFF9A9CA1, 0xFFDFE0E3, 0xFF5D6068, 0xFFC4C5C8);
}
