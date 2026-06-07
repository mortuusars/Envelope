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
import java.util.function.Predicate;
import java.util.stream.Stream;

public final class ContraptionTargets {
    private ContraptionTargets() {
    }

    public static List<BlockPos> findNearbyMailboxes(ServerLevel level, Vec3 entityPos, int radius,
                                                     Predicate<MailboxBlockEntity> filter) {
        double radiusSqr = radius * (double) radius;
        Mailboxes mailboxes = MailService.of(level).getMailboxes();

        return mailboxes.getAllAddresses().stream()
              .map(mailboxes::getPositionOf)
              .flatMap(optionalPos -> optionalPos.stream())
              .filter(pos -> level.getBlockEntity(pos) instanceof MailboxBlockEntity mailbox
                    && filter.test(mailbox))
              .filter(pos -> Position.distanceToSqr(level, pos, entityPos) <= radiusSqr)
              .sorted(Comparator.comparingDouble(pos -> Position.distanceToSqr(level, pos, entityPos)))
              .toList();
    }

    public static List<BlockPos> findNearbyPigeonholes(ServerLevel level, Vec3 entityPos, int radius,
                                                       Predicate<PigeonholeBlockEntity> filter) {
        double radiusSqr = radius * (double) radius;

        return PigeonholeRegistry.getAll(level).stream()
              .filter(pos -> level.getBlockEntity(pos) instanceof PigeonholeBlockEntity pigeonhole
                    && filter.test(pigeonhole))
              .filter(pos -> Position.distanceToSqr(level, pos, entityPos) <= radiusSqr)
              .sorted(Comparator.comparingDouble(pos -> Position.distanceToSqr(level, pos, entityPos)))
              .toList();
    }

    public static Stream<BlockPos> mergeWithContraptionTargets(ServerLevel level, Vec3 entityPos, int radius,
                                                               List<BlockPos> poiResults,
                                                               List<BlockPos> contraptionResults) {
        double radiusSqr = radius * (double) radius;

        return Stream.concat(poiResults.stream(), contraptionResults.stream())
              .distinct()
              .filter(pos -> Position.distanceToSqr(level, pos, entityPos) <= radiusSqr)
              .sorted(Comparator.comparingDouble(pos -> Position.distanceToSqr(level, pos, entityPos)));
    }
}
