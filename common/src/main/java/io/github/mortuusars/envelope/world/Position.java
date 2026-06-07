package io.github.mortuusars.envelope.world;

import io.github.mortuusars.envelope.integration.sable.MovingStructureCompat;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.Random;

public class Position {
    public static Vec3 lerp(BlockPos origin, BlockPos target, double delta) {
        double x = origin.getX() + (target.getX() - origin.getX()) * delta;
        double y = origin.getY() + (target.getY() - origin.getY()) * delta;
        double z = origin.getZ() + (target.getZ() - origin.getZ()) * delta;
        return new Vec3(x, y, z);
    }

    public static Vec3 lerp(Level level, BlockPos origin, BlockPos target, double delta) {
        return getGlobalCenter(level, origin).lerp(getGlobalCenter(level, target), delta);
    }

    public static BlockPos snapToGrid(BlockPos pos, int size) {
        int x = Math.round(pos.getX() / (float) size) * size;
        int z = Math.round(pos.getZ() / (float) size) * size;
        return new BlockPos(x, pos.getY(), z);
    }

    public static BlockPos nearestHub(BlockPos pos) {
        return snapToGrid(pos, 1024).atY(500);
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

    public static BlockPos ascendTowards(BlockPos origin, BlockPos target, int distance) {
        return Position.towardsHorizontalDirection(origin, target, distance)
              .above(distance);
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

    public static Vec3 getGlobalCenter(Level level, BlockPos localPos) {
        return MovingStructureCompat.getGlobalCenter(level, localPos);
    }

    public static BlockPos getNavigationPos(Level level, BlockPos localPos) {
        return MovingStructureCompat.getNavigationPos(level, localPos);
    }

    public static double distanceToSqr(Level level, BlockPos localPos, Vec3 entityPos) {
        return getGlobalCenter(level, localPos).distanceToSqr(entityPos);
    }

    /**
     * Whether an entity is within {@code distance} of a local block target.
     * Uses projected world-space distance first, then falls back to block-grid distance for flying entities.
     */
    public static boolean isWithinReach(Level level, BlockPos localPos, Vec3 entityPos, BlockPos entityBlockPos, double distance) {
        if (distanceToSqr(level, localPos, entityPos) < distance * distance) {
            return true;
        }
        return localPos.closerThan(entityBlockPos, distance);
    }

    public static boolean closerThan(Level level, BlockPos localPos, Vec3 entityPos, double distance) {
        return distanceToSqr(level, localPos, entityPos) < distance * distance;
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

    public static int getDistanceBetween(BlockPos a, BlockPos b) {
        return (int) Math.sqrt(a.distSqr(b));
    }

    public static int getDistanceBetween(Level level, BlockPos a, BlockPos b) {
        return (int) Math.sqrt(getGlobalCenter(level, a).distanceToSqr(getGlobalCenter(level, b)));
    }

    public static Optional<Integer> getDistanceBetween(Optional<BlockPos> a, Optional<BlockPos> b) {
        if (a.isEmpty() || b.isEmpty()) return Optional.empty();
        return Optional.of(getDistanceBetween(a.get(), b.get()));
    }

    public static boolean isFireNearby(Level level, BlockPos pos) {
        if (level == null) return false;

        for (BlockPos blockPos : BlockPos.betweenClosed(pos.offset(-1, -1, -1), pos.offset(1, 1, 1))) {
            if (level.getBlockState(blockPos).getBlock() instanceof FireBlock) {
                return true;
            }
        }

        return false;
    }
}
