package io.github.mortuusars.envelope.world.item.component.seal;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import io.github.mortuusars.envelope.util.HardLightColor;
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
    private final Colors colors;

    /**
     * @param id Used to identify this material and its texture in 'textures/gui/seal' folder.
     * @param itemTintColor Used to tint parts of the item model.
     * @param colors Used to tint the impression.
     */
    public SealMaterial(ResourceLocation id, int itemTintColor, Colors colors) {
        this.id = id;
        this.texture = id.withPath(path -> "textures/gui/seal/" + path + ".png");
        this.itemTintColor = itemTintColor;
        this.colors = colors;
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

    public Colors getColors() {
        return colors;
    }

    public record Colors(HardLightColor base, HardLightColor highlight, HardLightColor shadow, HardLightColor side) {
        public Colors(int base, int highlight, int shadow, int side) {
            this(HardLightColor.of(base), HardLightColor.of(highlight), HardLightColor.of(shadow), HardLightColor.of(side));
        }
    }
}
