package io.github.mortuusars.envelope.world.item.component.seal;

import io.github.mortuusars.envelope.Envelope;
import net.minecraft.Util;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class SealImpressions {
    public static final Map<ResourceLocation, SealImpression> REGISTRY = new HashMap<>();

    public static final SealImpression DEFAULT = register(new SealImpression(Envelope.resource("default")));
    public static final Map<String, SealImpression> LETTERS = Util.make(new HashMap<>(), map -> {
        for (char c = 'a'; c <= 'z'; c++) {
            String str = String.valueOf(c);
            map.put(str, register(new SealImpression(Envelope.resource(str))));
        }
    });
    public static final SealImpression APPLE = register(new SealImpression(Envelope.resource("apple")));
    public static final SealImpression AXE = register(new SealImpression(Envelope.resource("axe")));
    public static final SealImpression BLOCK = register(new SealImpression(Envelope.resource("block")));
    public static final SealImpression BOOK = register(new SealImpression(Envelope.resource("book")));
    public static final SealImpression CREEPER = register(new SealImpression(Envelope.resource("creeper")));
    public static final SealImpression EMERALD = register(new SealImpression(Envelope.resource("emerald")));
    public static final SealImpression HEART = register(new SealImpression(Envelope.resource("heart")));
    public static final SealImpression HOE = register(new SealImpression(Envelope.resource("hoe")));
    public static final SealImpression LETTER = register(new SealImpression(Envelope.resource("letter"))); // Do not confuse with LETTERS
    public static final SealImpression PICKAXE = register(new SealImpression(Envelope.resource("pickaxe")));
    public static final SealImpression SHOVEL = register(new SealImpression(Envelope.resource("shovel")));
    public static final SealImpression SKELETON = register(new SealImpression(Envelope.resource("skeleton")));
    public static final SealImpression SKELETON_SMIRK = register(new SealImpression(Envelope.resource("skeleton_smirk")));
    public static final SealImpression SWORD = register(new SealImpression(Envelope.resource("sword")));
    public static final SealImpression SWORDS = register(new SealImpression(Envelope.resource("swords")));
    public static final SealImpression VILLAGER = register(new SealImpression(Envelope.resource("villager")));

    // --

    public static SealImpression register(SealImpression material) {
        REGISTRY.put(material.getId(), material);
        return material;
    }

    public static @Nullable SealImpression get(ResourceLocation id) {
        return REGISTRY.get(id);
    }

    public static SealImpression getOrDefault(ResourceLocation id) {
        return REGISTRY.getOrDefault(id, DEFAULT);
    }

    public static SealImpression firstCharOrDefault(String string) {
        //TODO: numbers

        for (int i = 0; i < string.length(); i++) {
            char c = Character.toLowerCase(string.charAt(i));
            if (c >= 'a' && c <= 'z') {
                return LETTERS.getOrDefault(String.valueOf(c), DEFAULT);
            }
        }

        return DEFAULT;
    }
}
