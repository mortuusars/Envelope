package io.github.mortuusars.envelope.world.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public interface DeliveringPigeon {
    Delivery getDelivery();
    void setDelivery(Delivery delivery);

    default ItemStack getMail() {
        return getDelivery().getMail();
    }

    default void setMail(ItemStack mail) {
        setDelivery(getDelivery().setMail(mail));
    }

    // --

    default NextPhaseBuilder nextDeliveryPhase() {
        return new NextPhaseBuilder(this, getDelivery().getCurrentPhase());
    }

    class NextPhaseBuilder {
        protected final DeliveringPigeon pigeon;

        protected Delivery.Phase.Type type;
        protected Optional<BlockPos> start;
        protected Optional<BlockPos> end;
        protected int durationTicks;

        protected NextPhaseBuilder(DeliveringPigeon pigeon, Delivery.Phase previousPhase) {
            this.pigeon = pigeon;
            type = previousPhase.type().next();
            start = previousPhase.end();
            end = Optional.empty();
            durationTicks = previousPhase.durationTicks();
        }

        public NextPhaseBuilder type(Delivery.Phase.Type type) {
            this.type = type;
            return this;
        }

        public NextPhaseBuilder startAt(@Nullable BlockPos start) {
            this.start = Optional.ofNullable(start);
            return this;
        }

        public NextPhaseBuilder endAt(@Nullable  BlockPos end) {
            this.end = Optional.ofNullable(end);
            return this;
        }

        public NextPhaseBuilder duration(int durationTicks) {
            this.durationTicks = durationTicks;
            return this;
        }

        public void begin() {
            pigeon.setDelivery(
                    pigeon.getDelivery()
                            .setPhase(new Delivery.Phase(type, start, end, durationTicks))
                            .resetTimer());
        }
    }
}
