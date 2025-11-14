package io.github.mortuusars.envelope.world;

import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.world.mail.address.Address;
import net.minecraft.client.OptionInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.IntStream;

public class Position {
    public static Vec3 lerp(BlockPos origin, BlockPos target, double delta) {
        double x = origin.getX() + (target.getX() - origin.getX()) * delta;
        double y = origin.getY() + (target.getY() - origin.getY()) * delta;
        double z = origin.getZ() + (target.getZ() - origin.getZ()) * delta;
        return new Vec3(x, y, z);
    }

    public static Optional<BlockPos> ofAddress(ServerLevel level, Address address) {
        return address.map(
              pigeonhole -> level.getEnvelopeContext().getPigeonholeManager().getPositionOf(pigeonhole),
              player -> level.getEnvelopeContext().getKnownPlayers().getUuid(player)
                    .flatMap(uuid -> level.getEnvelopeContext().getDefaultAddresses().of(uuid))
                    .flatMap(pigeonholeAddress -> ofAddress(level, pigeonholeAddress)),
              entity -> Optional.empty());
    }

    public static BlockPos towardsDirection(BlockPos origin, BlockPos target, double distance) {
        Vec3 originVec = Vec3.atCenterOf(origin);
        Vec3 targetVec = Vec3.atCenterOf(target);
        Vec3 direction = targetVec.subtract(originVec).normalize();
        return BlockPos.containing(originVec.add(direction.scale(distance)));
    }

    public static BlockPos towardsHorizontalDirection(BlockPos origin, BlockPos target, double distance) {
        return towardsDirection(origin, target.atY(origin.getY()), distance);
    }

    public static BlockPos towardsRandomHorizontalDirection(BlockPos origin, int distance, int seed) {
        Random random = new Random(seed);
        random.nextLong(); // For some reason this fixes similar values returned for similar seeds. I'm not going to pretend I know why.
        double angle = random.nextDouble() * 2 * Math.PI;
        return origin.offset((int) (Math.cos(angle) * distance), 0, (int) (Math.sin(angle) * distance));
    }

    public static BlockPos ascendTowards(Level level, BlockPos origin, Optional<BlockPos> target, int distance, int seed) {
        BlockPos pos = target
              .map(recipientPos -> Position.towardsHorizontalDirection(origin, recipientPos, distance))
              .orElseGet(() -> Position.towardsRandomHorizontalDirection(origin, distance, seed))
              .above(distance);

        return aboveGround(level, pos, 5);
    }

    public static Optional<BlockPos> ascendTowards(Level level, Optional<BlockPos> origin,
                                                   Optional<BlockPos> target, int distance, int seed) {
        return origin.map(pos -> ascendTowards(level, pos, target, distance, seed));
    }

    @Deprecated
    public static boolean isInSafeSimulationDistance(ServerLevel level, BlockPos pos) {
        if (!level.isLoaded(pos)) {
            return false;
        }
        int simDistance = level.getServer().getPlayerList().getSimulationDistance();
        int range = simDistance - 1; // Reduce by 1 chunk to be safe.
        return level.players().stream().anyMatch(player -> {
            double dx = Math.abs(pos.getX() - player.getX()) / 16.0;
            double dz = Math.abs(pos.getZ() - player.getZ()) / 16.0;
            return Math.max(dx, dz) <= range;
        });
    }

    public static boolean isInSimulationDistance(ServerLevel level, ChunkPos chunkPos) {
        return level.getChunkSource().chunkMap.getDistanceManager().inEntityTickingRange(chunkPos.toLong());
    }

    public static boolean isInSimulationDistance(ServerLevel level, BlockPos pos) {
        return isInSimulationDistance(level, new ChunkPos(pos));
    }

    public static boolean isInSimulationDistance(ServerLevel level, Entity entity) {
        return isInSimulationDistance(level, entity.chunkPosition());
    }

    public static @Nullable BlockPos findNearbyHeightmapSpawnPosition(ServerLevel level, BlockPos pos, int altitude) {
        double lowestDistance = Double.MAX_VALUE;
        @Nullable BlockPos closestRandomPos = null;

        for (int i = 0; i < 5; i++) {
            BlockPos randomPos = aboveGround(level, level.getBlockRandomPos(pos.getX(), pos.getY(), pos.getZ(), 15), altitude);

            if (Position.isInSimulationDistance(level, randomPos)) {
                double distance = randomPos.distSqr(pos);

                if (distance < lowestDistance) {
                    lowestDistance = distance;
                    closestRandomPos = randomPos;
                }
            }
        }

        if (closestRandomPos != null) {
            return closestRandomPos;
        }

        BlockPos blockPos = aboveGround(level, pos, altitude);
        if (Position.isInSimulationDistance(level, blockPos)) {
            return blockPos;
        }

        return null;
    }

    public static BlockPos aboveGround(Level level, BlockPos pos, int altitude) {
        int heightmapY = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, pos).getY();
        int y = Math.max(pos.getY(), heightmapY + altitude);
        return pos.getY() == y ? pos : pos.atY(y);
    }
}
