package io.github.mortuusars.envelope.world.item.component.seal;

import io.github.mortuusars.envelope.Envelope;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class SealMaterials {
    public static final Map<ResourceLocation, SealMaterial> REGISTRY = new HashMap<>();

    public static final SealMaterial RED_WAX = register(
          new SealMaterial(Envelope.resource("red_wax"), 0xFFCC4E47, SealImpressionTheme.RED_WAX));

    public static final SealMaterial GOLD = register(
          new SealMaterial(Envelope.resource("gold"), 0xFFFFB347, SealImpressionTheme.GOLD));

    // --

    public static SealMaterial register(SealMaterial material) {
        REGISTRY.put(material.getId(), material);
        return material;
    }

    public static @Nullable SealMaterial get(ResourceLocation id) {
        return REGISTRY.get(id);
    }
}
