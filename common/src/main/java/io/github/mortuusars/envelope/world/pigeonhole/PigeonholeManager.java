package io.github.mortuusars.envelope.world.pigeonhole;

import com.mojang.logging.LogUtils;
import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.core.address.Address;
import io.github.mortuusars.envelope.util.result.Failure;
import io.github.mortuusars.envelope.util.result.Result;
import io.github.mortuusars.envelope.world.item.component.MailId;
import io.github.mortuusars.envelope.world.storage.PigeonholeSavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.*;

public class PigeonholeManager {
    protected static final Logger LOGGER = LogUtils.getLogger();

    private final ServerLevel level;

    public PigeonholeManager(ServerLevel level) {
        this.level = level;
    }

    // -- Pigeonhole

    public void register(Address.Pigeonhole address, BlockPos pos) {
        PigeonholeSavedData data = data();
        HashMap<Address.Pigeonhole, PigeonholeData> pigeonholes = data.getPigeonholes();

        @Nullable PigeonholeData existing = pigeonholes.get(address);
        if (existing != null) {
            if (existing.getPos().equals(pos)) {
                LOGGER.warn("Trying to register same Pigeonhole '{}'@[{}] twice.", address.id(), pos.toShortString());
            } else {
                LOGGER.error("Cannot register new Pigeonhole '{}'@[{}]: it is already registered @[{}]",
                        address.id(), pos.toShortString(), existing.getPos().toShortString());
            }
            return;
        }

        pigeonholes.values().stream()
                .filter(pigeonhole -> pigeonhole.getPos().equals(pos))
                .toList()
                .forEach(pigeonhole -> {
                    LOGGER.warn("Removing Pigeonhole '{}'@[{}] because new Pigeonhole '{}' is being registered at the same blockpos.",
                            pigeonhole.getAddress().id(), pigeonhole.getPos().toShortString(), address.id());
                    pigeonholes.remove(pigeonhole.getAddress());
                });

        pigeonholes.put(address, new PigeonholeData(address, pos));
        LOGGER.debug("Registered new Pigeonhole '{}'@[{}]", address.id(), pos.toShortString());
        data.setDirty();
    }

    public void remove(Address.Pigeonhole address) {
        PigeonholeSavedData data = data();
        @Nullable PigeonholeData removed = data.getPigeonholes().remove(address);
        if (removed != null) {
            level.getEnvelopePlayerInformation().getDefaultAddress().remove(address);
            data.setDirty();
            LOGGER.debug("Removed Pigeonhole '{}'@[{}]", removed.getAddress().id(), removed.getPos().toShortString());
        }
    }

    public boolean exists(Address.Pigeonhole pigeonhole) {
        return data().getPigeonholes().containsKey(pigeonhole);
    }

    public Set<Address.Pigeonhole> getAllAddresses() {
        return data().getPigeonholes().keySet();
    }

    public Optional<PigeonholeData> findByAddress(Address.Pigeonhole address) {
        return Optional.ofNullable(data().getPigeonholes().get(address));
    }

    public Optional<PigeonholeData> findByPosition(BlockPos pos) {
        for (PigeonholeData data : data().getPigeonholes().values()) {
            if (data.getPos().equals(pos)) {
                return Optional.of(data);
            }
        }
        return Optional.empty();
    }

    public Optional<BlockPos> getPositionOf(Address.Pigeonhole address) {
        return findByAddress(address).map(PigeonholeData::getPos);
    }

    public List<ItemStack> getAllMail(Address.Pigeonhole address) {
        return findByAddress(address).map(PigeonholeData::getMail).orElse(Collections.emptyList());
    }

    public boolean putMail(Address.Pigeonhole address, ItemStack mail) {
        return findByAddress(address)
                .map(data -> {
                    mail.set(Envelope.DataComponents.MAIL_ID, MailId.createRandom());
                    data.getMail().add(mail);
                    data().setDirty();
                    return true;
                })
                .orElse(false);
    }

    public Result<ItemStack> removeMailById(Address.Pigeonhole address, MailId id) {
        return findByAddress(address)
                .map(data -> {
                    @Nullable Result<ItemStack> result = null;
                    ListIterator<ItemStack> iterator = data.getMail().listIterator();
                    while (iterator.hasNext()) {
                        ItemStack mail = iterator.next();
                        if (id.matches(mail)) {
                            iterator.remove();
                            result = Result.success(mail);
                            data().setDirty();
                            break;
                        }
                    }

                    if (result == null) {
                        result = Result.failure(new Failure("Mail with mailId '" + id.toString() + "' is not found in pigeonhole '" + address + "'."));
                    }

                    return result;
                })
                .orElseGet(() -> Result.failure(new Failure("No mailbox with address '" + address + "' exists.")));
    }

    // --

    protected PigeonholeSavedData data() {
        return PigeonholeSavedData.get(level);
    }
}