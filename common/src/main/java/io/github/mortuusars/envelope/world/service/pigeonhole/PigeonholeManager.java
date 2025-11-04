package io.github.mortuusars.envelope.world.service.pigeonhole;

import com.mojang.logging.LogUtils;
import io.github.mortuusars.envelope.world.block.PigeonholeBlockEntity;
import io.github.mortuusars.envelope.world.mail.address.Address;
import io.github.mortuusars.envelope.world.mail.address.AddressUniquifier;
import io.github.mortuusars.envelope.world.mail.address.AllAddresses;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.*;

public class PigeonholeManager {
    protected static final Logger LOGGER = LogUtils.getLogger();

    private final ServerLevel level;
    private @Nullable PigeonholeRegistry data;

    public PigeonholeManager(ServerLevel level) {
        this.level = level;
    }

    protected @NotNull PigeonholeRegistry data() {
        if (data == null) {
            data = PigeonholeRegistry.get(level, "envelope_pigeonholes");
        }
        return data;
    }

    // -- Pigeonhole

    /**
     * Ensures that address is properly registered at current block position.<br>
     * If passed address is already in use elsewhere - it will be uniquified and registered properly.
     *
     * @return Correct address for this BlockPos.
     */
    public Address.Pigeonhole correctOrRegister(Address.Pigeonhole address, BlockPos pos) {
        return byPosition(pos)
              .map(PigeonholeData::getAddress) // Return correct (stored) address for that BlockPos
              .orElseGet(() -> {
                        AllAddresses knownAddresses = level.getEnvelopeContext().getKnownAddresses();
                        if (knownAddresses.isKnown(address)) {
                            AddressUniquifier uniquifier = new AddressUniquifier(knownAddresses, 22);
                            Address.Pigeonhole newAddress = new Address.Pigeonhole(uniquifier.uniquify(address.id()));
                            LOGGER.debug("Address '{}' is already registered. Pigeonhole @[{}] will have it's address changed to '{}' for uniqueness.",
                                  address.id(), pos.toShortString(), newAddress.id());
                            register(newAddress, pos);
                            return newAddress;
                        } else {
                            LOGGER.debug("Pigeonhole '{}'@[{}] is not registered. Registering it now.", address.id(), pos.toShortString());
                            register(address, pos);
                            return address;
                        }
                    }
              );
    }

    public boolean register(Address.Pigeonhole address, BlockPos pos) {
        @Nullable PigeonholeData existing = data().getPigeonholes().get(address);
        if (existing != null) {
            if (existing.getPos().equals(pos)) {
                LOGGER.warn("Trying to register same Pigeonhole '{}'@[{}] twice.", address.id(), pos.toShortString());
            } else {
                LOGGER.error("Cannot register new Pigeonhole '{}'@[{}]: it is already registered @[{}]",
                      address.id(), pos.toShortString(), existing.getPos().toShortString());
            }
            return false;
        }

        data().getPigeonholes().values().stream()
              .filter(pigeonhole -> pigeonhole.getPos().equals(pos))
              .toList()
              .forEach(pigeonhole -> {
                  LOGGER.warn("Removing Pigeonhole '{}'@[{}] because new Pigeonhole '{}' is being registered at the same blockpos.",
                        pigeonhole.getAddress().id(), pigeonhole.getPos().toShortString(), address.id());
                  data().getPigeonholes().remove(pigeonhole.getAddress());
              });

        data().getPigeonholes().put(address, new PigeonholeData(address, pos));
        LOGGER.debug("Registered new Pigeonhole '{}'@[{}]", address.id(), pos.toShortString());
        data().setDirty();
        return true;
    }

    public void remove(Address.Pigeonhole address) {
        @Nullable PigeonholeData removed = data().getPigeonholes().remove(address);
        if (removed != null) {
            level.getEnvelopeContext().getDefaultAddresses().remove(address);
            data().setDirty();
            LOGGER.debug("Removed Pigeonhole '{}'@[{}]", removed.getAddress().id(), removed.getPos().toShortString());
        }
    }

    public void rename(Address.Pigeonhole oldAddress, Address.Pigeonhole newAddress) {
        byAddress(oldAddress)
              .ifPresent(oldData -> {
                  level.getEnvelopeContext().getDefaultAddresses().get()
                        .entrySet()
                        .stream()
                        .filter(entry -> entry.getValue().equals(oldAddress))
                        .map(Map.Entry::getKey)
                        .toList()
                        .forEach(uuid -> level.getEnvelopeContext().getDefaultAddresses().set(uuid, newAddress));

                  PigeonholeData newData = new PigeonholeData(newAddress, oldData.getPos(), oldData.getMail());
                  oldData.invalidate();

                  data().getPigeonholes().remove(oldAddress);
                  data().getPigeonholes().put(newAddress, newData);
                  LOGGER.debug("Renamed Pigeonhole '{}'@[{}] to '{}'", oldData.getAddress().id(), oldData.getPos().toShortString(), newAddress.id());

                  data().setDirty();
              });
    }

    public boolean exists(Address.Pigeonhole pigeonhole) {
        return data().getPigeonholes().containsKey(pigeonhole);
    }

    public Set<Address.Pigeonhole> getAllAddresses() {
        return data().getPigeonholes().keySet();
    }

    // --

    public Optional<PigeonholeData> byAddress(Address.Pigeonhole address) {
        return Optional.ofNullable(data().getPigeonholes().get(address));
    }

    public Optional<PigeonholeData> byPosition(BlockPos pos) {
        for (PigeonholeData pigeonhole : data().getPigeonholes().values()) {
            if (pigeonhole.getPos().equals(pos)) {
                return Optional.of(pigeonhole);
            }
        }
        return Optional.empty();
    }

    public Optional<BlockPos> getPositionOf(Address.Pigeonhole address) {
        return byAddress(address).map(PigeonholeData::getPos);
    }

    public Optional<PigeonholeBlockEntity> getBlockEntityOf(Address.Pigeonhole address) {
        return getPositionOf(address)
              .flatMap(pos -> level.isLoaded(pos) && level.getBlockEntity(pos) instanceof PigeonholeBlockEntity blockEntity
                    ? Optional.of(blockEntity)
                    : Optional.empty());
    }
}