package io.github.mortuusars.envelope.world.block.mailbox;

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

public class Mailboxes {
    protected static final Logger LOGGER = LogUtils.getLogger();

    private final ServerLevel level;
    private @Nullable MailboxesSavedData data;

    public Mailboxes(ServerLevel level) {
        this.level = level;
    }

    protected @NotNull MailboxesSavedData data() {
        if (data == null) {
            data = MailboxesSavedData.get(level);
        }
        return data;
    }

    protected Map<Address.Block, RegisteredMailbox> getMailboxes() {
        return data().getMailboxes();
    }

    protected void setDirty() {
        data().setDirty();
    }

    public Set<Address.Block> getAllAddresses() {
        return getMailboxes().keySet();
    }

    /**
     * Ensures that address is properly registered at current block position.<br>
     * If suggestedAddress is already in use elsewhere - it will be uniquified.
     */
    public @NotNull Address.Block correctOrRegisterIfNeeded(Address.Block suggestedAddress, BlockPos pos) {
        @Nullable RegisteredMailbox registeredAtPos = getAtPosition(pos);
        if (registeredAtPos != null) {
            // inUseAsPlayerOrEntity check is to handle the case when new player joins,
            // or new mail entity is added with the same address id
            if (registeredAtPos.getAddress().equals(suggestedAddress) && !inUseAsPlayerOrEntity(registeredAtPos.getAddress())) {
                return suggestedAddress;
            }

            // Renaming will uniquify the address
            return rename(registeredAtPos, suggestedAddress).getAddress();
        }

        Address.Block address = uniquifyIfKnown(suggestedAddress);

        getMailboxes().entrySet().removeIf(entry -> {
            if (entry.getValue().getPos().equals(pos)) {
                LOGGER.info("Removing mailbox '{}'@[{}] because new mailbox '{}' is being registered at the same blockpos.",
                      entry.getValue().getAddress().id(), entry.getValue().getPos().toShortString(), address.id());
                return true;
            }
            return false;
        });

        RegisteredMailbox data = new RegisteredMailbox(address, pos);
        getMailboxes().put(address, data);
        if (Envelope.debug()) LOGGER.info("Registered new mailbox '{}'@[{}]", address.id(), pos.toShortString());
        setDirty();
        return address;
    }

    public void remove(Address.Block address) {
        @Nullable RegisteredMailbox removed = getMailboxes().remove(address);
        if (removed != null) {
            MailService.of(level).getPlayers().removeDefaultAddress(address);
            setDirty();
            if (Envelope.debug()) LOGGER.info("Removed mailbox '{}'@[{}]",
                  removed.getAddress().id(), removed.getPos().toShortString());
        }
    }

    public void remove(BlockPos pos) {
        Optional.ofNullable(getAtPosition(pos)).ifPresent(data -> remove(data.getAddress()));
    }

    public @NotNull RegisteredMailbox rename(RegisteredMailbox data, Address.Block suggestedAddress) {
        Address.Block newAddress = uniquifyIfKnown(suggestedAddress);

        MailService.of(level).getPlayers().renameDefaultAddress(data.getAddress(), newAddress);

        RegisteredMailbox newData = new RegisteredMailbox(newAddress, data.getPos());

        getMailboxes().remove(data.getAddress());
        getMailboxes().put(newAddress, newData);

        if (Envelope.debug()) {
            LOGGER.info("Renamed mailbox '{}'@[{}] to '{}'", data.getAddress().id(), data.getPos().toShortString(), newAddress.id());
        }

        setDirty();

        return newData;
    }

    public void rename(Address.Block oldAddress, Address.Block suggestedAddress) {
        getByAddress(oldAddress).ifPresent(data -> rename(data, suggestedAddress));
    }

    public boolean exists(Address.Block block) {
        return getMailboxes().containsKey(block);
    }

    // --

    public Optional<RegisteredMailbox> getByAddress(Address.Block address) {
        return Optional.ofNullable(getMailboxes().get(address));
    }

    public @Nullable RegisteredMailbox getAtPosition(BlockPos pos) {
        for (RegisteredMailbox mailbox : getMailboxes().values()) {
            if (mailbox.getPos().equals(pos)) {
                return mailbox;
            }
        }
        return null;
    }

    public Optional<BlockPos> getPositionOf(Address.Block address) {
        return getByAddress(address).map(RegisteredMailbox::getPos);
    }

    public Optional<PigeonholeBlockEntity> getBlockEntityOf(Address.Block address) {
        return getPositionOf(address)
              .flatMap(pos -> level.isLoaded(pos) && level.getBlockEntity(pos) instanceof PigeonholeBlockEntity blockEntity
                    ? Optional.of(blockEntity)
                    : Optional.empty());
    }

    // --

    private boolean inUseAsPlayerOrEntity(Address.Block address) {
        AllAddresses knownAddresses = MailService.of(level).getKnownAddresses();
        return knownAddresses.isKnownOfType(address, Address.Type.PLAYER)
              || knownAddresses.isKnownOfType(address, Address.Type.ENTITY);
    }

    private Address.Block uniquifyIfKnown(Address.Block address) {
        AllAddresses knownAddresses = MailService.of(level).getKnownAddresses();
        if (!knownAddresses.isKnown(address)) {
            return address;
        }
        AddressUniquifier uniquifier = new AddressUniquifier(knownAddresses);
        String uniqueId = uniquifier.uniquify(address.id());
        return new Address.Block(uniqueId);
    }
}