package io.github.mortuusars.envelope.world.mail.dropoff;

import io.github.mortuusars.envelope.world.item.mail.Mail;
import io.github.mortuusars.envelope.world.mail.MailService;
import io.github.mortuusars.envelope.world.mail.address.Address;
import io.github.mortuusars.envelope.world.mail.delivery.Delivery;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

import java.util.function.Consumer;
import java.util.function.Function;

public class MailDropOffContext {
    private final MailService service;
    private final Address target;
    private final Delivery delivery;

    public MailDropOffContext(MailService service, Address target, Delivery delivery) {
        this.service = service;
        this.target = target;
        this.delivery = delivery;
    }

    public MailService getService() {
        return service;
    }

    public ServerLevel getLevel() {
        return getService().getLevel();
    }

    public Address getTarget() {
        return target;
    }

    public Delivery getDelivery() {
        return delivery;
    }

    public ItemStack getMail() {
        return getDelivery().getMail();
    }

    public boolean isReturned() {
        return Mail.isReturned(getMail());
    }

    // -- Data

    /**
     * Retrieves data from persistent storage by ID.
     * <br>
     * Modifying the tag is not enough for it to save:
     * call {@link MailDropOffContext#setPersistentData(ResourceLocation, CompoundTag)} to make sure it will be saved.
     */
    public CompoundTag getPersistentData(ResourceLocation id) {
        return MailDropOffHandlerSavedData.get(getLevel()).get(id);
    }

    /**
     * Saves data in the persistent storage.
     */
    public void setPersistentData(ResourceLocation id, CompoundTag data) {
        MailDropOffHandlerSavedData.get(getLevel()).set(id, data);
    }

    public void readPersistentData(ResourceLocation id, Consumer<CompoundTag> consumer) {
        consumer.accept(getPersistentData(id));
    }

    public void modifyPersistentData(ResourceLocation id, Consumer<CompoundTag> consumer) {
        CompoundTag data = getPersistentData(id);
        consumer.accept(data);
        setPersistentData(id, data);
    }

    public void usePersistentData(ResourceLocation id, Function<CompoundTag, Boolean> consumer) {
        CompoundTag data = getPersistentData(id);
        if (consumer.apply(data)) {
            setPersistentData(id, data);
        }
    }
}
