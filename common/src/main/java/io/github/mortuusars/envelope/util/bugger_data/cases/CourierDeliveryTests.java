package io.github.mortuusars.envelope.util.bugger_data.cases;

import com.mojang.serialization.DataResult;
import io.github.mortuusars.envelope.world.mail.delivery.*;
import io.github.mortuusars.envelope.world.item.component.Id;
import io.github.mortuusars.envelope.world.mail.address.type.CustomAddress;
import io.github.mortuusars.mortaar.bugger.test.BuggerTests;
import io.github.mortuusars.mortaar.bugger.test.Test;
import net.minecraft.Util;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class CourierDeliveryTests extends BuggerTests {
    private final MinecraftServer server;

    public CourierDeliveryTests(MinecraftServer server) {
        this.server = server;
        add(new Test("TickDelivery_CallsCallbacksProperly", this::testCallbacks));
    }

    private DataResult<Boolean> testCallbacks() {
        Delivery delivery = new Delivery(
              Id.createUnsafe(0),
              Optional.empty(),
              new CustomAddress(Component.literal("test sender")),
              new CustomAddress(Component.literal("test recipient")),
              ItemStack.EMPTY,
              DeliveryRoute.EMPTY,
              DeliveryPhase.STARTED,
              0,
              false
        );
        TestCourier courier = new TestCourier(delivery);

        while (!delivery.isEnded()) {
            courier.tickDelivery(server.overworld(), delivery);
        }

        for (Map.Entry<DeliveryPhase, BitSet> check : courier.checks.entrySet()) {
            BitSet set = check.getValue();
            if (!set.get(0)) return DataResult.error(() -> check.getKey() + " - not called phaseStarted");
            if (!set.get(1)) return DataResult.error(() -> check.getKey() + " - not called phaseTicked");
            if (!set.get(2)) return DataResult.error(() -> check.getKey() + " - not called phaseCompleted");
        }

        if (!courier.endedCorrectly) {
            return DataResult.error(() -> "endDelivery was not called properly.");
        }

        return DataResult.success(true);
    }

    private static class TestCourier implements Courier {
        public final Map<DeliveryPhase, BitSet> checks = Util.make(new HashMap<>(), map -> {
            for (DeliveryPhase phase : DeliveryPhase.values()) {
                map.put(phase, new BitSet());
            }
        });
        public boolean endedCorrectly;
        public Delivery delivery;

        public TestCourier(Delivery delivery) {
            this.delivery = delivery;
        }

        @Override
        public void endDelivery(ServerLevel level, Delivery delivery) {
            if (delivery.getPhase() == DeliveryPhase.FINISHED) {
                endedCorrectly = true;
            }
        }

        @Override
        public Optional<Delivery> getCurrentDelivery() {
            return Optional.of(delivery);
        }

        @Override
        public @NotNull CourierOrigin getCourierOrigin() {
            return CourierOrigin.service();
        }

        @Override
        public boolean handlePhaseTransition(ServerLevel level, Delivery delivery) {
            // Skip any logic and just transition linearly
            delivery.beginPhase(delivery.getPhase().next());
            return true;
        }

        @Override
        public int getPhaseDuration(ServerLevel level, Delivery delivery, DeliveryPhase phase) {
            return Courier.super.getPhaseDuration(level, delivery, phase) - 1;
        }

        @Override
        public void phaseStarted(ServerLevel level, Delivery delivery) {
            checks.get(delivery.getPhase()).set(0);
        }

        @Override
        public void phaseTicked(ServerLevel level, Delivery delivery) {
            checks.get(delivery.getPhase()).set(1);
        }

        @Override
        public void phaseCompleted(ServerLevel level, Delivery delivery) {
            checks.get(delivery.getPhase()).set(2);
        }
    }
}
