package io.github.mortuusars.envelope.world.item.component.seal;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import io.github.mortuusars.envelope.Envelope;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class SealMaterial {
    public static final Map<ResourceLocation, SealMaterial> REGISTRY = new HashMap<>();

    public static final SealMaterial RED_WAX = register(
          new SealMaterial(Envelope.resource("red_wax"), 0xFFCC4E47, ShadingPalette.RED_WAX));
    public static final SealMaterial GOLD = register(
          new SealMaterial(Envelope.resource("gold"), 0xFFFFB347, ShadingPalette.GOLD));

    // --

    public static final Codec<SealMaterial> CODEC = ResourceLocation.CODEC.comapFlatMap(
          id -> {
              @Nullable SealMaterial material = get(id);
              return material != null
                    ? DataResult.success(material)
                    : DataResult.error(() -> "SealMaterial '" + id + "' is not registered.");
          },
          SealMaterial::getId);

    public static final StreamCodec<ByteBuf, SealMaterial> STREAM_CODEC = ResourceLocation.STREAM_CODEC.map(
          id -> Objects.requireNonNullElse(get(id), RED_WAX),
          SealMaterial::getId);

    private final ResourceLocation id;
    private final ResourceLocation texture;
    private final int itemTintColor;
    private final ShadingPalette impressionPalette;

    /**
     * @param id Used to identify this material and its texture in 'textures/gui/seal' folder.
     * @param itemTintColor Used to tint parts of the item model.
     * @param impressionPalette Used to tint the impression.
     */
    public SealMaterial(ResourceLocation id, int itemTintColor, ShadingPalette impressionPalette) {
        this.id = id;
        this.texture = id.withPath(path -> "textures/gui/seal/material/" + path + ".png");
        this.itemTintColor = itemTintColor;
        this.impressionPalette = impressionPalette;
    }

    public static SealMaterial register(SealMaterial material) {
        REGISTRY.put(material.getId(), material);
        return material;
    }

    public static @Nullable SealMaterial get(ResourceLocation id) {
        return REGISTRY.get(id);
    }

    public ResourceLocation getId() {
        return id;
    }

    public ResourceLocation getTexture() {
        return texture;
    }

    public int getItemTintColor() {
        return itemTintColor;
    }

    public ShadingPalette getImpressionPalette() {
        return impressionPalette;
    }
}
