package io.github.mortuusars.envelope.world.service.pigeonhole;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.world.item.component.MailId;
import io.github.mortuusars.envelope.world.mail.address.Address;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;

public class PigeonholeData {
    public static final Codec<PigeonholeData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
          Address.Pigeonhole.STRING_CODEC.fieldOf("address").forGetter(PigeonholeData::getAddress),
          BlockPos.CODEC.fieldOf("pos").forGetter(PigeonholeData::getPos),
          Codec.list(ItemStack.CODEC).fieldOf("mail").forGetter(PigeonholeData::getMail)
    ).apply(instance, PigeonholeData::new));

    private final Address.Pigeonhole address;
    private final BlockPos pos;
    private final List<ItemStack> mail;
    private boolean valid = true;
    private boolean dirty = true;

    public PigeonholeData(Address.Pigeonhole address, BlockPos pos, List<ItemStack> mail) {
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
    public List<ItemStack> getMail() {
        return mail;
    }

    public boolean hasMail() {
        return !getMail().isEmpty();
    }

    public void insertMail(ItemStack mail) {
        if (!mail.isEmpty()) {
            if (!mail.has(Envelope.DataComponents.MAIL_ID)) {
                mail.set(Envelope.DataComponents.MAIL_ID, MailId.createRandom());
            }
            getMail().add(mail);
            setDirty();
        }
    }

    public ItemStack extractMail(MailId id) {
        ItemStack result = ItemStack.EMPTY;

        ListIterator<ItemStack> iterator = getMail().listIterator();
        while (iterator.hasNext()) {
            ItemStack mail = iterator.next();
            if (id.matches(mail)) {
                iterator.remove();
                setDirty();
                result = mail;
                break;
            }
        }

        return result;
    }

    public List<ItemStack> extractAllMail() {
        if (!hasMail()) {
            return Collections.emptyList();
        }

        List<ItemStack> mail = new ArrayList<>(getMail());
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