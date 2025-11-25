package io.github.mortuusars.envelope.world.item.component.seal;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class SealMaterial {
    public static final Codec<SealMaterial> CODEC = ResourceLocation.CODEC.comapFlatMap(
          id -> {
              @Nullable SealMaterial material = SealMaterials.get(id);
              return material != null
                    ? DataResult.success(material)
                    : DataResult.error(() -> "SealMaterial '" + id + "' is not registered.");
          },
          SealMaterial::getId);

    public static final StreamCodec<ByteBuf, SealMaterial> STREAM_CODEC = ResourceLocation.STREAM_CODEC.map(
          id -> Objects.requireNonNullElse(SealMaterials.get(id), SealMaterials.RED_WAX),
          SealMaterial::getId);

    private final ResourceLocation id;
    private final ResourceLocation texture;
    private final int itemTintColor;
    private final SealImpressionTheme impressionTheme;

    /**
     * @param id Used to identify this material and its texture in 'textures/gui/seal' folder.
     * @param itemTintColor Used to tint parts of the item model.
     * @param impressionTheme Used to tint the impression.
     */
    public SealMaterial(ResourceLocation id, int itemTintColor, SealImpressionTheme impressionTheme) {
        this.id = id;
        this.texture = id.withPath(path -> "textures/gui/seal/material/" + path + ".png");
        this.itemTintColor = itemTintColor;
        this.impressionTheme = impressionTheme;
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

    public SealImpressionTheme getImpressionTheme() {
        return impressionTheme;
    }
}
