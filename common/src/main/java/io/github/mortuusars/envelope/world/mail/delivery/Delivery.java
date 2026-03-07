package io.github.mortuusars.envelope.world.mail.delivery;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.mortuusars.envelope.world.item.component.Id;
import io.github.mortuusars.envelope.world.mail.address.Address;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;
import java.util.UUID;

public class Delivery {
    public static final Codec<Delivery> CODEC = RecordCodecBuilder.create(i -> i.group(
          Id.CODEC.fieldOf("id").forGetter(Delivery::getId),
          UUIDUtil.CODEC.optionalFieldOf("owner").forGetter(Delivery::getOwner),
          Address.CODEC.fieldOf("sender").forGetter(Delivery::getSender),
          Address.CODEC.fieldOf("recipient").forGetter(Delivery::getRecipient),
          ItemStack.OPTIONAL_CODEC.optionalFieldOf("mail", ItemStack.EMPTY).forGetter(Delivery::getMail),
          DeliveryRoute.CODEC.optionalFieldOf("route", DeliveryRoute.EMPTY).forGetter(Delivery::getRoute),
          DeliveryPhase.CODEC.optionalFieldOf("phase", DeliveryPhase.STARTED).forGetter(Delivery::getPhase),
          Codec.intRange(0, Integer.MAX_VALUE).optionalFieldOf("phase_progress", 0).forGetter(Delivery::getPhaseProgress),
          Codec.BOOL.optionalFieldOf("ended", false).forGetter(Delivery::isEnded)
    ).apply(i, Delivery::new));

    private final Id id;
    private final Optional<UUID> owner;
    private final Address sender;
    private final Address recipient;
    private ItemStack mail;
    private DeliveryRoute route;
    private DeliveryPhase phase;
    private int phaseProgress;
    private boolean ended;

    public Delivery(Id id, Optional<UUID> owner, Address sender, Address recipient, ItemStack mail,
                    DeliveryRoute route, DeliveryPhase phase, int phaseProgress, boolean ended) {
        this.id = id;
        this.owner = owner;
        this.sender = sender;
        this.recipient = recipient;
        this.mail = mail;
        this.route = route;
        this.phase = phase;
        this.phaseProgress = phaseProgress;
        this.ended = ended;
    }

    public static DeliveryDraft draft() {
        return new DeliveryDraft();
    }

    // --

    public Id getId() {
        return id;
    }

    public Optional<UUID> getOwner() {
        return owner;
    }

    public Address getSender() {
        return sender;
    }

    public Address getRecipient() {
        return recipient;
    }

    public ItemStack getMail() {
        return mail;
    }

    public Delivery setMail(ItemStack mail) {
        this.mail = mail;
        return this;
    }

    public DeliveryRoute getRoute() {
        return route;
    }

    public void setRoute(DeliveryRoute route) {
        this.route = route;
    }

    public void updateRoute(ServerLevel level) {
        setRoute(DeliveryRoute.build(level, getSender(), getRecipient()));
    }

    public DeliveryPhase getPhase() {
        return phase;
    }

    public void setPhase(DeliveryPhase phase, int progress) {
        this.phase = phase;
        setPhaseProgress(progress);
    }

    public void beginPhase(DeliveryPhase phase) {
        setPhase(phase, 0);
    }

    public int getPhaseProgress() {
        return phaseProgress;
    }

    public void setPhaseProgress(int progress) {
        this.phaseProgress = Math.max(0, progress);
    }

    public void incrementCurrentPhaseProgress() {
        phaseProgress++;
    }

    public void end() {
        this.ended = true;
    }

    // --

    public boolean isEnded() {
        return ended;
    }

    // --

    @Override
    public String toString() {
        return "Delivery{" +
              "sender=" + sender +
              ", recipient=" + recipient +
              ", mail=" + mail +
              ", phase=" + phase +
              ", progress=" + phaseProgress +
              '}';
    }
}