package io.github.mortuusars.envelope.world.mail.delivery;

import io.github.mortuusars.envelope.world.item.component.Id;
import io.github.mortuusars.envelope.world.mail.address.Address;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

public class DeliveryDraft {
    private @Nullable Id id = null;
    private @Nullable UUID owner = null;
    private Address sender = Address.UNKNOWN;
    private Address recipient = Address.UNKNOWN;
    private ItemStack mail = ItemStack.EMPTY;
    private DeliveryPhase phase = DeliveryPhase.STARTED;

    // --

    public DeliveryDraft deliver(ItemStack mail) {
        this.mail = mail;
        return this;
    }

    public DeliveryDraft from(Address sender) {
        this.sender = sender;
        return this;
    }

    public DeliveryDraft to(Address recipient) {
        this.recipient = recipient;
        return this;
    }

    public DeliveryDraft owner(@Nullable UUID owner) {
        this.owner = owner;
        return this;
    }

    public DeliveryDraft id(@Nullable Id id) {
        this.id = id;
        return this;
    }

    public DeliveryDraft owner(Player player) {
        this.owner = player.getUUID();
        return this;
    }

    public DeliveryDraft startAtPhase(DeliveryPhase phase) {
        this.phase = phase;
        return this;
    }

    // --


    public @NotNull Id getOrCreateId(Level level) {
        return id != null ? id : Id.create(level);
    }

    public Optional<UUID> getOwner() {
        return Optional.ofNullable(owner);
    }

    public Address getRecipient() {
        return recipient;
    }

    public Address getSender() {
        return sender;
    }

    public ItemStack getMail() {
        return mail;
    }

    public DeliveryPhase getPhase() {
        return phase;
    }

    // --

    @Override
    public String toString() {
        return "DeliveryDraft{" +
              "owner=" + owner +
              ", sender=" + sender +
              ", recipient=" + recipient +
              ", mail=" + mail +
              ", phase=" + phase +
              '}';
    }
}
