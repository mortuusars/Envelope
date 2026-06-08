package io.github.mortuusars.envelope.integration.sable;

import io.github.mortuusars.envelope.world.Position;
import io.github.mortuusars.envelope.world.block.PigeonholeBlockEntity;
import io.github.mortuusars.envelope.world.block.PigeonholeRegistry;
import io.github.mortuusars.envelope.world.block.mailbox.MailboxBlockEntity;
import io.github.mortuusars.envelope.world.block.mailbox.Mailboxes;
import io.github.mortuusars.envelope.world.mail.MailService;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * Target discovery for blocks on Sable moving structures, where vanilla POI lookup does not apply.
 */
public final class ContraptionTargets {
    private ContraptionTargets() {
    }

    /**
     * Sorts POI results by projected distance, optionally merging in contraption-local targets when Sable is loaded.
     */
    public static List<BlockPos> locateNearby(ServerLevel level, Vec3 entityPos, int radius,
                                              List<BlockPos> poiResults,
                                              Supplier<List<BlockPos>> contraptionResults) {
        if (MovingStructureCompat.isAvailable()) {
            return mergeWithContraptionTargets(level, entityPos, radius, poiResults, contraptionResults.get())
                  .toList();
        }

        return sortByProjectedDistance(level, entityPos, poiResults.stream()).toList();
    }

    /**
     * O(n) over all registered mailbox addresses — acceptable while mailbox counts stay small.
     * {@link ServerLevel#getBlockEntity(BlockPos)} at plot-local positions relies on Sable's level hooks.
     */
    public static List<BlockPos> findNearbyMailboxes(ServerLevel level, Vec3 entityPos, int radius,
                                                     Predicate<MailboxBlockEntity> filter) {
        int radiusSqr = radius * radius;
        Mailboxes mailboxes = MailService.of(level).getMailboxes();

        return mailboxes.getAllAddresses().stream()
              .map(mailboxes::getPositionOf)
              .flatMap(Optional::stream)
              .filter(pos -> Position.distanceToSqr(level, pos, entityPos) <= radiusSqr)
              .filter(pos -> level.getBlockEntity(pos) instanceof MailboxBlockEntity mailbox
                    && filter.test(mailbox))
              .sorted(Comparator.comparingDouble(pos -> Position.distanceToSqr(level, pos, entityPos)))
              .toList();
    }

    /**
     * O(n) over {@link PigeonholeRegistry} entries for the level.
     * {@link ServerLevel#getBlockEntity(BlockPos)} at plot-local positions relies on Sable's level hooks.
     */
    public static List<BlockPos> findNearbyPigeonholes(ServerLevel level, Vec3 entityPos, int radius,
                                                       Predicate<PigeonholeBlockEntity> filter) {
        int radiusSqr = radius * radius;

        return PigeonholeRegistry.getAll(level).stream()
              .filter(pos -> Position.distanceToSqr(level, pos, entityPos) <= radiusSqr)
              .filter(pos -> level.getBlockEntity(pos) instanceof PigeonholeBlockEntity pigeonhole
                    && filter.test(pigeonhole))
              .sorted(Comparator.comparingDouble(pos -> Position.distanceToSqr(level, pos, entityPos)))
              .toList();
    }

    public static Stream<BlockPos> mergeWithContraptionTargets(ServerLevel level, Vec3 entityPos, int radius,
                                                               List<BlockPos> poiResults,
                                                               List<BlockPos> contraptionResults) {
        int radiusSqr = radius * radius;

        return sortByProjectedDistance(level, entityPos,
              Stream.concat(poiResults.stream(), contraptionResults.stream())
                    .distinct()
                    .filter(pos -> Position.distanceToSqr(level, pos, entityPos) <= radiusSqr));
    }

    private static Stream<BlockPos> sortByProjectedDistance(ServerLevel level, Vec3 entityPos, Stream<BlockPos> positions) {
        return positions.sorted(Comparator.comparingDouble(pos -> Position.distanceToSqr(level, pos, entityPos)));
    }
}
