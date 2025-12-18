package io.github.mortuusars.envelope.world.service.pigeonhole;

import com.mojang.logging.LogUtils;
import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.world.block.PigeonholeBlockEntity;
import io.github.mortuusars.envelope.world.mail.address.Address;
import io.github.mortuusars.envelope.world.mail.address.AddressUniquifier;
import io.github.mortuusars.envelope.world.mail.address.AllAddresses;
import io.github.mortuusars.envelope.world.service.MailService;
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

    protected Map<Address.Block, PigeonholeData> getPigeonholes() {
        return data().getPigeonholes();
    }

    protected void setDirty() {
        data().setDirty();
    }

    // -- Pigeonhole

    public Set<Address.Block> getAllAddresses() {
        return getPigeonholes().keySet();
    }

    /**
     * Ensures that address is properly registered at current block position.<br>
     * If suggestedAddress is already in use elsewhere - it will be uniquified and registered properly.
     */
    public @NotNull PigeonholeData getOrRegister(Address.Block suggestedAddress, BlockPos pos) {
        @Nullable PigeonholeData dataAtPos = getDataAt(pos);
        if (dataAtPos != null) {
            // inUseAsPlayerOrEntity check is to handle the case when new player joins, or new mail entity is added with the same address id
            if (dataAtPos.getAddress().equals(suggestedAddress) && !inUseAsPlayerOrEntity(dataAtPos.getAddress())) {
                dataAtPos.setValid(true);
                return dataAtPos;
            }

            return rename(dataAtPos, suggestedAddress);
        }

        Address.Block address = uniquifyIfKnown(suggestedAddress);

        getPigeonholes().entrySet().removeIf(entry -> {
            if (entry.getValue().getPos().equals(pos)) {
                LOGGER.warn("Removing registered Pigeonhole '{}'@[{}] because new Pigeonhole '{}' is being registered at the same blockpos.",
                      entry.getValue().getAddress().id(), entry.getValue().getPos().toShortString(), address.id());
                return true;
            }
            return false;
        });

        PigeonholeData data = new PigeonholeData(address, pos);
        getPigeonholes().put(address, data);
        if (Envelope.debug()) LOGGER.info("Registered new Pigeonhole '{}'@[{}]", address.id(), pos.toShortString());
        setDirty();
        return data;
    }

    public void remove(Address.Block address) {
        @Nullable PigeonholeData removed = getPigeonholes().remove(address);
        if (removed != null) {
            removed.invalidate();
            MailService.of(level).getPlayers().removeDefaultAddress(address);
            setDirty();
            if (Envelope.debug()) LOGGER.info("Removed Pigeonhole '{}'@[{}]",
                  removed.getAddress().id(), removed.getPos().toShortString());
        }
    }

    public @NotNull PigeonholeData rename(PigeonholeData data, Address.Block suggestedAddress) {
        Address.Block newAddress = uniquifyIfKnown(suggestedAddress);

        MailService.of(level).getPlayers().renameDefaultAddress(data.getAddress(), newAddress);

        PigeonholeData newData = new PigeonholeData(newAddress, data.getPos(), data.getMail());

        data.invalidate(); // Force users to re-query

        getPigeonholes().remove(data.getAddress());
        getPigeonholes().put(newAddress, newData);

        if (Envelope.debug()) {
            LOGGER.info("Renamed Pigeonhole '{}'@[{}] to '{}'", data.getAddress().id(), data.getPos().toShortString(), newAddress.id());
        }

        setDirty();

        return newData;
    }

    public void rename(Address.Block oldAddress, Address.Block suggestedAddress) {
        getData(oldAddress).ifPresent(data -> rename(data, suggestedAddress));
    }

    public boolean exists(Address.Block block) {
        return getPigeonholes().containsKey(block);
    }

    // --

    public Optional<PigeonholeData> getData(Address.Block address) {
        @Nullable PigeonholeData value = getPigeonholes().get(address);
        if (value != null) {
            value.setValid(true);
        }
        return Optional.ofNullable(value);
    }

    public @Nullable PigeonholeData getDataAt(BlockPos pos) {
        for (PigeonholeData pigeonhole : getPigeonholes().values()) {
            if (pigeonhole.getPos().equals(pos)) {
                return pigeonhole;
            }
        }
        return null;
    }

    public Optional<BlockPos> getPositionOf(Address.Block address) {
        return getData(address).map(PigeonholeData::getPos);
    }

    public Optional<PigeonholeBlockEntity> getBlockEntityOf(Address.Block address) {
        return getPositionOf(address)
              .flatMap(pos -> level.isLoaded(pos) && level.getBlockEntity(pos) instanceof PigeonholeBlockEntity blockEntity
                    ? Optional.of(blockEntity)
                    : Optional.empty());
    }

    // --

    protected boolean inUseAsPlayerOrEntity(Address.Block address) {
        AllAddresses knownAddresses = MailService.of(level).getKnownAddresses();
        return knownAddresses.isKnownOfType(address, Address.Type.PLAYER)
              || knownAddresses.isKnownOfType(address, Address.Type.ENTITY);
    }

    protected Address.Block uniquifyIfKnown(Address.Block address) {
        AllAddresses knownAddresses = MailService.of(level).getKnownAddresses();
        if (!knownAddresses.isKnown(address)) {
            return address;
        }
        AddressUniquifier uniquifier = new AddressUniquifier(knownAddresses);
        String uniqueId = uniquifier.uniquify(address.id());
        return new Address.Block(uniqueId);
    }
}