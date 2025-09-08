package io.github.mortuusars.envelope.world.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.mail.Address;
import io.github.mortuusars.envelope.mail.log.MailDeliveryLog;
import io.github.mortuusars.envelope.mail.log.TravelingRecord;
import io.github.mortuusars.envelope.world.PigeonholeNetwork;
import io.github.mortuusars.envelope.world.Addresses;
import io.github.mortuusars.envelope.world.block.PigeonholeBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.NotImplementedException;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.function.Function;

public class BackgroundPigeon implements DeliveringPigeon {
    public static final Codec<BackgroundPigeon> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            CompoundTag.CODEC.fieldOf("tag").forGetter(BackgroundPigeon::getEntityTag),
            Delivery.CODEC.fieldOf("delivery_data").forGetter(BackgroundPigeon::getDelivery)
    ).apply(instance, BackgroundPigeon::new));

    protected final CompoundTag entityTag;
    protected Delivery delivery;

    public BackgroundPigeon(CompoundTag entityTag, Delivery delivery) {
        this.entityTag = entityTag;
        this.delivery = delivery;
    }

    public CompoundTag getEntityTag() {
        return entityTag;
    }

    public Delivery getDelivery() {
        return delivery;
    }

    @Override
    public void setDelivery(Delivery delivery) {
        this.delivery = delivery;
    }

    public boolean tickDelivery(ServerLevel level) {
        if (getDelivery().tick()) {
            return advancePhase(level);
        }
        return false;
    }

    public boolean advancePhase(ServerLevel level) {
        switch (getDelivery().getCurrentPhase().type()) {
            case LEAVING_HOME -> {
                nextDeliveryPhase()
                        .startAt(getDelivery().getHomePos().orElse(BlockPos.ZERO))
                        .endAt(Addresses.getPosition(level, getDelivery().getRecipient()).orElse(BlockPos.ZERO).above(16))
                        .duration(getDelivery().getTravelDuration())
                        .begin();
            }
            case TRAVELING_TO_TARGET -> {
                switch (getDelivery().getRecipient()) {
                    case Address.Pigeonhole pigeonhole -> {
                        Optional<PigeonholeNetwork.PigeonholeData> data = PigeonholeNetwork.get(level).getPigeonholeData(pigeonhole);
                        BlockPos endPos = Addresses.getPosition(level, getDelivery().getRecipient()).orElse(BlockPos.ZERO);
                        BlockPos startPos = endPos.above(16);

                        nextDeliveryPhase()
                                .startAt(startPos)
                                .endAt(endPos)
                                .begin();

                        return data
                                .flatMap(hole -> trySpawnNearby(level, hole.getPosition().above(16)))
                                .isPresent();
                    }
                    case Address.Player player -> {
                        throw new NotImplementedException("Player addresses are not implemented yet.");
                    }
                    case Address.Npc npc -> {
                        throw new NotImplementedException("NPC addresses are not implemented yet.");
                    }
                    default -> throw new IllegalStateException("Unexpected value: " + getDelivery().getRecipient());
                }
            }
            case APPROACHING_TARGET -> {
                nextDeliveryPhase()
                        .endAt(Addresses.getPosition(level, getDelivery().getRecipient()).orElse(BlockPos.ZERO).above(16))
                        .begin();

                ItemStack mail = getMail();
                if (mail.isEmpty()) return false;

                getDelivery().getRecipient()
                        .ifPigeonhole(pigeonhole -> {
                            PigeonholeNetwork pigeonholeNetwork = PigeonholeNetwork.get(level);
                            if (pigeonholeNetwork.putMail(pigeonhole, mail)) {
                                MailDeliveryLog.addRecords(mail, TravelingRecord.arrivedTo(pigeonhole));

                                pigeonholeNetwork.getPositionOf(pigeonhole).ifPresent(pos -> {
                                    if (level.isLoaded(pos) && level.getBlockEntity(pos) instanceof PigeonholeBlockEntity blockEntity) {
                                        blockEntity.onMailDelivered(level, mail);
                                    }
                                });

                                setMail(ItemStack.EMPTY);
                            } else {
                                MailDeliveryLog.addRecords(mail, TravelingRecord.returned(pigeonhole).atTime(level.getGameTime()));
                                MailDeliveryLog.addRecords(mail, TravelingRecord.travelingTo(getDelivery().getSender()));
                            }
                        })
                        .ifPlayer(player -> {
                            throw new NotImplementedException("Player addresses are not implemented yet");
                        })
                        .ifNpc(npc -> {
                            throw new NotImplementedException("NPC addresses are not implemented yet");
                        });
            }
            case LEAVING_TARGET -> {
                nextDeliveryPhase()
                        .endAt(getDelivery().getHomePos().orElse(BlockPos.ZERO).above(16))
                        .duration(getDelivery().getTravelDuration())
                        .begin();
            }
            case TRAVELING_TO_HOME -> {
                switch (getDelivery().getSender()) {
                    case Address.Pigeonhole pigeonhole -> {
                        Optional<PigeonholeNetwork.PigeonholeData> data = PigeonholeNetwork.get(level).getPigeonholeData(pigeonhole);
                        BlockPos endPos = Addresses.getPosition(level, getDelivery().getSender()).orElse(BlockPos.ZERO);
                        BlockPos startPos = endPos.above(16);

                        nextDeliveryPhase()
                                .startAt(startPos)
                                .endAt(endPos)
                                .begin();

                        return data
                                .flatMap(hole -> trySpawnNearby(level, hole.getPosition().above(16)))
                                .isPresent();
                    }
                    case Address.Player player -> {
                        throw new NotImplementedException("Player addresses are not implemented yet.");
                    }
                    case Address.Npc npc -> {
                        throw new NotImplementedException("NPC addresses are not implemented yet.");
                    }
                    default -> throw new IllegalStateException("Unexpected value: " + getDelivery().getRecipient());
                }
            }
            case APPROACHING_HOME -> {
                ItemStack mail = getMail();
                if (!mail.isEmpty()) {
                    getDelivery().getSender()
                            .ifPigeonhole(pigeonhole -> {
                                MailDeliveryLog.addRecords(mail, TravelingRecord.arrivedTo(getDelivery().getSender()));
                                PigeonholeNetwork pigeonholeNetwork = PigeonholeNetwork.get(level);

                                if (pigeonholeNetwork.putMail(pigeonhole, mail)) {
                                    pigeonholeNetwork.getPositionOf(pigeonhole).ifPresent(pos -> {
                                        if (level.isLoaded(pos) && level.getBlockEntity(pos) instanceof PigeonholeBlockEntity blockEntity) {
                                            blockEntity.onMailDelivered(level, mail);
                                        }
                                    });
                                }

                                //TODO: wait to return back the mail
                            })
                            .ifPlayer(player -> {
                                throw new IllegalStateException("Player senders should not be a thing.");
                            })
                            .ifNpc(npc -> {
                                throw new IllegalStateException("NPC sender return to home should not be handled by alive Pigeons.");
                            });
                }
                if (getDelivery().getHomePos().isPresent()) {
                    Envelope.LOGGER.warn("Waiting for chunk to load to spawn a pigeon.");
                }
                setDelivery(Delivery.EMPTY);
                return true;
            }
            default -> throw new IllegalStateException("Unexpected value: " + getDelivery().getCurrentPhase().type());
        }
        return false;
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
        Pigeon.IGNORED_TAGS.forEach(entityTag::remove);
        @Nullable Entity entity = EntityType.loadEntityRecursive(entityTag, level, Function.identity());
        return entity;
    }
}
