package io.github.mortuusars.envelope.world;

import io.github.mortuusars.envelope.core.address.Address;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.NotImplementedException;

import java.util.Optional;

public class Position {
    public static Vec3 lerp(BlockPos origin, BlockPos target, double delta) {
        double x = origin.getX() + (target.getX() - origin.getX()) * delta;
        double y = origin.getY() + (target.getY() - origin.getY()) * delta;
        double z = origin.getZ() + (target.getZ() - origin.getZ()) * delta;
        return new Vec3(x, y, z);
    }

    public static Optional<BlockPos> ofAddress(ServerLevel level, Address address) {
        return address.map(
                pigeonhole -> level.getEnvelopePigeonholeManager().getPositionOf(pigeonhole),
                player -> {
                    throw new NotImplementedException("Player address is not implemented yet.");
                }, npc -> {
                    throw new NotImplementedException("NPC address is not implemented yet.");
                });
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

    public static BlockPos towardsRandomHorizontalDirection(BlockPos origin, RandomSource random, int distance) {
        return origin.relative(Direction.Plane.HORIZONTAL.getRandomDirection(random), distance);
    }

    public static BlockPos ascent(Level level, BlockPos origin, Optional<BlockPos> target, int distance) {
        BlockPos pos = target
                .map(recipientPos -> Position.towardsHorizontalDirection(origin, recipientPos, distance))
                .orElseGet(() -> Position.towardsRandomHorizontalDirection(origin, level.getRandom(), distance))
                .above(distance);

        int surface = level.getHeight(Heightmap.Types.MOTION_BLOCKING, pos.getX(), pos.getZ()) + 5;

        if (surface > pos.getY()) {
            pos = pos.atY(surface);
        }

        return pos;
    }

    public static BlockPos ascent(Level level, BlockPos origin, Optional<BlockPos> target) {
        return ascent(level, origin, target, 8);
    }
}
