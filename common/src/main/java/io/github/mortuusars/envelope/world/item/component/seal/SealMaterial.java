package io.github.mortuusars.envelope.world.item.component.seal;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.util.EnvelopeCodecs;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.RegistryFileCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

public final class SealMaterial {
    public static final Codec<SealMaterial> DIRECT_CODEC = RecordCodecBuilder.create(i -> i.group(
          ResourceLocation.CODEC.fieldOf("texture").forGetter(SealMaterial::textureId),
          EnvelopeCodecs.HEX_COLOR.fieldOf("model_tint_color").forGetter(SealMaterial::modelTintColor),
          ShadingPalette.CODEC.fieldOf("impression_palette").forGetter(SealMaterial::impressionPalette)
    ).apply(i, SealMaterial::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, SealMaterial> DIRECT_STREAM_CODEC = StreamCodec.composite(
          ResourceLocation.STREAM_CODEC, SealMaterial::textureId,
          ByteBufCodecs.INT, SealMaterial::modelTintColor,
          ShadingPalette.STREAM_CODEC, SealMaterial::impressionPalette,
          SealMaterial::new
    );

    public static final Codec<Holder<SealMaterial>> CODEC =
          RegistryFileCodec.create(Envelope.Registries.SEAL_MATERIAL, DIRECT_CODEC);

    public static final StreamCodec<RegistryFriendlyByteBuf, Holder<SealMaterial>> STREAM_CODEC =
          ByteBufCodecs.holder(Envelope.Registries.SEAL_MATERIAL, DIRECT_STREAM_CODEC);

    public static final ResourceKey<SealMaterial> RED_WAX = ResourceKey.create(Envelope.Registries.SEAL_MATERIAL, Envelope.resource("red_wax"));
    public static final ResourceKey<SealMaterial> GOLD = ResourceKey.create(Envelope.Registries.SEAL_MATERIAL, Envelope.resource("gold"));

    private final ResourceLocation textureId;
    private final ResourceLocation textureFull;
    private final int modelTintColor;
    private final ShadingPalette impressionPalette;

    public SealMaterial(ResourceLocation texture, int modelTintColor, ShadingPalette impressionPalette) {
        this.textureId = texture;
        this.textureFull = texture.withPath(path -> "textures/" + path + ".png");
        this.modelTintColor = modelTintColor;
        this.impressionPalette = impressionPalette;
    }

    public ResourceLocation textureId() {
        return textureId;
    }

    public ResourceLocation texture() {
        return textureFull;
    }

    public int modelTintColor() {
        return modelTintColor;
    }

    public ShadingPalette impressionPalette() {
        return impressionPalette;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (SealMaterial) obj;
        return Objects.equals(this.textureId, that.textureId) &&
              this.modelTintColor == that.modelTintColor &&
              Objects.equals(this.impressionPalette, that.impressionPalette);
    }

    @Override
    public int hashCode() {
        int i = 1;
        i = 31 * i + this.textureId.hashCode();
        i = 31 * i + this.modelTintColor;
        return 31 * i + this.impressionPalette.hashCode();
    }

    @Override
    public String toString() {
        return "SealMaterial[" +
              "texture=" + textureId + ", " +
              "modelTintColor=" + modelTintColor + ", " +
              "impressionPalette=" + impressionPalette + ']';
    }

    // --

    public static Optional<Holder.Reference<SealMaterial>> get(HolderLookup.Provider registries, ResourceKey<SealMaterial> key) {
        return registries.lookupOrThrow(Envelope.Registries.SEAL_MATERIAL).get(key);
    }

    public static Holder<SealMaterial> getOrThrow(HolderLookup.Provider registries, ResourceKey<SealMaterial> key) {
        return registries.lookupOrThrow(Envelope.Registries.SEAL_MATERIAL).getOrThrow(key);
    }

    public static void bootstrap(BootstrapContext<SealMaterial> context) {
        Function<ResourceKey<SealMaterial>, ResourceLocation> keyToTexture = key ->
              key.location().withPath(path -> "seal/material/" + path);

        context.register(RED_WAX, new SealMaterial(
              keyToTexture.apply(RED_WAX),
              0xFFCC4E47,
              new ShadingPalette(0xFFA73A34, 0xFFF18E78, 0xFF660C0A, 0xFF8A2622)));
        context.register(GOLD, new SealMaterial(
              keyToTexture.apply(GOLD),
              0xFFFFB347,
              new ShadingPalette(0xFFD79736, 0xFFFFEAAD, 0xFF75340B, 0xFFB56D24)));
    }
}
