package io.github.mortuusars.envelope.world.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.mortuusars.envelope.Envelope;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.RegistryFileCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.RandomSource;
import net.minecraft.util.random.WeightedEntry;
import net.minecraft.util.random.WeightedRandomList;
import net.minecraft.world.level.biome.Biome;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

public record PigeonVariant(ResourceLocation texture, HolderSet<Biome> biomes, SpawningWeights weights, boolean inheritable) {
    public static final ResourceKey<PigeonVariant> GRAY =
          ResourceKey.create(Envelope.Registries.PIGEON_VARIANT, Envelope.resource("gray"));
    public static final ResourceKey<PigeonVariant> BROWN =
          ResourceKey.create(Envelope.Registries.PIGEON_VARIANT, Envelope.resource("brown"));
    public static final ResourceKey<PigeonVariant> WHITE =
          ResourceKey.create(Envelope.Registries.PIGEON_VARIANT, Envelope.resource("white"));
    public static final ResourceKey<PigeonVariant> PASSENGER =
          ResourceKey.create(Envelope.Registries.PIGEON_VARIANT, Envelope.resource("passenger"));
    public static final ResourceKey<PigeonVariant> CHARRED =
          ResourceKey.create(Envelope.Registries.PIGEON_VARIANT, Envelope.resource("charred"));
    public static final ResourceKey<PigeonVariant> ARCHIMEDES =
          ResourceKey.create(Envelope.Registries.PIGEON_VARIANT, Envelope.resource("archimedes"));
    public static final ResourceKey<PigeonVariant> DEFAULT = GRAY;

    // --

