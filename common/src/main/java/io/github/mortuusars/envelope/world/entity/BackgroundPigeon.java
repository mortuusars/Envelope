package io.github.mortuusars.envelope.world.entity;

import com.google.common.base.Preconditions;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.mortuusars.envelope.Config;
import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.mail.Address;
import io.github.mortuusars.envelope.mail.Mail;
import io.github.mortuusars.envelope.mail.log.MailTravelingLog;
import io.github.mortuusars.envelope.mail.log.TravelingRecord;
import io.github.mortuusars.envelope.network.Packets;
import io.github.mortuusars.envelope.network.packet.clientbound.PigeonholeHasNewMailS2CP;
import io.github.mortuusars.envelope.world.PigeonholeNetwork;
import io.github.mortuusars.envelope.world.Addresses;
import io.github.mortuusars.envelope.world.inventory.PigeonholeMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.NotImplementedException;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.function.Function;

public class BackgroundPigeon {
    public static final Codec<BackgroundPigeon> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            CompoundTag.CODEC.fieldOf("tag").forGetter(BackgroundPigeon::getEntityTag),
            PigeonDelivery.CODEC.fieldOf("delivery_data").forGetter(BackgroundPigeon::getDelivery)
    ).apply(instance, BackgroundPigeon::new));

    protected final CompoundTag entityTag;
    protected PigeonDelivery delivery;

    public BackgroundPigeon(CompoundTag entityTag, PigeonDelivery delivery) {
        this.entityTag = entityTag;
        this.delivery = delivery;
    }

    public CompoundTag getEntityTag() {
        return entityTag;
    }

    public PigeonDelivery getDelivery() {
        return delivery;
    }

    public boolean tickDelivery(ServerLevel level) {
        if (getDelivery().tick()) {
            return advancePhase(level);
        }
        return false;
    }

    public boolean advancePhase(ServerLevel level) {
        switch (delivery.getCurrentPhase()) {
            case LEFT_HOME -> {
                delivery.advancePhase()
                        .setPhaseEndPos(Addresses.getPosition(level, delivery.getRecipient())
                                .map(pos -> pos.above(32))
                                .orElse(new BlockPos(0, level.getMaxBuildHeight(), 0)));
                return false;
            }
            case TRAVELING_TO_TARGET -> {
                if (delivery.getRecipient() instanceof Address.Pigeonhole pigeonhole) {
                    delivery.advancePhase()
                            .setPhaseEndPos(Addresses.getPosition(level, delivery.getRecipient()).orElse(BlockPos.ZERO));
                    return PigeonholeNetwork.get(level).getPigeonholeData(pigeonhole)
                            .flatMap(hole -> trySpawnNearby(level, hole.getPos()))
                            .isPresent();
                } else {
                    throw new NotImplementedException("NPC mail is not implemented yet.");
                }
            }
            case APPROACHING_TARGET -> {
                ItemStack mail = delivery.getMail();
                if (!mail.isEmpty()) {
                    if (delivery.getRecipient() instanceof Address.Pigeonhole pigeonhole) {
                        boolean delivered = PigeonholeNetwork.get(level).putMail(pigeonhole, mail);
                        if (delivered) {
                            for (ServerPlayer player : level.players()) {
                                if (player.containerMenu instanceof PigeonholeMenu menu
                                        && menu.getBlockEntity().getAddress().map(a -> a.equals(delivery.getRecipient())).orElse(false)) {
                                    Packets.sendToClient(PigeonholeHasNewMailS2CP.INSTANCE, player);
                                }
                            }
                            MailTravelingLog.addRecords(mail, TravelingRecord.arrivedTo(delivery.getRecipient()));
                            delivery.setMail(ItemStack.EMPTY);
                        } else {
                            returnMail(level, mail, delivery.getHomePos());
                        }
                    } else {
                        throw new NotImplementedException("NPC mail is not implemented yet.");
                    }
                }

                if (delivery.getHomePos() == null) {
                    return true; // Despawn pigeon with no home
                }

                delivery.advancePhase()
                        .setPhaseStartPos(BlockPos.ZERO)
                        .setPhaseEndPos(BlockPos.ZERO.above(32));
                return false;
            }
            case LEFT_TARGET -> {
                delivery.advancePhase()
                        .setPhaseStartPos(BlockPos.ZERO)
                        .setPhaseEndPos(Optional.ofNullable(delivery.getHomePos()).orElse(new BlockPos(0, level.getMaxBuildHeight(), 0)));
                return false;
            }
            case TRAVELING_TO_HOME -> {
                if (delivery.getHomePos() == null) {
                    return true; // Despawn pigeon with no home
                }

                delivery.advancePhase()
                        .setPhaseEndPos(delivery.getHomePos());

                return trySpawnNearby(level, delivery.getHomePos()).isPresent();
            }
            case APPROACHING_HOME -> {
                if (delivery.getHomePos() == null) {
                    return true; // Despawn pigeon with no home
                }

                if (!level.isLoaded(delivery.getHomePos())) {
                    //TODO: add pigeon to "Spawning" waiting list.
                    return true;
                }

                ItemStack mail = delivery.getMail();
                if (!mail.isEmpty()) {
                    if (delivery.getRecipient() instanceof Address.Pigeonhole pigeonhole) {
                        boolean delivered = PigeonholeNetwork.get(level).putMail(pigeonhole, mail);
                        if (delivered) {
                            for (ServerPlayer player : level.players()) {
                                if (player.containerMenu instanceof PigeonholeMenu menu
                                        && menu.getBlockEntity().getAddress().map(a -> a.equals(delivery.getRecipient())).orElse(false)) {
                                    Packets.sendToClient(PigeonholeHasNewMailS2CP.INSTANCE, player);
                                }
                            }
                            MailTravelingLog.addRecords(mail, TravelingRecord.arrivedTo(delivery.getRecipient()));
                            delivery.setMail(ItemStack.EMPTY);
                        } else {
                            Containers.dropItemStack(level, delivery.getHomePos().getX(), delivery.getHomePos().getY(), delivery.getHomePos().getZ(), mail);
                            delivery.setMail(ItemStack.EMPTY);
                        }
                    } else {
                        throw new NotImplementedException("NPC mail is not implemented yet.");
                    }
                }

                return trySpawnNearby(level, delivery.getHomePos()).isPresent();
            }
            default -> throw new IllegalStateException("Unexpected value: " + delivery.getCurrentPhase());
        }
    }

    private Optional<Pigeon> trySpawnNearby(ServerLevel level, BlockPos pos) {
        BlockPos blockPos = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, pos).above(8);

        if (!level.isLoaded(blockPos)) {
            return Optional.empty();
        }

        Vec3 p = Vec3.atCenterOf(blockPos);
        @Nullable Entity entity = createEntity(level);
        if (entity instanceof Pigeon pigeon) {
            entity.moveTo(p.x(), p.y(), p.z(), entity.getYRot(), entity.getXRot());
            level.addFreshEntity(entity);
            level.sendParticles(ParticleTypes.CLOUD, p.x(), p.y(), p.z(), 16, 0.1, 0.1, 0.1, 0.05);
            pigeon.setDelivery(delivery);
            return Optional.of(pigeon);
        }

        return Optional.empty();
    }

    public @Nullable Entity createEntity(ServerLevel level) {
        Pigeon.IGNORED_PIGEON_TAGS.forEach(entityTag::remove);
        @Nullable Entity entity = EntityType.loadEntityRecursive(entityTag, level, Function.identity());
        return entity;
    }

    protected void returnMail(ServerLevel level, ItemStack mail, @Nullable BlockPos homePos) {
        Preconditions.checkState(!delivery.isEmpty(), "Cannot advance delivery phase: Pigeon is not delivering.");

        Mail.addReturnData(level, mail);

        Address sender = mail.get(Envelope.DataComponents.MAIL_SENDER);
        Address recipient = mail.get(Envelope.DataComponents.MAIL_RECIPIENT);
        Integer travelDuration = mail.getOrDefault(Envelope.DataComponents.MAIL_TRAVEL_DURATION, Config.Server.TRAVEL_DURATION.get());

        delivery = PigeonDelivery.start(homePos, recipient, sender, travelDuration, mail)
                .setPhaseStartPos(BlockPos.ZERO)
                .setPhaseEndPos(BlockPos.ZERO.above(32)); //TODO: pos in the direction of address
    }
}
