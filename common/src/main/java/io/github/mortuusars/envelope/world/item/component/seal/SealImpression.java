package io.github.mortuusars.envelope.world.item.component.seal;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class SealImpression {
    public static final Codec<SealImpression> CODEC = ResourceLocation.CODEC.comapFlatMap(
          id -> {
              @Nullable SealImpression impression = SealImpressions.get(id);
              return impression != null
                    ? DataResult.success(impression)
                    : DataResult.error(() -> "SealImpression '" + id + "' is not registered.");
          },
          SealImpression::getId);

    public static final StreamCodec<ByteBuf, SealImpression> STREAM_CODEC = ResourceLocation.STREAM_CODEC.map(
          id -> Objects.requireNonNullElse(SealImpressions.get(id), SealImpressions.DEFAULT),
          SealImpression::getId);

    private final ResourceLocation id;
    private final ResourceLocation texture;

    public SealImpression(ResourceLocation id) {
        this.id = id;
        this.texture = id.withPath(path -> "textures/gui/seal_impression/" + path + ".png");
    }

    public ResourceLocation getId() {
        return id;
    }

    public ResourceLocation getTexture() {
        return texture;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (SealImpression) obj;
        return Objects.equals(this.id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "SealImpression[" +
              "id=" + id + ']';
    }
}
