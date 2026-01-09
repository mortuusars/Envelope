package io.github.mortuusars.envelope.world.item.component;

import com.google.common.base.Preconditions;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import io.github.mortuusars.envelope.world.GameTime;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

public final class Id implements GameTime, Comparable<Id> {
    public static final Codec<Id> CODEC = Codec.STRING.comapFlatMap(Id::parse, Id::toString);
    public static final StreamCodec<ByteBuf, Id> STREAM_CODEC = StreamCodec.composite(
          ByteBufCodecs.VAR_LONG, Id::getTick,
          ByteBufCodecs.VAR_INT, Id::getSuffix,
          Id::new
    );

    private final long tick;
    private final int suffix;

    private Id(long tick, int suffix) {
        Preconditions.checkArgument(tick >= 0, "Tick must be >= 0. Value: " + tick);
        Preconditions.checkArgument(suffix >= 0, "Suffix must be >= 0. Value: " + suffix);
        this.tick = tick;
        this.suffix = suffix;
    }

    @Override
    public long get() {
        return getTick();
    }

    public long getTick() {
        return tick;
    }

    public int getSuffix() {
        return suffix;
    }

    @Override
    public String toString() {
        return suffix <= 0
              ? Long.toString(tick)
              : tick + "-" + suffix;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Id id = (Id) o;
        return tick == id.tick && suffix == id.suffix;
    }

    @Override
    public int hashCode() {
        return Objects.hash(tick, suffix);
    }

    @Override
    public int compareTo(@NotNull Id other) {
        Preconditions.checkNotNull(other);
        if (equals(other)) return 0;
        if (tick == other.tick) return Integer.compare(suffix, other.suffix);
        return Long.compare(tick, other.tick);
    }

    // --

    private static volatile long currentTick;
    private static final AtomicInteger currentSuffix = new AtomicInteger();

    /**
     * Creates new unique Id.
     * @param tick Current game tick.
     * @throws IllegalArgumentException if passed tick is lesser than last used tick. Use {@link Id#createUnsafe(long)} for arbitrary ticks.
     */
    public static Id create(long tick) {
        if (tick < currentTick) {
            throw new IllegalArgumentException("Cannot generate Id for non-current tick: " + tick + ". Current tick: " + currentTick);
        }
        if (tick != currentTick) {
            currentTick = tick;
            currentSuffix.set(0);
        }
        return new Id(tick, currentSuffix.getAndIncrement());
    }

    /**
     * Creates new unique Id.
     * @param time Current game time.
     * @throws IllegalArgumentException if passed tick is lesser than last used tick. Use {@link Id#createUnsafe(GameTime)} for arbitrary ticks.
     */
    public static Id create(GameTime time) {
        return create(time.get());
    }

    /**
     * Creates new unique Id.
     */
    public static Id create(Level level) {
        return create(level.getGameTime());
    }

    /**
     * Creates an id that is not guaranteed to be unique for a given tick.
     */
    public static Id createUnsafe(long tick, int suffix) {
        return new Id(tick, suffix);
    }

    /**
     * Creates an id that is not guaranteed to be unique for a given tick.
     */
    public static Id createUnsafe(long tick) {
        return createUnsafe(tick, 0);
    }

    /**
     * Creates an id that is not guaranteed to be unique for a given tick.
     */
    public static Id createUnsafe(GameTime time) {
        return createUnsafe(time.get());
    }

    // --

    public static DataResult<Id> parse(String input) {
        if (input == null || input.isBlank()) {
            return DataResult.error(() -> "Id string is null or empty");
        }

        String[] parts = input.split("-", -1);
        if (parts.length > 2) {
            return DataResult.error(() -> "Invalid Id format: " + input);
        }

        try {
            long tick = Long.parseLong(parts[0]);

            if (tick < 0) {
                return DataResult.error(() -> "Tick must be larger or equal to 0. Input: " + input);
            }

            int suffix = 0;
            if (parts.length == 2) {
                if (parts[1].isEmpty()) {
                    return DataResult.error(() -> "Suffix is empty in Id: " + input);
                }
                suffix = Integer.parseInt(parts[1]);
                if (suffix < 0) {
                    return DataResult.error(() -> "Suffix must be larger or equal to 0. Input: " + input);
                }
            }

            return DataResult.success(new Id(tick, suffix));
        } catch (NumberFormatException e) {
            return DataResult.error(() -> "Invalid number in Id: " + input);
        }
    }
}