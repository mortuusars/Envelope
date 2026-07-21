package io.github.mortuusars.envelope.world.item.component.seal;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.mortuusars.envelope.Envelope;
import net.minecraft.Util;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
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
import java.util.Optional;
import java.util.function.Function;

public final class SealSymbol {
    public static final Codec<SealSymbol> DIRECT_CODEC = RecordCodecBuilder.create(i -> i.group(
          ResourceLocation.CODEC.fieldOf("texture").forGetter(SealSymbol::textureId)
    ).apply(i, SealSymbol::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, SealSymbol> DIRECT_STREAM_CODEC = StreamCodec.composite(
          ResourceLocation.STREAM_CODEC, SealSymbol::textureId,
          SealSymbol::new
    );

    public static final Codec<Holder<SealSymbol>> CODEC =
          RegistryFileCodec.create(Envelope.Registries.SEAL_SYMBOL, DIRECT_CODEC);

    public static final StreamCodec<RegistryFriendlyByteBuf, Holder<SealSymbol>> STREAM_CODEC =
          ByteBufCodecs.holder(Envelope.Registries.SEAL_SYMBOL, DIRECT_STREAM_CODEC);

    // --

    public static final Map<Character, ResourceKey<SealSymbol>> LETTERS = Util.make(new HashMap<>(), map -> {
        for (char c = 'a'; c <= 'z'; c++) {
            map.put(c, key("letter/" + c));
        }
    });
    public static final Map<Character, ResourceKey<SealSymbol>> NUMBERS = Util.make(new HashMap<>(), map -> {
        for (char c = '0'; c <= '9'; c++) {
            map.put(c, key("number/" + c));
        }
    });
    public static final Map<String, ResourceKey<SealSymbol>> EMBLEMS = new HashMap<>();

    public static final ResourceKey<SealSymbol> APPLE = emblemKey("apple");
    public static final ResourceKey<SealSymbol> AXE = emblemKey("axe");
    public static final ResourceKey<SealSymbol> BLOCK = emblemKey("block");
    public static final ResourceKey<SealSymbol> BOOK = emblemKey("book");
    public static final ResourceKey<SealSymbol> CREEPER = emblemKey("creeper");
    public static final ResourceKey<SealSymbol> EMERALD = emblemKey("emerald");
    public static final ResourceKey<SealSymbol> HEART = emblemKey("heart");
    public static final ResourceKey<SealSymbol> HOE = emblemKey("hoe");
    public static final ResourceKey<SealSymbol> LETTER = emblemKey("letter"); // Do not confuse with LETTERS
    public static final ResourceKey<SealSymbol> PICKAXE = emblemKey("pickaxe");
    public static final ResourceKey<SealSymbol> SHOVEL = emblemKey("shovel");
    public static final ResourceKey<SealSymbol> SKELETON = emblemKey("skeleton");
    public static final ResourceKey<SealSymbol> SKELETON_SMIRK = emblemKey("skeleton_smirk");
    public static final ResourceKey<SealSymbol> SWORD = emblemKey("sword");
    public static final ResourceKey<SealSymbol> SWORDS = emblemKey("swords");
    public static final ResourceKey<SealSymbol> VILLAGER = emblemKey("villager");

    public static final ResourceKey<SealSymbol> DEFAULT = CREEPER;

    // --

    private final ResourceLocation texture;
    private final ResourceLocation textureFull;

    public SealSymbol(ResourceLocation texture) {
        this.texture = texture;
        this.textureFull = texture.withPath(path -> "textures/" + path + ".png");
    }

    public ResourceLocation textureId() {
        return texture;
    }

    public ResourceLocation texture() {
        return textureFull;
    }

    public static ResourceKey<SealSymbol> firstCharOrDefault(String string) {
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

    public static ResourceKey<SealSymbol> firstCharOrDefault(@Nullable Player player) {
        if (player == null) {
            return DEFAULT;
        }
        return firstCharOrDefault(player.getScoreboardName());
    }

    // --

    public static Optional<Holder.Reference<SealSymbol>> get(HolderLookup.Provider registries, ResourceKey<SealSymbol> key) {
        return registries.lookupOrThrow(Envelope.Registries.SEAL_SYMBOL).get(key);
    }

    public static Holder<SealSymbol> getOrThrow(HolderLookup.Provider registries, ResourceKey<SealSymbol> key) {
        return registries.lookupOrThrow(Envelope.Registries.SEAL_SYMBOL).getOrThrow(key);
    }

    // --

    private static ResourceKey<SealSymbol> key(String path) {
        return ResourceKey.create(Envelope.Registries.SEAL_SYMBOL, Envelope.resource(path));
    }

    private static ResourceKey<SealSymbol> emblemKey(String path) {
        ResourceKey<SealSymbol> key = ResourceKey.create(Envelope.Registries.SEAL_SYMBOL, Envelope.resource(path));
        EMBLEMS.put(path, key);
        return key;
    }

    public static void bootstrap(BootstrapContext<SealSymbol> context) {
        Function<ResourceKey<SealSymbol>, ResourceLocation> keyToTexture = key ->
              key.location().withPath(path -> "seal/symbol/" + path);

        NUMBERS.values().forEach(key -> context.register(key, new SealSymbol(keyToTexture.apply(key))));
        LETTERS.values().forEach(key -> context.register(key, new SealSymbol(keyToTexture.apply(key))));
        EMBLEMS.values().forEach(key -> context.register(key, new SealSymbol(keyToTexture.apply(key))));
    }
}
