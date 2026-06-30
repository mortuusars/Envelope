package io.github.mortuusars.envelope.world.block;

import io.github.mortuusars.envelope.Config;
import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.world.Position;
import io.github.mortuusars.envelope.world.block.occupiable.PigeonOccupiable;
import io.github.mortuusars.envelope.world.block.occupiable.Occupant;
import io.github.mortuusars.envelope.world.entity.Pigeon;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class PigeonholeBlockEntity extends BlockEntity implements PigeonOccupiable {
    protected List<Occupant.Mutable> occupants = new ArrayList<>();
    protected boolean registered;

    protected PigeonholeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    public PigeonholeBlockEntity(BlockPos pos, BlockState blockState) {
        this(Envelope.BlockEntityTypes.PIGEONHOLE.get(), pos, blockState);
    }

    // -- Events

    public void serverTick(ServerLevel level, BlockPos pos, BlockState state) {
        if (!registered) {
            PigeonholeRegistry.register(level, getBlockPos());
            registered = true;
        }
        tickOccupants(level, pos, state);
    }

    @Override
    public void clearRemoved() {
        super.clearRemoved();
        registered = false;
    }

    @Override
    public void setRemoved() {
        if (registered && level instanceof ServerLevel serverLevel) {
            PigeonholeRegistry.unregister(serverLevel, getBlockPos());
            registered = false;
        }
        super.setRemoved();
    }

    @Override
    public void setChanged() {
        if (Position.isFireNearby(level, getBlockPos())) {
            releaseAllOccupants(getLevel(), getBlockPos(), getBlockState(), ReleaseReason.EMERGENCY);
        }
        super.setChanged();
    }

    // -- Occupiable

    @Override
    public List<Occupant.Mutable> getOccupants() {
        return occupants;
    }

    @Override
    public void onOccupantReleased(Level level, Entity entity, ReleaseReason reason) {
        if (reason != ReleaseReason.EMERGENCY
              && getBlockState().getBlock() instanceof PigeonholeBlock block
              && level.random.nextDouble() < getWasteIncreaseChanceOnRelease(entity)) {
            block.addWaste(level, getBlockPos(), getBlockState());
            setChanged();
        }

        if (entity instanceof Pigeon pigeon) {
            pigeon.releasedFromPigeonhole(getBlockPos(), getBlockState(), reason);
        }
    }

    protected double getWasteIncreaseChanceOnRelease(Entity releasedEntity) {
        return releasedEntity instanceof Pigeon pigeon && pigeon.isTired()
              ? Config.Server.PIGEONHOLE_WASTE_INCREASE_CHANCE_AFTER_DELIVERY.get()
              : Config.Server.PIGEONHOLE_WASTE_INCREASE_CHANCE.get();
    }

    @Override
    public void onOccupantsChanged() {
        setChanged();
    }

    @Override
    public void tickOccupants(Level level, BlockPos pos, BlockState state) {
        if (!getOccupants().isEmpty()
              && (level.getGameTime() + pos.hashCode()) % 20 == 0
              && CampfireBlock.isSmokeyPos(level, pos)) {
            releaseAllOccupants(level, pos, state, ReleaseReason.EMERGENCY);
            if (level instanceof ServerLevel serverLevel) {
                serverLevel.players()
                      .stream()
                      .min(Comparator.comparingDouble(pl -> pos.distSqr(pl.blockPosition())))
                      .ifPresent(player -> {
                          // Triggering on the nearest player is not ideal, someone standing further can place campfire.
                          // But doing it properly seems to be too much hassle for simple joke advancement.
                          Envelope.CriteriaTriggers.SMOKE_PIGEONHOLE.get().trigger(player);
                      });
            }
        }

        PigeonOccupiable.super.tickOccupants(level, pos, state);
    }

    // -- Component

    @Override
    protected void applyImplicitComponents(BlockEntity.DataComponentInput componentInput) {
        super.applyImplicitComponents(componentInput);
        this.occupants.clear();
        List<Occupant> occupants = componentInput.getOrDefault(Envelope.DataComponents.PIGEONS, List.of());
        occupants.forEach(o -> getOccupants().add(o.toMutable()));
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        components.set(Envelope.DataComponents.PIGEONS, getImmutableOccupants());
    }

    @SuppressWarnings("deprecation")
    @Override
    public void removeComponentsFromTag(CompoundTag tag) {
        super.removeComponentsFromTag(tag);
        tag.remove(getSerializedOccupantsName());
    }

    // -- Save/Load

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        loadOccupiable(tag, registries);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        saveOccupiable(tag, registries);
    }

    // --

    public @NotNull Level getLevelOrThrow() {
        return Objects.requireNonNull(level);
    }

    public void playSound(SoundEvent soundEvent, float volume, float pitch) {
        if (level != null) {
            level.playSound(null, getBlockPos(), soundEvent, SoundSource.BLOCKS, volume, pitch);
        }
    }
}
