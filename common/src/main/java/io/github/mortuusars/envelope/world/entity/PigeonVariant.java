package io.github.mortuusars.envelope.world.entity;

import com.mojang.serialization.Codec;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.util.random.Weight;
import net.minecraft.util.random.WeightedEntry;
import net.minecraft.util.random.WeightedRandomList;
import org.jetbrains.annotations.NotNull;

import java.util.function.IntFunction;

public enum PigeonVariant implements StringRepresentable {
    GRAY(0, "gray"),
    BROWN(1, "brown"),
    WHITE(2, "white"),
    PASSENGER(3, "passenger");

    public static final Codec<PigeonVariant> CODEC =
          StringRepresentable.fromEnum(PigeonVariant::values);
    private static final IntFunction<PigeonVariant> BY_ID =
          ByIdMap.continuous(PigeonVariant::getId, values(), ByIdMap.OutOfBoundsStrategy.CLAMP);

    public static final WeightedRandomList<WeightedEntry.Wrapper<PigeonVariant>> SERVICE_POOL = WeightedRandomList.create(
          new WeightedEntry.Wrapper<>(GRAY, Weight.of(16)),
          new WeightedEntry.Wrapper<>(BROWN, Weight.of(8)),
          new WeightedEntry.Wrapper<>(WHITE, Weight.of(4)),
          new WeightedEntry.Wrapper<>(PASSENGER, Weight.of(2))
    );

    public static final WeightedRandomList<WeightedEntry.Wrapper<PigeonVariant>> REGULAR_BIOME_POOL = WeightedRandomList.create(
          new WeightedEntry.Wrapper<>(GRAY, Weight.of(10)),
          new WeightedEntry.Wrapper<>(BROWN, Weight.of(4)),
          new WeightedEntry.Wrapper<>(WHITE, Weight.of(1))
    );

    public static final WeightedRandomList<WeightedEntry.Wrapper<PigeonVariant>> HAS_PASSENGER_PIGEON_BIOME_POOL = WeightedRandomList.create(
          new WeightedEntry.Wrapper<>(PASSENGER, Weight.of(16)),
          new WeightedEntry.Wrapper<>(GRAY, Weight.of(6)),
          new WeightedEntry.Wrapper<>(BROWN, Weight.of(4)),
          new WeightedEntry.Wrapper<>(WHITE, Weight.of(1))
    );

    private final int id;
    private final String name;

    PigeonVariant(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public int getId() {
        return this.id;
    }

    @Override
    public @NotNull String getSerializedName() {
        return this.name;
    }

    public static PigeonVariant byId(int id) {
        return BY_ID.apply(id);
    }

    // --

    public static PigeonVariant getRandomRegular(RandomSource random) {
        return REGULAR_BIOME_POOL.getRandom(random).orElseThrow().data();
    }

    public static PigeonVariant getRandomPassengerPriority(RandomSource random) {
        return HAS_PASSENGER_PIGEON_BIOME_POOL.getRandom(random).orElseThrow().data();
    }

    public static PigeonVariant getRandomService(RandomSource random) {
        return SERVICE_POOL.getRandom(random).orElseThrow().data();
    }
}
