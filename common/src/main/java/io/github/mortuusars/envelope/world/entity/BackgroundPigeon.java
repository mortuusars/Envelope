package io.github.mortuusars.envelope.world.entity;

import com.google.common.base.Preconditions;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.mail.log.MailDeliveryLog;
import io.github.mortuusars.envelope.mail.log.TravelingRecord;
import io.github.mortuusars.envelope.world.Position;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.function.Function;

public class BackgroundPigeon implements DeliveringPigeon {
    public static final Codec<BackgroundPigeon> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            CompoundTag.CODEC.fieldOf("tag").forGetter(BackgroundPigeon::getEntityTag),
            Delivery.CODEC.optionalFieldOf("delivery", null).forGetter(BackgroundPigeon::getDelivery)
    ).apply(instance, BackgroundPigeon::new));

    protected final CompoundTag entityTag;
    protected @Nullable Delivery delivery;
    protected boolean remove = false;

    public BackgroundPigeon(CompoundTag entityTag, @NotNull Delivery delivery) {
        this.entityTag = entityTag;
        this.delivery = delivery;
    }

    public CompoundTag getEntityTag() {
        return entityTag;
    }

    public @Nullable Delivery getDelivery() {
        return delivery;
    }

    @Override
    public void setDelivery(@Nullable Delivery delivery) {
        this.delivery = delivery;
    }

    public boolean shouldBeRemoved() {
        return remove;
    }

    @Override
    public Optional<BlockPos> getCurrentPos() {
        Preconditions.checkNotNull(getDelivery());
        return getDelivery().getPhase().estimateCurrentPos();
    }

    // --


    @Override
    public void tickDelivery(ServerLevel level) {
        if (getDelivery() == null) {
            remove = true;
            return;
        }
        DeliveringPigeon.super.tickDelivery(level);
    }

    @Override
    public void startDeliveryPhase(ServerLevel level) {
        Preconditions.checkNotNull(getDelivery());
        Envelope.LOGGER.info("BackgroundPigeon has started phase '{}'", getDelivery().getPhase().getType().getSerializedName());

        switch (getDelivery().getPhase().getType()) {
            case APPROACHING_TARGET -> {
                getDelivery().getRecipientPos()
                        .flatMap(pos -> trySpawnNearby(level, Position.ascent(level, pos, getDelivery().getSenderPos()), true))
                        .ifPresent(pigeon -> {
                            pigeon.startDeliveryPhase(level);
                            remove = true;
                        });
            }
            case APPROACHING_HOME -> {
                getDelivery().getSenderPos()
                        .flatMap(pos -> trySpawnNearby(level, Position.ascent(level, pos, getDelivery().getRecipientPos()), true))
                        .ifPresent(pigeon -> {
                            pigeon.startDeliveryPhase(level);
                            remove = true;
                        });
            }
        }
    }

    @Override
    public void endDeliveryPhase(ServerLevel level) {
        Preconditions.checkNotNull(getDelivery());
        switch (getDelivery().getPhase().getType()) {
            case APPROACHING_TARGET -> {
                ItemStack mail = getDelivery().getMail();
                if (mail.isEmpty()) return;

                if (tryDeliverMail(level, mail, getDelivery().getRecipient())) {
                    getDelivery().setMail(ItemStack.EMPTY);
                } else {
                    MailDeliveryLog.addRecords(mail,
                            TravelingRecord.returned(getDelivery().getRecipient()).atTime(level.getGameTime()),
                            TravelingRecord.travelingTo(getDelivery().getSender()));
                }
            }
            case APPROACHING_HOME -> {
                ItemStack mail = getDelivery().getMail();

                if (!mail.isEmpty() && tryDeliverMail(level, mail, getDelivery().getSender())) {
                    remove = true;
                }

                Envelope.LOGGER.error("Waiting for spawn is not implemented yet.");
            }
        }
    }

    public Optional<Pigeon> trySpawnNearby(ServerLevel level, BlockPos pos, boolean effects) {
        BlockPos blockPos = new BlockPos(
                pos.getX(),
                Math.max(pos.getY(), level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, pos).getY() + 5),
                pos.getZ());

        if (!level.isLoaded(blockPos)) {
            return Optional.empty();
        }

        Vec3 p = Vec3.atCenterOf(blockPos);
        @Nullable Entity entity = createEntity(level);
        if (entity instanceof Pigeon pigeon) {
            entity.moveTo(p.x(), p.y(), p.z(), entity.getYRot(), entity.getXRot());
            level.addFreshEntity(entity);

            if (effects) {
                level.sendParticles(ParticleTypes.CLOUD, p.x(), p.y(), p.z(), 16, 0.1, 0.1, 0.1, 0.05);
                level.playSound(null, p.x(), p.y(), p.z(),
                        SoundEvents.BUBBLE_COLUMN_BUBBLE_POP, SoundSource.NEUTRAL, 1, 1);
            }

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
