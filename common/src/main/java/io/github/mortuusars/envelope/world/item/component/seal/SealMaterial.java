package io.github.mortuusars.envelope.world.item.component.seal;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.mortaar.serialization.Codecs;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.RegistryFileCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public final class SealMaterial {
    public static final Codec<SealMaterial> DIRECT_CODEC = RecordCodecBuilder.create(i -> i.group(
          ResourceLocation.CODEC.fieldOf("texture").forGetter(SealMaterial::textureId),
          Codecs.HEX_COLOR.fieldOf("model_tint_color").forGetter(SealMaterial::modelTintColor),
          ShadingPalette.CODEC.fieldOf("impression_palette").forGetter(SealMaterial::impressionPalette),
          Codec.BOOL.optionalFieldOf("has_glint", false).forGetter(SealMaterial::hasGlint)
    ).apply(i, SealMaterial::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, SealMaterial> DIRECT_STREAM_CODEC = StreamCodec.composite(
          ResourceLocation.STREAM_CODEC, SealMaterial::textureId,
          ByteBufCodecs.INT, SealMaterial::modelTintColor,
          ShadingPalette.STREAM_CODEC, SealMaterial::impressionPalette,
          ByteBufCodecs.BOOL, SealMaterial::hasGlint,
          SealMaterial::new
    );

    public static final Codec<Holder<SealMaterial>> CODEC =
          RegistryFileCodec.create(Envelope.Registries.SEAL_MATERIAL, DIRECT_CODEC);

    public static final StreamCodec<RegistryFriendlyByteBuf, Holder<SealMaterial>> STREAM_CODEC =
          ByteBufCodecs.holder(Envelope.Registries.SEAL_MATERIAL, DIRECT_STREAM_CODEC);

    public static final Map<DyeColor, ResourceKey<SealMaterial>> WAX_COLORS = new LinkedHashMap<>();
    public static final ResourceKey<SealMaterial> WAX = ResourceKey.create(Envelope.Registries.SEAL_MATERIAL, Envelope.resource("wax"));
    public static final ResourceKey<SealMaterial> WHITE_WAX = dyedWax(DyeColor.WHITE);
    public static final ResourceKey<SealMaterial> LIGHT_GRAY_WAX = dyedWax(DyeColor.LIGHT_GRAY);
    public static final ResourceKey<SealMaterial> GRAY_WAX = dyedWax(DyeColor.GRAY);
    public static final ResourceKey<SealMaterial> BLACK_WAX = dyedWax(DyeColor.BLACK);
    public static final ResourceKey<SealMaterial> BROWN_WAX = dyedWax(DyeColor.BROWN);
    public static final ResourceKey<SealMaterial> RED_WAX = dyedWax(DyeColor.RED);
    public static final ResourceKey<SealMaterial> ORANGE_WAX = dyedWax(DyeColor.ORANGE);
    public static final ResourceKey<SealMaterial> YELLOW_WAX = dyedWax(DyeColor.YELLOW);
    public static final ResourceKey<SealMaterial> LIME_WAX = dyedWax(DyeColor.LIME);
    public static final ResourceKey<SealMaterial> GREEN_WAX = dyedWax(DyeColor.GREEN);
    public static final ResourceKey<SealMaterial> CYAN_WAX = dyedWax(DyeColor.CYAN);
    public static final ResourceKey<SealMaterial> LIGHT_BLUE_WAX = dyedWax(DyeColor.LIGHT_BLUE);
    public static final ResourceKey<SealMaterial> BLUE_WAX = dyedWax(DyeColor.BLUE);
    public static final ResourceKey<SealMaterial> PURPLE_WAX = dyedWax(DyeColor.PURPLE);
    public static final ResourceKey<SealMaterial> MAGENTA_WAX = dyedWax(DyeColor.MAGENTA);
    public static final ResourceKey<SealMaterial> PINK_WAX = dyedWax(DyeColor.PINK);

    public static final ResourceKey<SealMaterial> GOLD = ResourceKey.create(Envelope.Registries.SEAL_MATERIAL, Envelope.resource("gold"));

    private final ResourceLocation textureId;
    private final ResourceLocation textureFull;
    private final int modelTintColor;
    private final ShadingPalette impressionPalette;
    private final boolean hasGlint;

    public SealMaterial(ResourceLocation texture, int modelTintColor, ShadingPalette impressionPalette, boolean hasGlint) {
        this.textureId = texture;
        this.textureFull = texture.withPath(path -> "textures/" + path + ".png");
        this.modelTintColor = modelTintColor;
        this.impressionPalette = impressionPalette;
        this.hasGlint = hasGlint;
    }

    public static @Nullable ResourceKey<SealMaterial> fromDyeColor(DyeColor color) {
        return WAX_COLORS.get(color);
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

    public boolean hasGlint() {
        return hasGlint;
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
              "impressionPalette=" + impressionPalette + "," +
              "hasGlint=" + hasGlint + ']';
    }

    // --

    private static ResourceKey<SealMaterial> dyedWax(DyeColor color) {
        ResourceKey<SealMaterial> key = ResourceKey.create(Envelope.Registries.SEAL_MATERIAL, Envelope.resource(color.getSerializedName() + "_wax"));
        WAX_COLORS.put(color, key);
        return key;
    }

    // --

    public static Optional<Holder.Reference<SealMaterial>> get(HolderLookup.Provider registries, ResourceKey<SealMaterial> key) {
        return registries.lookupOrThrow(Envelope.Registries.SEAL_MATERIAL).get(key);
    }

    public static Holder<SealMaterial> getOrThrow(HolderLookup.Provider registries, ResourceKey<SealMaterial> key) {
        return registries.lookupOrThrow(Envelope.Registries.SEAL_MATERIAL).getOrThrow(key);
    }

    public static ResourceLocation textureLocationFromKey(ResourceKey<SealMaterial> key) {
        return key.location().withPath(path -> "seal/material/" + path);
    }

    public static void bootstrap(BootstrapContext<SealMaterial> context) {
        register(context, WAX, 0xFFCC4E47, 0xFFA53732, 0xFFE17D68, 0xFF761814, 0xFF902926, false);
        register(context, WHITE_WAX, 0xFFFCFDFF, 0xFFD1D1D7, 0xFFECECEE, 0xFF9F9FAC, 0xFFB8B8BF, false);
        register(context, LIGHT_GRAY_WAX, 0xFFC9C9CD, 0xFF9D9DA5, 0xFFCCCCCF, 0xFF71717D, 0xFF8A8A94, false);
        register(context, GRAY_WAX, 0xFF969595, 0xFF63636F, 0xFF8D8D94, 0xFF3F3F4C, 0xFF52525C, false);
        register(context, BLACK_WAX, 0xFF414053, 0xFF2A293F, 0xFF58566E, 0xFF09081D, 0xFF161526, false);
        register(context, BROWN_WAX, 0xFFA76641, 0xFF834623, 0xFFCC865C, 0xFF5D2A09, 0xFF6E3816, false);
        register(context, RED_WAX, 0xFFFF5C53, 0xFFBB3B36, 0xFFF56F5D, 0xFF891312, 0xFFA62C29, false);
        register(context, ORANGE_WAX, 0xFFF39B19, 0xFFCA712C, 0xFFF7A94F, 0xFF9D460C, 0xFFB55C1D, false);
        register(context, YELLOW_WAX, 0xFFE6E029, 0xFFD3AF2A, 0xFFF4DF4F, 0xFFA3770C, 0xFFC2951B, false);
        register(context, LIME_WAX, 0xFF87DA1C, 0xFF8AC03C, 0xFFB8ED63, 0xFF578812, 0xFF71A626, false);
        register(context, GREEN_WAX, 0xFF668A16, 0xFF66881C, 0xFF97BA38, 0xFF3F5812, 0xFF527016, false);
        register(context, CYAN_WAX, 0xFF4AA7B0, 0xFF3A8992, 0xFF6ABCC0, 0xFF16616A, 0xFF28747D, false);
        register(context, LIGHT_BLUE_WAX, 0xFF93BFFF, 0xFF6E9FC8, 0xFF9DCDEA, 0xFF376993, 0xFF5386B5, false);
        register(context, BLUE_WAX, 0xFF537EE8, 0xFF3A4D8F, 0xFF6A80C6, 0xFF182C65, 0xFF2A3D78, false);
        register(context, PURPLE_WAX, 0xFFA755DA, 0xFF6C3697, 0xFF9D69CF, 0xFF3D1468, 0xFF53257D, false);
        register(context, MAGENTA_WAX, 0xFFCA6DC4, 0xFF973291, 0xFFCF66CD, 0xFF681369, 0xFF7D247B, false);
        register(context, PINK_WAX, 0xFFF283B9, 0xFFD4548E, 0xFFFA94CD, 0xFFA11D5D, 0xFFBA3C75, false);
        register(context, GOLD, 0xFFFFB347, 0xFFE39C38, 0xFFFFE685, 0xFFA15611, 0xFFBF7B22, true);
    }

    private static void register(BootstrapContext<SealMaterial> context, ResourceKey<SealMaterial> key, int modelTintColor,
                                 int paletteBaseColor, int paletteHighlightColor, int paletteShadowColor, int paletteSideColor, boolean hasGlint) {
        context.register(key, new SealMaterial(
              textureLocationFromKey(key),
              modelTintColor,
              new ShadingPalette(paletteBaseColor, paletteHighlightColor, paletteShadowColor, paletteSideColor), hasGlint));
    }
}
