package io.github.mortuusars.envelope.world.block.mailbox;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.mortuusars.envelope.world.mail.address.Address;
import net.minecraft.core.BlockPos;

public class RegisteredMailbox {
    public static final Codec<RegisteredMailbox> CODEC = RecordCodecBuilder.create(instance -> instance.group(
          Address.Block.STRING_CODEC.fieldOf("address").forGetter(RegisteredMailbox::getAddress),
          BlockPos.CODEC.fieldOf("pos").forGetter(RegisteredMailbox::getPos)
    ).apply(instance, RegisteredMailbox::new));

    private final Address.Block address;
    private final BlockPos pos;

    public RegisteredMailbox(Address.Block address, BlockPos pos) {
        this.address = address;
        this.pos = pos;
    }

    public Address.Block getAddress() {
        return address;
    }

    public BlockPos getPos() {
        return pos;
    }

//    public boolean hasMail() {
//        return !getMail().isEmpty();
//    }
//
//    public void insertMail(Mail mail) {
//        if (mail.getLog().getLastExceptionRecord().isEmpty()) {
//            //TODO: not the best place for it
//            mail = mail.asDeliveryResult();
//        }
//
//        //TODO: add overload to accept mail with known id. might be useful in some cases
//        if (!mail.isEmpty()) {
//            StoredMail storedMail = new StoredMail(MailId.createRandom(), mail.getItem().copy(), mail.getLog());
//            getMail().add(storedMail);
//            setDirty();
//        }
//    }
//
//    public Optional<StoredMail> extractMail(MailId id) {
//        ListIterator<StoredMail> iterator = getMail().listIterator();
//        while (iterator.hasNext()) {
//            StoredMail mail = iterator.next();
//            if (id.matches(mail.getId())) {
//                iterator.remove();
//                setDirty();
//                return Optional.of(mail);
//            }
//        }
//
//        return Optional.empty();
//    }
//
//    public List<StoredMail> extractAllMail() {
//        if (!hasMail()) {
//            return Collections.emptyList();
//        }
//
//        List<StoredMail> mail = new ArrayList<>(getMail());
//        getMail().clear();
//        setDirty();
//        return mail;
//    }

    // --

//    public boolean stillValid() {
//        return valid;
//    }
//
//    public void invalidate() {
//        setValid(false);
//    }
//
//    public void setValid(boolean valid) {
//        this.valid = valid;
//    }

    // --

//    public boolean isDirty() {
//        return dirty;
//    }
//
//    public void setDirty() {
//        setDirty(true);
//    }
//
//    public void setDirty(boolean dirty) {
//        this.dirty = dirty;
//    }
}