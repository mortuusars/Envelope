package io.github.mortuusars.envelope.world.item.component.seal;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.mortuusars.envelope.Envelope;
import net.minecraft.Util;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.RegistryFileCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public final class SealImpression {
    public static final Codec<SealImpression> DIRECT_CODEC = RecordCodecBuilder.create(i -> i.group(
          ResourceLocation.CODEC.fieldOf("texture").forGetter(SealImpression::textureId)
    ).apply(i, SealImpression::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, SealImpression> DIRECT_STREAM_CODEC = StreamCodec.composite(
          ResourceLocation.STREAM_CODEC, SealImpression::textureId,
          SealImpression::new
    );

    public static final Codec<Holder<SealImpression>> CODEC =
          RegistryFileCodec.create(Envelope.Registries.SEAL_IMPRESSION, DIRECT_CODEC);

    public static final StreamCodec<RegistryFriendlyByteBuf, Holder<SealImpression>> STREAM_CODEC =
          ByteBufCodecs.holder(Envelope.Registries.SEAL_IMPRESSION, DIRECT_STREAM_CODEC);

    // --

    public static final Map<Character, ResourceKey<SealImpression>> LETTERS = Util.make(new HashMap<>(), map -> {
        for (char c = 'a'; c <= 'z'; c++) {
            map.put(c, key("letter/" + c));
        }
    });
    public static final Map<Character, ResourceKey<SealImpression>> NUMBERS = Util.make(new HashMap<>(), map -> {
        for (char c = '0'; c <= '9'; c++) {
            map.put(c, key("number/" + c));
        }
    });
    public static final Map<String, ResourceKey<SealImpression>> SYMBOLS = new HashMap<>();

    public static final ResourceKey<SealImpression> APPLE = symbolKey("apple");
    public static final ResourceKey<SealImpression> AXE = symbolKey("axe");
    public static final ResourceKey<SealImpression> BLOCK = symbolKey("block");
    public static final ResourceKey<SealImpression> BOOK = symbolKey("book");
    public static final ResourceKey<SealImpression> CREEPER = symbolKey("creeper");
    public static final ResourceKey<SealImpression> EMERALD = symbolKey("emerald");
    public static final ResourceKey<SealImpression> HEART = symbolKey("heart");
    public static final ResourceKey<SealImpression> HOE = symbolKey("hoe");
    public static final ResourceKey<SealImpression> LETTER = symbolKey("letter"); // Do not confuse with LETTERS
    public static final ResourceKey<SealImpression> PICKAXE = symbolKey("pickaxe");
    public static final ResourceKey<SealImpression> SHOVEL = symbolKey("shovel");
    public static final ResourceKey<SealImpression> SKELETON = symbolKey("skeleton");
    public static final ResourceKey<SealImpression> SKELETON_SMIRK = symbolKey("skeleton_smirk");
    public static final ResourceKey<SealImpression> SWORD = symbolKey("sword");
    public static final ResourceKey<SealImpression> SWORDS = symbolKey("swords");
    public static final ResourceKey<SealImpression> VILLAGER = symbolKey("villager");

    public static final ResourceKey<SealImpression> DEFAULT = CREEPER;

    // --

    private final ResourceLocation texture;
    private final ResourceLocation textureFull;

    public SealImpression(ResourceLocation texture) {
        this.texture = texture;
        this.textureFull = texture.withPath(path -> "textures/" + path + ".png");
    }

    public ResourceLocation textureId() {
        return texture;
    }

    public ResourceLocation texture() {
        return textureFull;
    }

    public static ResourceKey<SealImpression> firstCharOrDefault(String string) {
        for (int i = 0; i < string.length(); i++) {
            char c = Character.toLowerCase(string.charAt(i));

            if (c >= 'a' && c <= 'z') {
                return LETTERS.getOrDefault(c, DEFAULT);
            }

            if (c >= '0' && c <= '9') {
                return NUMBERS.getOrDefault(c, DEFAULT);
            }
        }

        return DEFAULT;
    }

    public static ResourceKey<SealImpression> firstCharOrDefault(@Nullable Player player) {
        if (player == null) {
            return DEFAULT;
        }
        return firstCharOrDefault(player.getScoreboardName());
    }

    // --

    public static Holder<SealImpression> getHolder(RegistryAccess access, ResourceKey<SealImpression> key) {
        return access.registryOrThrow(Envelope.Registries.SEAL_IMPRESSION).getHolderOrThrow(key);
    }

    // --

    private static ResourceKey<SealImpression> key(String path) {
        return ResourceKey.create(Envelope.Registries.SEAL_IMPRESSION, Envelope.resource(path));
    }

    private static ResourceKey<SealImpression> symbolKey(String path) {
        ResourceKey<SealImpression> key = ResourceKey.create(Envelope.Registries.SEAL_IMPRESSION, Envelope.resource(path));
        SYMBOLS.put(path, key);
        return key;
    }

    public static void bootstrap(BootstrapContext<SealImpression> context) {
        Function<ResourceKey<SealImpression>, ResourceLocation> keyToTexture = key ->
              key.location().withPath(path -> "seal/impression/" + path);

        NUMBERS.values().forEach(key -> context.register(key, new SealImpression(keyToTexture.apply(key))));
        LETTERS.values().forEach(key -> context.register(key, new SealImpression(keyToTexture.apply(key))));
        SYMBOLS.values().forEach(key -> context.register(key, new SealImpression(keyToTexture.apply(key))));
    }
}
