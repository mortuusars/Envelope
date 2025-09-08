package io.github.mortuusars.envelope.world;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.mail.Address;
import io.github.mortuusars.envelope.util.result.Failure;
import io.github.mortuusars.envelope.util.result.Result;
import io.github.mortuusars.envelope.mail.MailId;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class PigeonholeNetwork extends SavedData {
    protected static final String SAVED_DATA_NAME = "envelope_pigeonholes";

    public static final Codec<PigeonholeNetwork> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.unboundedMap(Address.Pigeonhole.CODEC_STRING, PigeonholeData.CODEC).fieldOf("pigeonholes").forGetter(PigeonholeNetwork::getPigeonholes)
    ).apply(instance, PigeonholeNetwork::new));

    protected Map<Address.Pigeonhole, PigeonholeData> pigeonholes;

    public PigeonholeNetwork(Map<Address.Pigeonhole, PigeonholeData> pigeonholes) {
        this.pigeonholes = new HashMap<>(pigeonholes);
    }

    public Map<Address.Pigeonhole, PigeonholeData> getPigeonholes() {
        return pigeonholes;
    }

    public void addOrUpdate(Address.Pigeonhole address, BlockPos pos) {
        getPigeonholeData(address).ifPresentOrElse(
                existingData -> {
                    if (!existingData.getPosition().equals(pos)) {
                        existingData.setPos(pos);
                        setDirty();
                    }
                },
                () -> {
                    pigeonholes.put(address, new PigeonholeData(pos));
                    setDirty();
                });
    }

    public void remove(Address.Pigeonhole address) {
        if (pigeonholes.remove(address) != null) {
            setDirty();
        }
    }

    public boolean exists(Address.Pigeonhole address) {
        return pigeonholes.containsKey(address);
    }

    // --

    public List<ItemStack> getAllMail(Address.Pigeonhole address) {
        return getPigeonholeData(address).map(PigeonholeData::getMail).orElse(Collections.emptyList());
    }

    public boolean putMail(Address.Pigeonhole address, ItemStack mail) {
        return getPigeonholeData(address)
                .map(data -> {
                    mail.set(Envelope.DataComponents.MAIL_ID, MailId.createRandom());
                    data.getMail().add(mail);
                    setDirty();
                    return true;
                })
                .orElse(false);
    }

    public Result<ItemStack> removeMailById(Address.Pigeonhole address, MailId id) {
        return getPigeonholeData(address)
                .map(data -> {
                    @Nullable Result<ItemStack> result = null;
                    ListIterator<ItemStack> iterator = data.getMail().listIterator();
                    while (iterator.hasNext()) {
                        ItemStack mail = iterator.next();
                        if (id.matches(mail)) {
                            iterator.remove();
                            result = Result.success(mail);
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

    public Optional<PigeonholeData> getPigeonholeData(Address.Pigeonhole address) {
        return Optional.ofNullable(pigeonholes.get(address));
    }

    public Optional<BlockPos> getPositionOf(Address.Pigeonhole address) {
        return getPigeonholeData(address).map(PigeonholeData::getPosition);
    }

    // --

    public static PigeonholeNetwork get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(factory(), SAVED_DATA_NAME);
    }

    public @NotNull CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        CompoundTag tag1 = CODEC.encodeStart(NbtOps.INSTANCE, this)
                .ifError(e -> Envelope.LOGGER.error("Cannot save PigeonholeNetwork: {}", e.message()))
                .result()
                .filter(t -> t instanceof CompoundTag)
                .map(t -> ((CompoundTag) t))
                .orElse(tag);
        return tag1;
    }

    private static Factory<PigeonholeNetwork> factory() {
        return new Factory<>(PigeonholeNetwork::createEmpty, PigeonholeNetwork::loadFromTag, null);
    }

    private static PigeonholeNetwork createEmpty() {
        return new PigeonholeNetwork(new HashMap<>());
    }

    private static PigeonholeNetwork loadFromTag(CompoundTag tag, HolderLookup.Provider registries) {
        return CODEC.decode(NbtOps.INSTANCE, tag)
                .ifError(e -> Envelope.LOGGER.error("Cannot load PigeonholeNetwork: {}", e.message()))
                .result().map(Pair::getFirst).orElse(createEmpty());
    }

    // --

    public static class PigeonholeData {
        public static final Codec<PigeonholeData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                BlockPos.CODEC.fieldOf("pos").forGetter(PigeonholeData::getPosition),
                Codec.list(ItemStack.CODEC).fieldOf("mail").forGetter(PigeonholeData::getMail)
        ).apply(instance, PigeonholeData::new));

        protected BlockPos pos;
        protected List<ItemStack> mail;

        protected PigeonholeData(BlockPos pos, List<ItemStack> mail) {
            this.pos = pos;
            this.mail = new ArrayList<>(mail);
        }

        public PigeonholeData(BlockPos pos) {
            this(pos, new ArrayList<>());
        }

        public BlockPos getPosition() {
            return pos;
        }

        public void setPos(BlockPos pos) {
            this.pos = pos;
        }

        public List<ItemStack> getMail() {
            return mail;
        }

        public void setMail(List<ItemStack> mail) {
            this.mail = mail;
        }
    }
}