package io.github.mortuusars.envelope.world.service.pigeonhole;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.mortuusars.envelope.world.mail.MailId;
import io.github.mortuusars.envelope.world.mail.Mail;
import io.github.mortuusars.envelope.world.mail.StoredMail;
import io.github.mortuusars.envelope.world.mail.address.Address;
import net.minecraft.core.BlockPos;

import java.util.*;

public class PigeonholeData {
    public static final Codec<PigeonholeData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
          Address.Pigeonhole.STRING_CODEC.fieldOf("address").forGetter(PigeonholeData::getAddress),
          BlockPos.CODEC.fieldOf("pos").forGetter(PigeonholeData::getPos),
          Codec.list(StoredMail.CODEC).fieldOf("mail").forGetter(PigeonholeData::getMail)
    ).apply(instance, PigeonholeData::new));

    private final Address.Pigeonhole address;
    private final BlockPos pos;
    private final List<StoredMail> mail;
    private boolean valid = true;
    private boolean dirty = true;

    public PigeonholeData(Address.Pigeonhole address, BlockPos pos, List<StoredMail> mail) {
        this.address = address;
        this.pos = pos;
        this.mail = new ArrayList<>(mail); // Make sure it's mutable
    }

    public PigeonholeData(Address.Pigeonhole address, BlockPos pos) {
        this(address, pos, Collections.emptyList());
    }

    public Address.Pigeonhole getAddress() {
        return address;
    }

    public BlockPos getPos() {
        return pos;
    }

    /**
     * This method is not suitable for outside modification (at least without calling {@link PigeonholeData#setDirty()})
     * Use dedicated methods to add/remove mail.
     */
    public List<StoredMail> getMail() {
        return mail;
    }

    public boolean hasMail() {
        return !getMail().isEmpty();
    }

    public void insertMail(Mail mail) {
        if (!mail.isEmpty()) {
            StoredMail storedMail = new StoredMail(mail.getItemCopy(), mail.getDeliveryLog(), MailId.createRandom());
            getMail().add(storedMail);
            setDirty();
        }
    }

    public Optional<StoredMail> extractMail(MailId id) {
        ListIterator<StoredMail> iterator = getMail().listIterator();
        while (iterator.hasNext()) {
            StoredMail mail = iterator.next();
            if (id.matches(mail.getId())) {
                iterator.remove();
                setDirty();
                return Optional.of(mail);
            }
        }

        return Optional.empty();
    }

    public List<Mail> extractAllMail() {
        if (!hasMail()) {
            return Collections.emptyList();
        }

        List<Mail> mail = new ArrayList<>(getMail());
        getMail().clear();
        setDirty();
        return mail;
    }

    // --

    public boolean stillValid() {
        return valid;
    }

    public void invalidate() {
        setValid(false);
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    // --

    public boolean isDirty() {
        return dirty;
    }

    public void setDirty() {
        setDirty(true);
    }

    public void setDirty(boolean dirty) {
        this.dirty = dirty;
    }
}