    public static final Codec<PigeonVariant> DIRECT_CODEC = RecordCodecBuilder.create(i -> i.group(
          ResourceLocation.CODEC.fieldOf("texture").forGetter(PigeonVariant::texture),
          RegistryCodecs.homogeneousList(Registries.BIOME).optionalFieldOf("spawn_biomes", HolderSet.empty()).forGetter(PigeonVariant::biomes),
          SpawningWeights.CODEC.optionalFieldOf("spawn_weights", SpawningWeights.DEFAULT).forGetter(PigeonVariant::weights),
          Codec.BOOL.optionalFieldOf("inheritable", true).forGetter(PigeonVariant::inheritable)
    ).apply(i, PigeonVariant::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, PigeonVariant> DIRECT_STREAM_CODEC = StreamCodec.composite(
          ResourceLocation.STREAM_CODEC, PigeonVariant::texture,
          ByteBufCodecs.holderSet(Registries.BIOME), PigeonVariant::biomes,
          SpawningWeights.STREAM_CODEC, PigeonVariant::weights,
          ByteBufCodecs.BOOL, PigeonVariant::inheritable,
          PigeonVariant::new
    );

    public static final Codec<Holder<PigeonVariant>> CODEC =
          RegistryFileCodec.create(Envelope.Registries.PIGEON_VARIANT, DIRECT_CODEC);

    public static final StreamCodec<RegistryFriendlyByteBuf, Holder<PigeonVariant>> STREAM_CODEC =
          ByteBufCodecs.holder(Envelope.Registries.PIGEON_VARIANT, DIRECT_STREAM_CODEC);

    // --

    public static Optional<Holder.Reference<PigeonVariant>> get(RegistryAccess registryAccess, ResourceKey<PigeonVariant> key) {
        return registryAccess.registryOrThrow(Envelope.Registries.PIGEON_VARIANT).getHolder(key);
    }

    public static Holder<PigeonVariant> getOrThrow(RegistryAccess registryAccess, ResourceKey<PigeonVariant> key) {
        return registryAccess.registryOrThrow(Envelope.Registries.PIGEON_VARIANT).getHolderOrThrow(key);
    }

    public static Holder<PigeonVariant> getRandomSpawnVariant(RegistryAccess registryAccess, RandomSource random, Holder<Biome> biome) {
        return getRandomVariant(registryAccess, random,
              variant -> variant.biomes().contains(biome),
              variant -> variant.weights().biome())
              .orElseGet(() -> getRandomCommonVariant(registryAccess, random));
    }

    public static Holder<PigeonVariant> getRandomCommonVariant(RegistryAccess registryAccess, RandomSource random) {
        return withFallback(registryAccess,
              getRandomVariant(registryAccess, random, variant -> variant.weights().common()));
    }

    public static Holder<PigeonVariant> getRandomServiceVariant(RegistryAccess registryAccess, RandomSource random) {
        return withFallback(registryAccess,
              getRandomVariant(registryAccess, random, variant -> variant.weights().service()));
    }

    public static Optional<Holder<PigeonVariant>> getRandomVariant(RegistryAccess registryAccess, RandomSource random, Function<PigeonVariant, Integer> weightGetter) {
        return getRandomVariant(registryAccess, random, variant -> true, weightGetter);
    }

    public static Optional<Holder<PigeonVariant>> getRandomVariant(RegistryAccess registryAccess, RandomSource random, Predicate<PigeonVariant> filter, Function<PigeonVariant, Integer> weightGetter) {
        Registry<PigeonVariant> registry = registryAccess.registryOrThrow(Envelope.Registries.PIGEON_VARIANT);
        var variants = registry.holders()
              .filter(holder -> filter.test(holder.value()))
              .map(variant -> WeightedEntry.wrap(variant, weightGetter.apply(variant.value())))
              .toList();
        return WeightedRandomList.create(variants)
              .getRandom(random)
              .map(WeightedEntry.Wrapper::data);
    }

    public static Holder<PigeonVariant> withFallback(RegistryAccess registryAccess, Optional<Holder<PigeonVariant>> variant) {
        Registry<PigeonVariant> registry = registryAccess.registryOrThrow(Envelope.Registries.PIGEON_VARIANT);
        return variant
              .or(() -> registry.getHolder(DEFAULT))
              .or(registry::getAny)
              .orElseThrow();
    }

    public static String fromLegacyId(int variant) {
        var key = switch (variant) {
            case 1 -> BROWN;
            case 2 -> WHITE;
            case 3 -> PASSENGER;
            default -> DEFAULT;
        };
        return key.location().toString();
    }

    public record SpawningWeights(int biome, int service, int common) {
        public static final Codec<SpawningWeights> CODEC = RecordCodecBuilder.create(i -> i.group(
              ExtraCodecs.POSITIVE_INT.optionalFieldOf("biome", 1).forGetter(SpawningWeights::biome),
              ExtraCodecs.NON_NEGATIVE_INT.optionalFieldOf("service", 0).forGetter(SpawningWeights::service),
              ExtraCodecs.NON_NEGATIVE_INT.optionalFieldOf("common", 0).forGetter(SpawningWeights::common)
        ).apply(i, SpawningWeights::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, SpawningWeights> STREAM_CODEC = StreamCodec.composite(
              ByteBufCodecs.INT, SpawningWeights::biome,
              ByteBufCodecs.INT, SpawningWeights::service,
              ByteBufCodecs.INT, SpawningWeights::common,
              SpawningWeights::new
        );

        public static final SpawningWeights DEFAULT = new SpawningWeights(1, 0, 0);

        public SpawningWeights(int service, int common) {
            this(1, service, common);
        }
    }

    // --

    public static void bootstrap(BootstrapContext<PigeonVariant> context) {
        register(context, GRAY, "gray", HolderSet.empty(), new SpawningWeights(12, 10), true);
        register(context, BROWN, "brown", HolderSet.empty(), new SpawningWeights(6, 5), true);
        register(context, WHITE, "white", HolderSet.empty(), new SpawningWeights(2, 1), true);
        register(context, PASSENGER, "passenger", Envelope.Tags.Biomes.SPAWNS_PASSENGER_PIGEONS, new SpawningWeights(1, 0), true);
        register(context, CHARRED, "charred", HolderSet.empty(), new SpawningWeights(0, 0), false);
        register(context, ARCHIMEDES, "archimedes", HolderSet.empty(), new SpawningWeights(0, 0), false);
    }

    static void register(BootstrapContext<PigeonVariant> context, ResourceKey<PigeonVariant> key,
                         String name, ResourceKey<Biome> spawnBiome, SpawningWeights weights, boolean inheritable) {
        register(context, key, name,
              HolderSet.direct(context.lookup(Registries.BIOME).getOrThrow(spawnBiome)), weights, inheritable);
    }

    static void register(BootstrapContext<PigeonVariant> context, ResourceKey<PigeonVariant> key,
                         String name, TagKey<Biome> spawnBiomes, SpawningWeights weights, boolean inheritable) {
        register(context, key, name,
              context.lookup(Registries.BIOME).getOrThrow(spawnBiomes), weights, inheritable);
    }

    static void register(BootstrapContext<PigeonVariant> context, ResourceKey<PigeonVariant> key,
                         String name, HolderSet<Biome> spawnBiomes, SpawningWeights weights, boolean inheritable) {
        context.register(key, new PigeonVariant(Envelope.resource("textures/entity/pigeon/pigeon_" + name + ".png"),
              spawnBiomes, weights, inheritable));
    }
}
