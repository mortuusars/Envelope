package io.github.mortuusars.envelope.world.pigeonhole;

import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.mail.Address;
import io.github.mortuusars.envelope.util.result.Failure;
import io.github.mortuusars.envelope.util.result.Result;
import io.github.mortuusars.envelope.mail.MailId;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class PigeonholeManager {
    private final ServerLevel level;

    public PigeonholeManager(ServerLevel level) {
        this.level = level;
    }

    // -- Pigeonhole

    public void registerOrUpdate(Address.Pigeonhole address, BlockPos pos) {
        findByAddress(address).ifPresentOrElse(
                pigeonhole -> {
                    if (!pigeonhole.getPos().equals(pos)) {
                        pigeonhole.setPos(pos);
                        data().setDirty();
                    }
                },
                () -> {
                    data().getPigeonholes().put(address, new PigeonholeData(address, pos));
                    data().setDirty();
                });
    }

    public void remove(Address.Pigeonhole address) {
        if (data().getPigeonholes().remove(address) != null) {
            data().getPlayerAddresses().entrySet().removeIf(entry -> entry.getValue().equals(address));
            data().setDirty();
        }
    }

    public boolean exists(Address.Pigeonhole pigeonhole) {
        return data().getPigeonholes().containsKey(pigeonhole);
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

    // -- Default Address

    public Optional<Address.Pigeonhole> getDefaultAddressOf(UUID uuid) {
        return Optional.ofNullable(data().getPlayerAddresses().get(uuid));
    }

    public void setDefaultAddressOf(UUID uuid, Address.Pigeonhole address) {
        data().getPlayerAddresses().put(uuid, address);
        data().setDirty();
    }

    // --

    protected PigeonholeSavedData data() {
        return PigeonholeSavedData.get(level);
    }
}